# 99 契约未覆盖的临时接口调用 SOP

## 适用场景

现有工具契约（见 [`00-tool-contract.md`](00-tool-contract.md)）未覆盖的 BlogLoom 后台接口，需要临时兜底调用。

## 步骤

1. 在 `scripts/rest_tools.py` 的 `BlogLoomClient` 中新增一一映射方法，例如：

```python
def export_knowledge(self):
    return self._request("GET", "/admin/knowledge/export")
```

2. 在 `scripts/blogloom-skill.py` 的 `build_tool_registry` 中注册业务工具并暴露 `--tool`。
3. 或在本地用 `curl` 直接验证，临时排障：

```bash
curl -s "http://localhost:8090/admin/categories" -H "Authorization: $BLOOM_API_TOKEN"
```

## 约束

- 原子层只做「鉴权 + 请求 + 返回 JSON」，禁止写入业务分支；
- 新接口若复用于稳定场景，应补进 `00-tool-contract.md` 与本 Skill 的 SOP；
- 不得把 `BLOOM_API_TOKEN` 写入 `--params` 或代码。

## 输出要求

调用结果原样字段 + 必要解释；若接口不适用，明确说明原因，不要推测返回。