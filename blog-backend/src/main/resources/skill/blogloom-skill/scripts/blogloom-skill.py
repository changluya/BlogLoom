#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""BlogLoom 业务 CLI（编排层）。

用法：
    python3 scripts/blogloom-skill.py --tool <工具名> --params '<JSON>' [--pretty]
"""
import argparse
import json
import os
import re
import sys
import tempfile
import time
from os import environ

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from rest_tools import BlogLoomClient, AuthError, BusinessError


# ---------------------------------------------------------------------------
# 参数解析
# ---------------------------------------------------------------------------
def resolve_config(payload, environment=None):
    env = environment if environment is not None else os.environ
    base_url = payload.get("base_url") or env.get("BLOOM_BASE_URL")
    token = env.get("BLOOM_API_TOKEN")

    page_size = payload.get("page_size", 100)
    missing = [name for name, value in (
        ("BLOOM_BASE_URL", base_url),
        ("BLOOM_API_TOKEN", token),
    ) if not value]
    if missing:
        raise ValueError("缺少必要参数: " + ", ".join(missing))
    return {"base_url": base_url.rstrip("/"), "token": token, "page_size": page_size}


def normalize_list(value):
    """把 tags/columns 入参统一为字符串列表：支持数组、逗号分隔字符串。"""
    if value is None:
        return []
    if isinstance(value, (list, tuple)):
        return [str(v).strip() for v in value if v is not None and str(v).strip()]
    text = str(value)
    return [p.strip() for p in re.split(r"[,，、|;；]", text) if p.strip()]


def count_words(content):
    return len(re.sub(r"\s+", "", content or ""))


# ---------------------------------------------------------------------------
# Markdown 元数据解析（与平台导入规则一致：顶部 ```json 代码块）
# ---------------------------------------------------------------------------
_META_BLOCK = re.compile(r"\A\ufeff?\s*```[ \t]*json[ \t]*\r?\n(.*?)\r?\n```[ \t]*\r?\n?", re.IGNORECASE | re.DOTALL)


def parse_markdown(file_path):
    """返回 (metadata:dict, content:str)。解析失败时 metadata 为空 dict 且保留原文。"""
    if not os.path.isfile(file_path):
        raise ValueError("文件不存在: %s" % file_path)
    with open(file_path, "r", encoding="utf-8") as fp:
        raw = fp.read()
    match = _META_BLOCK.match(raw)
    metadata = {}
    content = raw
    if match:
        try:
            parsed = json.loads(match.group(1))
            if isinstance(parsed, dict):
                metadata = parsed
                content = raw[match.end():]
        except ValueError:
            metadata = {}  # 非法 JSON：视为无元数据，代码块保留在正文
    return metadata, content


def pick_first_picture(content):
    """取正文第一个图片链接（Markdown 或 HTML，取更靠前者）。"""
    candidates = []
    md = re.search(r"!\[[^\]]*\]\(([^)\s]+)", content or "")
    if md:
        candidates.append((md.start(), md.group(1)))
    html = re.search(r"<img[^>]+src=[\"']([^\"']+)[\"']", content or "", re.IGNORECASE)
    if html:
        candidates.append((html.start(), html.group(1)))
    if not candidates:
        return ""
    candidates.sort(key=lambda item: item[0])
    return candidates[0][1]


DEFAULT_COVER_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "scripts", "default-cover.png")


def resolve_first_picture(client, first_picture):
    """无封面时上传内置默认封面，保证服务端 firstPicture 非空校验通过。"""
    if first_picture and str(first_picture).strip():
        return str(first_picture).strip()
    cover_path = os.path.abspath(DEFAULT_COVER_PATH)
    if os.path.isfile(cover_path):
        resource = client.upload_resource(cover_path) or {}
        return resource.get("url") or ""
    return ""


def derive_description(metadata, content):
    summary = (metadata or {}).get("articleSummary")
    if summary and str(summary).strip():
        return str(summary).strip()
    plain = re.sub(r"!\[[^\]]*\]\([^)]*\)", "", content or "")
    plain = re.sub(r"<[^>]+>", "", plain)
    plain = re.sub(r"[#>*`\-\[\]()]", " ", plain)
    plain = re.sub(r"\s+", " ", plain).strip()
    return plain[:200]


# ---------------------------------------------------------------------------
# 本地文件收集 + 知识库导入（复用页面「导入 Markdown 到知识库」接口）
# ---------------------------------------------------------------------------
def collect_markdown_files(params):
    """从 file_path / file_paths / dir_path 收集本地 .md 文件，去重并排序。"""
    files = []
    for key in ("file_path", "file", "path"):
        value = params.get(key)
        if value and os.path.isfile(value):
            files.append(value)
    for value in (params.get("file_paths") or []):
        if value and os.path.isfile(value):
            files.append(value)
    dir_path = params.get("dir_path")
    if dir_path:
        if not os.path.isdir(dir_path):
            raise ValueError("目录不存在: %s" % dir_path)
        if params.get("recursive", True):
            for root, _dirs, names in os.walk(dir_path):
                for name in names:
                    if name.lower().endswith(".md"):
                        files.append(os.path.join(root, name))
        else:
            for name in sorted(os.listdir(dir_path)):
                full = os.path.join(dir_path, name)
                if os.path.isfile(full) and name.lower().endswith(".md"):
                    files.append(full)
    seen, unique = set(), []
    for path in files:
        real = os.path.abspath(path)
        if real not in seen:
            seen.add(real)
            unique.append(real)
    return sorted(unique)


def perform_import(client, file_paths, params):
    """调用平台导入接口：预检 -> 执行 -> 轮询进度，返回 (preview, progress)。

    与页面「导入 Markdown 到知识库」完全一致：目录由每篇的 knowledgeBasePath 决定，
    缺失目录逐级自动创建；标签/分类不存在自动创建；专栏只匹配已存在的。
    """
    if not file_paths:
        raise ValueError("没有可导入的 Markdown 文件")
    conflict_policy = (params.get("conflict_policy") or "SKIP").upper()
    if conflict_policy not in ("SKIP", "RENAME"):
        raise ValueError("conflict_policy 只支持 SKIP 或 RENAME")
    target_parent_id = params.get("target_parent_id", 0)
    published = params.get("published", True)

    preview = client.preview_import_files(file_paths, target_parent_id, published, conflict_policy) or {}
    token = preview.get("token")
    if not token:
        raise ValueError("导入预检未返回 token")
    task_id = (client.execute_import(token) or {}).get("taskId")
    if not task_id:
        raise ValueError("导入任务提交失败")

    timeout = float(params.get("timeout", 300))
    interval = float(params.get("poll_interval", 1.0))
    deadline = time.time() + timeout
    progress = {}
    while True:
        progress = client.get_import_progress(task_id) or {}
        if progress.get("status") in ("SUCCESS", "FAILED") or time.time() >= deadline:
            break
        time.sleep(interval)
    return preview, progress


def match_imported_blogs(client, file_paths):
    """导入完成后按每篇标题回查博客 id，便于 Agent 继续操作。"""
    results = []
    for path in file_paths:
        metadata, _content = parse_markdown(path)
        title = metadata.get("title") or os.path.splitext(os.path.basename(path))[0]
        blog = find_blog_by_title(client, title)
        results.append({
            "file": path,
            "title": title,
            "blog_id": blog.get("id") if blog else None,
            "knowledgeBasePath": metadata.get("knowledgeBasePath"),
        })
    return results


def build_override_file(file_path, overrides):
    """把覆盖项合并进顶部元数据后写到临时文件，用于按导入接口新增时覆盖元数据字段。

    返回 (上传用路径, 临时文件路径或 None)。无覆盖项时返回原文件。
    """
    overrides = {k: v for k, v in (overrides or {}).items() if v is not None and v != ""}
    if not overrides:
        return file_path, None
    metadata, content = parse_markdown(file_path)
    metadata.update(overrides)
    # 元数据无 title 且未覆盖 title 时，用原文件名兜底，避免服务端回退到临时文件名
    if not metadata.get("title"):
        metadata["title"] = os.path.splitext(os.path.basename(file_path))[0]
    block = "```json\n" + json.dumps(metadata, ensure_ascii=False, indent=2) + "\n```\n\n"
    fd, temp_path = tempfile.mkstemp(suffix=".md", prefix="blogloom-skill-")
    with os.fdopen(fd, "w", encoding="utf-8") as fp:
        fp.write(block + content)
    return temp_path, temp_path


def edit_existing_blog(client, resolver, blog_id, params, file_path=None, metadata=None):
    """按页面「编辑博客」接口更新已有文章：只改传入字段，其余沿用原文。"""
    current = client.get_blog(blog_id)
    if not current:
        raise ValueError("文章不存在: %s" % blog_id)

    if file_path:
        metadata, content = parse_markdown(file_path)
    else:
        content = current.get("content") or ""
    metadata = metadata or {}

    if params.get("content") is not None and not file_path:
        content = params["content"]

    title = params.get("title") or metadata.get("title") or current.get("title")
    description = params.get("description") or metadata.get("articleSummary") or current.get("description") or ""
    first_picture = params.get("firstPicture", current.get("firstPicture") or "")
    # 编辑接口要求 firstPicture 非空，缺失时补默认封面
    first_picture = resolve_first_picture(client, first_picture)

    if params.get("category"):
        cate = resolver.resolve_cate(params["category"])
    elif metadata.get("category"):
        cate = resolver.resolve_cate(metadata["category"])
    else:
        cate = (current.get("category") or {}).get("id")

    if "tags" in params:
        tag_list = resolver.resolve_tags(params.get("tags"))
    elif metadata.get("tags"):
        tag_list = resolver.resolve_tags(metadata.get("tags"))
    else:
        tag_list = [t.get("id") for t in (current.get("tags") or []) if t.get("id")]

    if params.get("columns"):
        column_ids, unresolved = resolver.resolve_columns(params["columns"])
    elif metadata.get("columns"):
        column_ids, unresolved = resolver.resolve_columns(metadata["columns"])
    else:
        column_ids = [c for c in (current.get("columnIds") or []) if c is not None]
        unresolved = []

    published = params.get("published", current.get("published", True))
    top = params.get("top", current.get("top", False))
    recommend = params.get("recommend", current.get("recommend", False))
    appreciation = params.get("appreciation", current.get("appreciation", True))
    comment_enabled = params.get("commentEnabled", current.get("commentEnabled", True))
    password = params.get("password", current.get("password") or "")

    payload = build_blog_payload(blog_id, title, content, description, first_picture,
                                 cate, tag_list, published, top, recommend,
                                 appreciation, comment_enabled, password,
                                 views=current.get("views"), create_time=current.get("createTime"),
                                 column_ids=column_ids)
    client.update_blog(payload)

    warning = None
    if unresolved:
        warning = "以下专栏不存在，已跳过（不自动创建）: " + ", ".join(unresolved)
    return {"ok": True, "action": "updated", "blog_id": blog_id, "title": title,
            "category": cate, "tags": tag_list, "columnIds": column_ids,
            "words": payload["words"], "warning": warning}


# ---------------------------------------------------------------------------
# 名称 -> id 解析（分类自动创建，标签自动创建，专栏只匹配不创建）
# ---------------------------------------------------------------------------
def _as_text(value):
    return "" if value is None else str(value)


class Resolver(object):
    def __init__(self, client):
        self.client = client
        self._categories = None
        self._tags = None
        self._columns = None

    def categories(self):
        if self._categories is None:
            data = self.client.list_categories() or {}
            self._categories = {c["name"].strip().lower(): c["id"] for c in (data.get("list") or [])}
        return self._categories

    def tags(self):
        if self._tags is None:
            data = self.client.list_tags() or {}
            self._tags = {t["name"].strip().lower(): t["id"] for t in (data.get("list") or [])}
        return self._tags

    def column_pairs(self):
        """返回 [(id, full_path_name, leaf_name), ...]，全层级展开。"""
        if self._columns is None:
            tree = self.client.list_columns() or []
            pairs = []

            def walk(nodes, prefix):
                for node in nodes or []:
                    name = _as_text(node.get("name")).strip()
                    full = "%s/%s" % (prefix, name) if prefix else name
                    pairs.append((node.get("id"), full, name))
                    walk(node.get("children"), full)

            walk(tree, "")
            self._columns = pairs
        return self._columns

    def resolve_cate(self, category):
        """分类：支持 id(int) 或名称(str)；名称不存在时返回名称交由服务端新建。"""
        if category is None or category == "":
            raise ValueError("缺少分类：请通过 category 指定分类名或分类 id")
        if isinstance(category, int) or (isinstance(category, str) and category.isdigit()):
            return int(category)
        name = str(category).strip()
        existing = self.categories().get(name.lower())
        if existing is not None:
            return existing
        # 名称不存在：返回字符串，服务端 getResult 会自动创建分类
        return name

    def resolve_tags(self, tags):
        """标签：名称已存在返回 id，不存在返回名称（服务端自动创建）。"""
        result = []
        existing = self.tags()
        for name in normalize_list(tags):
            if name.isdigit():
                result.append(int(name))
                continue
            tag_id = existing.get(name.lower())
            result.append(tag_id if tag_id is not None else name)
        return result

    def resolve_columns(self, columns):
        """专栏：只匹配已存在的（含二级），支持「父/子」或「名称」，不创建。"""
        pairs = self.column_pairs()
        by_full = {}
        by_leaf = {}
        for cid, full, leaf in pairs:
            by_full.setdefault(full.strip().lower(), cid)
            by_leaf.setdefault(leaf.strip().lower(), []).append(cid)
        resolved, unresolved = [], []
        for name in normalize_list(columns):
            key = name.strip().lower()
            if key in by_full:
                resolved.append(by_full[key])
            elif key in by_leaf and len(by_leaf[key]) == 1:
                resolved.append(by_leaf[key][0])
            elif key in by_leaf:
                resolved.append(by_leaf[key][0])  # 同名多层级取第一个
            else:
                unresolved.append(name)
        # 去重保序
        seen, unique = set(), []
        for cid in resolved:
            if cid not in seen:
                seen.add(cid)
                unique.append(cid)
        return unique, unresolved


# ---------------------------------------------------------------------------
# 博客构造
# ---------------------------------------------------------------------------
def build_blog_payload(blog_id, title, content, description, first_picture, cate, tag_list,
                       published, top, recommend, appreciation, comment_enabled, password,
                       views=None, create_time=None, column_ids=None):
    words = count_words(content)
    payload = {
        "title": title,
        "content": content,
        "description": description,
        "firstPicture": first_picture or "",
        "words": words,
        "readTime": max(1, round(words / 200.0)) if words else 0,
        "published": bool(published),
        "top": bool(top),
        "recommend": bool(recommend),
        "appreciation": bool(appreciation),
        "commentEnabled": bool(comment_enabled),
        "password": password or "",
        "cate": cate,
        "tagList": tag_list,
    }
    if column_ids is not None:
        payload["columnIds"] = list(column_ids)
    if blog_id is not None:
        payload["id"] = blog_id
        # updateBlog 会写入 create_time/views 且不允许为空，更新时必须回填原值
        payload["createTime"] = create_time
        payload["views"] = views if views is not None else 0
    return payload


# ---------------------------------------------------------------------------
# 工具注册表
# ---------------------------------------------------------------------------
def build_tool_registry(client):
    resolver = Resolver(client)

    # —— 查询类 ——
    def get_blog(params):
        blog_id = params.get("id") or params.get("blog_id")
        if not blog_id:
            raise ValueError("缺少必要参数: id")
        return {"ok": True, "blog": client.get_blog(blog_id)}

    def list_blogs(params):
        data = client.list_blogs(params.get("title", ""), params.get("categoryId", ""),
                                 params.get("pageNum", 1), params.get("page_size", 100)) or {}
        blogs = (data.get("blogs") or {})
        return {"ok": True, "total": blogs.get("total"), "blogs": blogs.get("list") or [],
                "categories": data.get("categories") or []}

    def list_categories(params):
        data = client.list_categories() or {}
        return {"ok": True, "total": data.get("total"), "categories": data.get("list") or []}

    def list_tags(params):
        data = client.list_tags() or {}
        return {"ok": True, "total": data.get("total"), "tags": data.get("list") or []}

    def list_columns(params):
        return {"ok": True, "columns": client.list_columns() or []}

    # —— 新增/导入：直接复用页面「导入 Markdown 到知识库」接口（原生支持 knowledgeBasePath）——
    def import_blog(params):
        file_paths = collect_markdown_files(params)
        if not file_paths:
            raise ValueError("没有可导入的 Markdown 文件：请通过 file_path / file_paths / dir_path 指定")
        preview, progress = perform_import(client, file_paths, params)
        imported = match_imported_blogs(client, file_paths)
        return {
            "ok": progress.get("status") == "SUCCESS",
            "total": len(file_paths),
            "preview": preview,
            "progress": progress,
            "importedBlogs": imported,
        }

    # 兼容旧工具名：目录批量导入 = import_blog 传 dir_path
    def import_markdown_dir(params):
        return import_blog(params)

    # —— 本地单篇同步：存在则走「编辑博客」接口更新，不存在则走「导入 Markdown」接口新增 ——
    def sync_local_blog(params):
        file_path = params.get("file_path") or params.get("path")
        if not file_path:
            raise ValueError("缺少必要参数: file_path")
        if not os.path.isfile(file_path):
            raise ValueError("文件不存在: %s" % file_path)

        metadata, _content = parse_markdown(file_path)
        title = params.get("title") or metadata.get("title") or \
            os.path.splitext(os.path.basename(file_path))[0]

        mode = (params.get("mode") or "auto").lower()
        if mode not in ("auto", "create", "update"):
            raise ValueError("mode 只支持 auto / create / update")
        target_id = params.get("blog_id")
        existing = client.get_blog(target_id) if target_id else find_blog_by_title(client, title)
        if mode == "auto":
            mode = "update" if existing else "create"

        if mode == "update":
            if existing is None:
                raise ValueError("文章不存在: %s（如需新增请用 mode=create）" % title)
            return edit_existing_blog(client, resolver, existing.get("id"), params, file_path=file_path)

        # mode == create：走页面导入接口，元数据覆盖项通过改写顶部代码块实现
        overrides = {}
        if params.get("title"):
            overrides["title"] = params["title"]
        if params.get("category"):
            overrides["category"] = params["category"]
        if params.get("tags"):
            overrides["tags"] = params["tags"]
        if params.get("columns"):
            overrides["columns"] = params["columns"]
        if params.get("description"):
            overrides["articleSummary"] = params["description"]
        if params.get("knowledgeBasePath"):
            overrides["knowledgeBasePath"] = params["knowledgeBasePath"]

        upload_path, temp_path = build_override_file(file_path, overrides)
        try:
            preview, progress = perform_import(client, [upload_path], params)
        finally:
            if temp_path:
                try:
                    os.remove(temp_path)
                except OSError:
                    pass

        created = find_blog_by_title(client, title)
        return {
            "ok": progress.get("status") == "SUCCESS",
            "action": "created",
            "blog_id": created.get("id") if created else None,
            "title": title,
            "category": overrides.get("category") or metadata.get("category") or "知识库",
            "tags": normalize_list(overrides.get("tags") or metadata.get("tags")),
            "knowledgeBasePath": overrides.get("knowledgeBasePath") or metadata.get("knowledgeBasePath"),
            "preview": preview,
            "progress": progress,
        }

    # —— 安全增量更新（只改指定参数，正文与其余字段沿用原文）——
    def update_blog(params):
        blog_id = params.get("blog_id") or params.get("id")
        if not blog_id:
            raise ValueError("缺少必要参数: blog_id")
        result = edit_existing_blog(client, resolver, blog_id, params,
                                    file_path=params.get("file_path"))
        result["updated"] = [key for key in ("title", "description", "firstPicture", "category",
                                             "tags", "published", "top", "recommend", "appreciation",
                                             "commentEnabled", "password", "content", "columns")
                             if key in params]
        return result

    # —— 专栏关联维护 ——
    def update_column_relation(params):
        blog_id = params.get("blog_id") or params.get("id")
        if not blog_id:
            raise ValueError("缺少必要参数: blog_id")
        columns = params.get("columns")
        if columns is None:
            raise ValueError("缺少必要参数: columns")
        resolved, unresolved = resolver.resolve_columns(columns)
        mode = (params.get("mode") or "replace").lower()
        current = _current_column_ids(client, blog_id)
        if mode == "add":
            final = list(dict.fromkeys(current + resolved))
        elif mode == "remove":
            final = [cid for cid in current if cid not in set(resolved)]
        else:
            final = resolved
        _replace_columns(client, blog_id, final)
        return {"ok": True, "blog_id": blog_id, "mode": mode, "columnIds": final,
                "unresolved": unresolved}

    # —— 可见性一键修改 ——
    def publish_visibility(params):
        blog_id = params.get("blog_id") or params.get("id")
        if not blog_id:
            raise ValueError("缺少必要参数: blog_id")
        if "published" not in params:
            raise ValueError("缺少必要参数: published")
        visibility = {
            "published": bool(params.get("published")),
            "password": params.get("password", "") or "",
            "appreciation": bool(params.get("appreciation", True)),
            "recommend": bool(params.get("recommend", False)),
            "commentEnabled": bool(params.get("commentEnabled", True)),
            "top": bool(params.get("top", False)),
        }
        client.update_visibility(blog_id, visibility)
        return {"ok": True, "blog_id": blog_id, "visibility": visibility}

    def update_top(params):
        blog_id = params.get("blog_id") or params.get("id")
        if not blog_id or "top" not in params:
            raise ValueError("缺少必要参数: blog_id, top")
        client.update_top(blog_id, params.get("top"))
        return {"ok": True, "blog_id": blog_id, "top": bool(params.get("top"))}

    def update_recommend(params):
        blog_id = params.get("blog_id") or params.get("id")
        if not blog_id or "recommend" not in params:
            raise ValueError("缺少必要参数: blog_id, recommend")
        client.update_recommend(blog_id, params.get("recommend"))
        return {"ok": True, "blog_id": blog_id, "recommend": bool(params.get("recommend"))}

    def upload_resource(params):
        file_path = params.get("file_path")
        if not file_path:
            raise ValueError("缺少必要参数: file_path")
        return {"ok": True, "resource": client.upload_resource(file_path)}

    return {
        "get_blog": get_blog,
        "list_blogs": list_blogs,
        "list_categories": list_categories,
        "list_tags": list_tags,
        "list_columns": list_columns,
        "import_blog": import_blog,
        "sync_local_blog": sync_local_blog,
        "import_markdown_dir": import_markdown_dir,
        "update_blog": update_blog,
        "update_column_relation": update_column_relation,
        "publish_visibility": publish_visibility,
        "update_top": update_top,
        "update_recommend": update_recommend,
        "upload_resource": upload_resource,
    }


# ---------------------------------------------------------------------------
# 内部辅助
# ---------------------------------------------------------------------------
def find_blog_by_title(client, title):
    data = client.list_blogs(title=title, page_size=100) or {}
    blogs = (data.get("blogs") or {}).get("list") or []
    for blog in blogs:
        if _as_text(blog.get("title")).strip() == _as_text(title).strip():
            return blog
    return None


def _current_column_ids(client, blog_id):
    blog = client.get_blog(blog_id) or {}
    return [c for c in (blog.get("columnIds") or []) if c is not None]


def _replace_columns(client, blog_id, column_ids):
    """通过 PUT /admin/blog 更新文章的专栏关联（服务端 updateBlog 会 replace 专栏关系）。"""
    blog = client.get_blog(blog_id) or {}
    category = (blog.get("category") or {}).get("id")
    tags = [t.get("id") for t in (blog.get("tags") or []) if t.get("id")]
    payload = {
        "id": blog_id,
        "title": blog.get("title"),
        "content": blog.get("content") or "",
        "description": blog.get("description") or "",
        "firstPicture": blog.get("firstPicture") or "",
        "words": blog.get("words") or count_words(blog.get("content")),
        "readTime": blog.get("readTime"),
        "published": blog.get("published"),
        "top": blog.get("top"),
        "recommend": blog.get("recommend"),
        "appreciation": blog.get("appreciation"),
        "commentEnabled": blog.get("commentEnabled"),
        "password": blog.get("password") or "",
        "createTime": blog.get("createTime"),
        "views": blog.get("views") or 0,
        "cate": category,
        "tagList": tags,
        "columnIds": column_ids,
    }
    client.update_blog(payload)


# ---------------------------------------------------------------------------
# 入口
# ---------------------------------------------------------------------------
def main():
    parser = argparse.ArgumentParser(description="BlogLoom skill CLI")
    parser.add_argument("--tool", required=True)
    parser.add_argument("--params", default="{}")
    parser.add_argument("--pretty", action="store_true")
    args = parser.parse_args()

    def emit(obj, code=0):
        print(json.dumps(obj, ensure_ascii=False, indent=2 if args.pretty else None))
        sys.exit(code)

    try:
        payload = json.loads(args.params)
        if not isinstance(payload, dict):
            raise ValueError("--params 必须是 JSON 对象")
    except ValueError as exc:
        emit({"ok": False, "tool": args.tool, "data": None, "error": "--params 不是合法 JSON: %s" % exc}, 2)

    try:
        config = resolve_config(payload)
    except ValueError as exc:
        emit({"ok": False, "tool": args.tool, "data": None, "error": str(exc)}, 2)

    client = BlogLoomClient(config["base_url"], config["token"])
    registry = build_tool_registry(client)

    if args.tool not in registry:
        emit({"ok": False, "tool": args.tool, "data": None,
              "error": "未知工具: %s" % args.tool, "available": sorted(registry)}, 2)

    try:
        result = registry[args.tool](payload)
        result.setdefault("tool", args.tool)
        result.setdefault("error", None)
        emit(result, 0 if result.get("ok") else 1)
    except (AuthError, BusinessError) as exc:
        emit({"ok": False, "tool": args.tool, "data": None, "error": str(exc)}, 1)
    except ValueError as exc:
        emit({"ok": False, "tool": args.tool, "data": None, "error": str(exc)}, 2)
    except Exception as exc:  # noqa: BLE001
        emit({"ok": False, "tool": args.tool, "data": None,
              "error": "%s: %s" % (type(exc).__name__, exc)}, 1)


if __name__ == "__main__":
    main()