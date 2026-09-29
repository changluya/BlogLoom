# BlogLoom Publisher Skill 环境变量（按需修改后 source 本文件）
# 数据根目录：浏览器 profile、运行产物默认落在这里
export PUBLISHER_HOME="$HOME/.blogloom-publisher"

# 浏览器渠道：chrome（默认，系统 Chrome）/ msedge / chromium（留空用 Playwright Chromium）
export PUBLISHER_BROWSER_CHANNEL="chrome"

# 浏览器模式：headed（默认，可见，便于观察操作）/ headless（无头）
export PUBLISHER_MODE="headed"

# 登录等待上限（毫秒），默认 10 分钟，给扫码充足时间
export PUBLISHER_LOGIN_TIMEOUT="600000"

# 复用已有 Chrome 登录态（可选）：先带 --remote-debugging-port=9222 启动 Chrome 再启用
# export PUBLISHER_CDP_URL="http://127.0.0.1:9222"
