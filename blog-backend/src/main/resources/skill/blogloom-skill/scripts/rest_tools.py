# -*- coding: utf-8 -*-
"""BlogLoom REST 原子工具层：一个函数 = 一个 REST 接口，只做鉴权 + 请求 + 原样返回 JSON。"""
import os

import requests


class BlogLoomClient(object):
    def __init__(self, base_url, token, timeout=120):
        self.base_url = (base_url or "").rstrip("/")
        self.token = token
        self.timeout = timeout

    def _headers(self):
        return {"Authorization": self.token, "Content-Type": "application/json"}

    def _handle_response(self, resp, default_message="请求失败"):
        if resp.status_code in (401, 403):
            raise AuthError("鉴权失败（%s），Token 可能已过期，请在 CMS「文章管理」右上角重新下载 Skill 刷新 token。" % resp.status_code)
        resp.raise_for_status()
        body = resp.json()
        if isinstance(body, dict) and body.get("code") not in (None, 200):
            raise BusinessError(body.get("code"), body.get("msg") or default_message)
        return body.get("data") if isinstance(body, dict) else body

    def _request(self, method, path, **kwargs):
        resp = requests.request(
            method,
            self.base_url + path,
            headers=self._headers(),
            timeout=self.timeout,
            **kwargs
        )
        return self._handle_response(resp)

    # —— 接口一一映射 ——
    def list_categories(self, page_num=1, page_size=100):
        return self._request("GET", "/admin/categories",
                             params={"pageNum": page_num, "pageSize": page_size})

    def list_tags(self, page_num=1, page_size=100):
        return self._request("GET", "/admin/tags",
                             params={"pageNum": page_num, "pageSize": page_size})

    def list_columns(self):
        return self._request("GET", "/admin/columns/options")

    def get_blog(self, blog_id):
        return self._request("GET", "/admin/blog", params={"id": blog_id})

    def list_blogs(self, title="", category_id="", page_num=1, page_size=100):
        return self._request("GET", "/admin/blogs",
                             params={"title": title or "", "categoryId": category_id or "",
                                     "pageNum": page_num, "pageSize": page_size})

    def update_blog(self, blog):
        return self._request("PUT", "/admin/blog", json=blog)

    def update_visibility(self, blog_id, visibility):
        return self._request("PUT", "/admin/blog/%s/visibility" % blog_id, json=visibility)

    def update_top(self, blog_id, top):
        return self._request("PUT", "/admin/blog/top",
                             params={"id": blog_id, "top": str(bool(top)).lower()})

    def update_recommend(self, blog_id, recommend):
        return self._request("PUT", "/admin/blog/recommend",
                             params={"id": blog_id, "recommend": str(bool(recommend)).lower()})

    # —— 知识库导入（复用页面「导入 Markdown 到知识库」接口，原生支持 knowledgeBasePath）——
    def preview_import_files(self, file_paths, target_parent_id=0, published=True, conflict_policy="SKIP"):
        handles = []
        files = []
        try:
            for path in file_paths:
                fp = open(path, "rb")
                handles.append(fp)
                files.append(("files", (os.path.basename(path), fp, "text/markdown")))
            data = {
                "targetParentId": target_parent_id or 0,
                "published": str(bool(published)).lower(),
                "conflictPolicy": conflict_policy or "SKIP",
            }
            resp = requests.post(
                self.base_url + "/admin/knowledge/import/preview/files",
                headers={"Authorization": self.token},
                files=files, data=data, timeout=self.timeout,
            )
        finally:
            for fp in handles:
                fp.close()
        return self._handle_response(resp, "预检失败")

    def execute_import(self, token):
        return self._request("POST", "/admin/knowledge/import/%s/execute" % token)

    def get_import_progress(self, task_id):
        return self._request("GET", "/admin/knowledge/import/tasks/%s" % task_id)

    def upload_resource(self, file_path):
        file_name = file_path.replace("\\", "/").rsplit("/", 1)[-1]
        with open(file_path, "rb") as fp:
            files = {"file": (file_name, fp)}
            resp = requests.post(
                self.base_url + "/admin/blog/resources",
                headers={"Authorization": self.token},
                files=files,
                timeout=self.timeout,
            )
        return self._handle_response(resp, "上传失败")


class AuthError(Exception):
    pass


class BusinessError(Exception):
    def __init__(self, code, message):
        super(BusinessError, self).__init__(message)
        self.code = code