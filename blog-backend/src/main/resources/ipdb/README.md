# ip2region 数据文件

本目录存放后端使用的 ip2region 离线 IP 地域数据文件。ip2region 是一个开源的离线 IP 查询库及数据管理项目，官方仓库地址：[https://github.com/lionsoul2014/ip2region](https://github.com/lionsoul2014/ip2region)。官方数据文件位于 [data 目录](https://github.com/lionsoul2014/ip2region/tree/master/data)。

| 文件 | 地址族 | 用途 |
| --- | --- | --- |
| `ip2region_v4.xdb` | IPv4 | 查询 IPv4 地址的地域信息 |
| `ip2region_v6.xdb` | IPv6 | 查询 IPv6 地址的地域信息 |

后端需要根据 IP 地址类型使用对应的数据文件；两份文件共同提供 IPv4/IPv6 双栈查询能力。数据内容及精度以 ip2region 上游项目为准。

## 更新方式

在 BlogLoom 仓库根目录执行本地更新脚本：

```bash
./bin/local/update-ip2region.sh
```

脚本从上游 `data` 目录下载 v4、v6 两份 XDB 文件，并更新到当前目录。执行环境需要安装 `curl` 且能够访问 GitHub；如需使用镜像，可通过 `IP2REGION_BASE_URL` 指定数据文件的基础 URL，详细说明见 [`bin/local/README.md`](../../../../../bin/local/README.md)。
