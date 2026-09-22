#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""BlogLoom Skill 单测：不依赖真实服务，全部 mock RestClient。

运行：
    python3 test/test_blogloom_skill.py
"""
import os
import sys
import unittest

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "scripts"))

import importlib.util  # noqa: E402

_spec = importlib.util.spec_from_file_location(
    "blogloom_skill", os.path.join(ROOT, "scripts", "blogloom-skill.py"))
skill = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(skill)


class FakeClient(object):
    """内存假客户端：模拟 BlogLoomClient，并在导入接口中复刻服务端建博客行为。"""

    def __init__(self):
        self.blogs = {}
        self.categories = [{"id": 1, "name": "AI模型"}]
        self.tags = [{"id": 1, "name": "Maven"}]
        self.columns = [{"id": 10, "name": "项目管理工具", "children": [{"id": 11, "name": "Maven&Gradle"}]}]
        self._seq = 100
        self._import_files = []
        self._import_done = True
        self._import_published = True
        self._import_conflict = "SKIP"

    # —— 查询 ——
    def list_categories(self):
        return {"total": len(self.categories), "list": self.categories}

    def list_tags(self):
        return {"total": len(self.tags), "list": self.tags}

    def list_columns(self):
        return self.columns

    def list_blogs(self, title="", category_id="", page_num=1, page_size=100):
        items = [b for b in self.blogs.values() if title in (b.get("title") or "")]
        return {"blogs": {"total": len(items), "list": items}, "categories": self.categories}

    def get_blog(self, blog_id):
        return self.blogs.get(int(blog_id))

    # —— 编辑 ——
    def update_blog(self, blog):
        assert blog.get("createTime"), "updateBlog 必须回填 createTime"
        old = self.blogs.get(int(blog["id"]), {})
        merged = dict(old)
        merged.update(blog)
        self.blogs[int(blog["id"])] = merged
        return None

    def update_visibility(self, blog_id, visibility):
        self.blogs[int(blog_id)].update(visibility)
        return None

    def update_top(self, blog_id, top):
        self.blogs[int(blog_id)]["top"] = bool(top)
        return None

    def update_recommend(self, blog_id, recommend):
        self.blogs[int(blog_id)]["recommend"] = bool(recommend)
        return None

    def upload_resource(self, file_path):
        return {"url": "https://example.com/default-cover.png", "relativePath": "tmp/x.png"}

    # —— 导入（复刻服务端：解析元数据 -> 建博客，SKIP 同名）——
    def _create(self, payload):
        self._seq += 1
        payload = dict(payload)
        payload["id"] = self._seq
        payload["createTime"] = "2026-09-21T10:00:00.000+0000"
        payload["views"] = 0
        self.blogs[payload["id"]] = payload
        return payload["id"]

    def preview_import_files(self, file_paths, target_parent_id=0, published=True, conflict_policy="SKIP"):
        self._import_files = list(file_paths)
        self._import_published = published
        self._import_conflict = conflict_policy
        self._import_done = False
        return {"token": "token-%d" % (self._seq + 1), "documentCount": len(file_paths),
                "directoryCount": 0, "ignoredCount": 0, "paths": []}

    def execute_import(self, token):
        return {"taskId": "task-%s" % token}

    def get_import_progress(self, task_id):
        if not self._import_done:
            for path in self._import_files:
                metadata, content = skill.parse_markdown(path)
                title = metadata.get("title") or os.path.splitext(os.path.basename(path))[0]
                if self._import_conflict == "SKIP" and any(
                        b.get("title") == title for b in self.blogs.values()):
                    continue
                self._create({
                    "title": title,
                    "content": content,
                    "description": metadata.get("articleSummary") or "",
                    "firstPicture": skill.pick_first_picture(content),
                    "published": self._import_published,
                    "category": {"id": None, "name": metadata.get("category") or "知识库"},
                    "tags": [], "columnIds": [],
                    "knowledgeBasePath": metadata.get("knowledgeBasePath"),
                })
            self._import_done = True
        return {"status": "SUCCESS", "progress": 100, "total": len(self._import_files),
                "processed": len(self._import_files), "createdBlogCount": len(self._import_files),
                "createdDirectoryCount": 0, "skippedCount": 0, "message": "导入完成"}


class WriteMarkdown(object):
    def __init__(self, path, text):
        self.path = path
        self.text = text

    def __enter__(self):
        with open(self.path, "w", encoding="utf-8") as fp:
            fp.write(self.text)

    def __exit__(self, *exc):
        return False


TMP_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_tmp")


class BlogLoomSkillTest(unittest.TestCase):
    def setUp(self):
        if not os.path.isdir(TMP_DIR):
            os.makedirs(TMP_DIR)
        self.client = FakeClient()
        self.registry = skill.build_tool_registry(self.client)

    def tearDown(self):
        for name in os.listdir(TMP_DIR):
            os.remove(os.path.join(TMP_DIR, name))
        os.rmdir(TMP_DIR)

    def _md(self, name, text):
        path = os.path.join(TMP_DIR, name)
        with WriteMarkdown(path, text):
            pass
        return path

    # —— 工具注册 ——
    def test_registry_contains_expected_tools(self):
        expected = {
            "get_blog", "list_blogs", "list_categories", "list_tags", "list_columns",
            "import_blog", "sync_local_blog", "import_markdown_dir", "update_blog",
            "update_column_relation", "publish_visibility", "update_top",
            "update_recommend", "upload_resource",
        }
        self.assertEqual(expected, set(self.registry))

    # —— 参数映射 ——
    def test_missing_params_listed_once(self):
        with self.assertRaises(ValueError) as ctx:
            skill.resolve_config({}, environment={})
        msg = str(ctx.exception)
        self.assertIn("BLOOM_BASE_URL", msg)
        self.assertIn("BLOOM_API_TOKEN", msg)

    def test_base_url_override_by_payload(self):
        cfg = skill.resolve_config({"base_url": "http://test/"}, environment={"BLOOM_API_TOKEN": "t"})
        self.assertEqual("http://test", cfg["base_url"])

    def test_token_only_from_env(self):
        with self.assertRaises(ValueError):
            skill.resolve_config({"base_url": "http://x", "BLOOM_API_TOKEN": "injected"}, environment={})

    # —— 解析 ——
    def test_parse_markdown_removes_meta_block(self):
        path = self._md("a.md", '```json\n{"title":"T1","tags":"a,b"}\n```\n\n# 正文\n内容')
        metadata, content = skill.parse_markdown(path)
        self.assertEqual("T1", metadata["title"])
        self.assertNotIn("```json", content)
        self.assertIn("# 正文", content)

    def test_parse_markdown_bad_json_keeps_block(self):
        path = self._md("b.md", '```json\n{not json}\n```\n正文')
        metadata, content = skill.parse_markdown(path)
        self.assertEqual({}, metadata)
        self.assertIn("```json", content)

    def test_pick_cover_only_matches_coverImg(self):
        # 普通图片不再作为封面
        self.assertEqual("", skill.pick_first_picture("x\n![pic](http://a.png)\ny"))
        # 仅 alt 为 coverImg 的图片作为封面
        self.assertEqual("http://a.png", skill.pick_first_picture("x\n![coverImg](http://a.png)\ny"))
        # HTML 写法
        self.assertEqual("http://b.png",
                         skill.pick_first_picture("<img alt=\"coverImg\" src=\"http://b.png\">"))
        # 同时存在时取正文中更靠前者
        content = "<img alt=\"coverImg\" src=\"http://first.png\">\n![coverImg](http://second.png)"
        self.assertEqual("http://first.png", skill.pick_first_picture(content))
        # 无任何图片 / 无 coverImg 标记
        self.assertEqual("", skill.pick_first_picture("纯文本，没有图片"))

    def test_derive_description_fallback(self):
        desc = skill.derive_description({}, "# 标题\n\n正文内容")
        self.assertIn("标题", desc)

    def test_collect_markdown_files_from_dir(self):
        self._md("f1.md", "# 1")
        self._md("f2.md", "# 2")
        with open(os.path.join(TMP_DIR, "ignore.txt"), "w", encoding="utf-8") as fp:
            fp.write("x")
        files = skill.collect_markdown_files({"dir_path": TMP_DIR})
        self.assertEqual(2, len(files))
        self.assertTrue(all(f.endswith(".md") for f in files))

    # —— 编排逻辑 ——
    def test_import_creates_blog_with_knowledge_path(self):
        path = self._md("k.md", '```json\n{"title":"知识库文章","category":"Maven",'
                                '"knowledgeBasePath":"/Java/Maven"}\n```\n正文')
        result = self.registry["import_blog"]({"file_path": path})
        self.assertTrue(result["ok"])
        self.assertEqual(1, result["total"])
        self.assertEqual("知识库文章", result["importedBlogs"][0]["title"])
        self.assertEqual("/Java/Maven", result["importedBlogs"][0]["knowledgeBasePath"])
        self.assertIsNotNone(result["importedBlogs"][0]["blog_id"])

    def test_sync_creates_then_updates(self):
        path = self._md("c.md", '```json\n{"title":"新文章","category":"新分类","tags":"t1,t2"}\n```\n正文')
        first = self.registry["sync_local_blog"]({"file_path": path})
        self.assertEqual("created", first["action"])
        self.assertIsNotNone(first["blog_id"])
        self.assertEqual("新分类", first["category"])
        self.assertEqual(["t1", "t2"], first["tags"])

        second = self.registry["sync_local_blog"]({"file_path": path})
        self.assertEqual("updated", second["action"])
        self.assertEqual(first["blog_id"], second["blog_id"])
        self.assertEqual(1, len(self.client.blogs))

    def test_sync_forces_update_with_blog_id(self):
        path = self._md("d.md", "# 标题\n正文")
        created = self.registry["sync_local_blog"]({"file_path": path})
        updated = self.registry["sync_local_blog"]({"file_path": path, "blog_id": created["blog_id"]})
        self.assertEqual("updated", updated["action"])

    def test_sync_create_mode_always_imports(self):
        path = self._md("e.md", "# 强制新增\n正文")
        first = self.registry["sync_local_blog"]({"file_path": path, "mode": "create"})
        self.assertEqual("created", first["action"])
        self.assertEqual(1, len(self.client.blogs))

    def test_update_fills_default_cover_when_missing(self):
        path = self._md("g.md", "# 无图\n正文")
        created = self.registry["sync_local_blog"]({"file_path": path})
        self.assertEqual("", self.client.get_blog(created["blog_id"])["firstPicture"])
        self.registry["update_blog"]({"blog_id": created["blog_id"], "title": "无图改"})
        self.assertEqual("https://example.com/default-cover.png",
                         self.client.get_blog(created["blog_id"])["firstPicture"])

    def test_import_dir_creates_and_skips(self):
        self._md("f1.md", "# 文章一\n正文")
        self._md("f2.md", "# 文章二\n正文")
        first = self.registry["import_markdown_dir"]({"dir_path": TMP_DIR, "recursive": False})
        self.assertTrue(first["ok"])
        self.assertEqual(2, first["total"])
        self.assertEqual(2, len(self.client.blogs))
        # 再次导入（SKIP 同名）不产生新博客
        self.registry["import_markdown_dir"]({"dir_path": TMP_DIR, "recursive": False,
                                               "conflict_policy": "SKIP"})
        self.assertEqual(2, len(self.client.blogs))

    def test_import_invalid_conflict_policy(self):
        path = self._md("h.md", "# 冲突\n正文")
        with self.assertRaises(ValueError) as ctx:
            self.registry["import_blog"]({"file_path": path, "conflict_policy": "UPDATE"})
        self.assertIn("SKIP", str(ctx.exception))

    def test_update_column_relation_modes(self):
        path = self._md("i.md", "# 专栏文\n正文")
        created = self.registry["sync_local_blog"]({"file_path": path})
        bid = created["blog_id"]
        rel = self.registry["update_column_relation"]({"blog_id": bid, "columns": ["Maven&Gradle"]})
        self.assertEqual([11], rel["columnIds"])
        rel2 = self.registry["update_column_relation"]({"blog_id": bid, "columns": ["项目管理工具"]})
        self.assertEqual([10], rel2["columnIds"])
        rel3 = self.registry["update_column_relation"]({"blog_id": bid, "columns": ["不存在"]})
        self.assertEqual([], rel3["columnIds"])
        self.assertEqual(["不存在"], rel3["unresolved"])

    def test_publish_visibility(self):
        path = self._md("j.md", "# 可见性\n正文")
        created = self.registry["sync_local_blog"]({"file_path": path})
        result = self.registry["publish_visibility"]({"blog_id": created["blog_id"], "published": False})
        self.assertTrue(result["ok"])
        self.assertFalse(self.client.get_blog(created["blog_id"])["published"])

    # —— 错误边界 ——
    def test_sync_missing_file_path(self):
        with self.assertRaises(ValueError) as ctx:
            self.registry["sync_local_blog"]({})
        self.assertIn("file_path", str(ctx.exception))

    def test_import_missing_files(self):
        with self.assertRaises(ValueError) as ctx:
            self.registry["import_blog"]({})
        self.assertIn("Markdown", str(ctx.exception))

    def test_publish_requires_published(self):
        with self.assertRaises(ValueError) as ctx:
            self.registry["publish_visibility"]({"blog_id": 1})
        self.assertIn("published", str(ctx.exception))

    def test_get_blog_requires_id(self):
        with self.assertRaises(ValueError) as ctx:
            self.registry["get_blog"]({})
        self.assertIn("id", str(ctx.exception))


if __name__ == "__main__":
    unittest.main(verbosity=2)