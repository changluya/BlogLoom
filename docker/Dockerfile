# syntax=docker/dockerfile:1
#
# =============================================================================
# BlogLoom 一体化镜像
# =============================================================================
# 一个镜像同时包含：Spring Boot 后端 + 博客前台（blog-view-ui）+ 内容管理后台（blog-cms-ui）。
# 运行时不依赖外部 Web 服务器，前端静态资源由后端直接提供。
#
# 容器内目录遵循 conf / lib / bin 部署结构：
#   /opt/blogloom/bin    启动与增量升级脚本（start.sh、upgrade-sql.sh）
#   /opt/blogloom/conf   配置、日志、上传、前端静态资源
#                        - conf/application.properties  外置配置（运行时由仓库 conf/ 挂载覆盖）
#                        - conf/logback-spring.xml      日志配置（运行时由仓库 conf/ 挂载覆盖）
#                        - conf/static/view             博客前台构建产物（映射到根路径 /）
#                        - conf/static/cms              管理后台构建产物（映射到 /cms）
#                        - conf/logs、conf/upload       运行日志与上传资源（运行时挂载持久化）
#   /opt/blogloom/lib    后端可执行 jar
#   /opt/blogloom/sql    增量 SQL 与执行记录（运行时挂载，由 bin/start.sh 在启动前执行）
#
# 运行时基础镜像直接使用 mysql:8.0：
#   - 自带 mysql 客户端与常用 shell 工具，使 bin/start.sh 能在启动 Java 前执行增量 SQL；
#   - 无需 apt/apk 安装依赖，构建更稳定、可离线。
#   - Java 运行时从 eclipse-temurin 镜像拷贝。
#
# 构建上下文为仓库根目录（注意末尾的 .）：
#   docker build -f docker/Dockerfile -t blogloom:1.0.0 .
# 或直接使用一键脚本：cd docker && ./scripts/package.sh 1.0.0
# =============================================================================

# ---------- 1. 构建后端（Maven + JDK8） ----------
FROM maven:3.8-jdk-8 AS backend-build
# 部分 arm64 / 虚拟化环境下 JDK8 的 PerfData 会导致 JVM 崩溃，这里显式关闭。
ENV MAVEN_OPTS="-XX:-UsePerfData"
WORKDIR /workspace
# 先只拷贝 pom 预热依赖，利用 Docker 层缓存加速后续构建。
COPY blog-backend/pom.xml ./blog-backend/pom.xml
RUN cd blog-backend && mvn -B -q dependency:resolve
COPY blog-backend/src ./blog-backend/src
RUN cd blog-backend && mvn -B -q clean package -Dmaven.test.skip=true

# ---------- 2. 构建博客前台（blog-view-ui） ----------
FROM node:18 AS view-build
WORKDIR /workspace/blog-view-ui
COPY blog-view-ui/package.json ./
RUN npm install --no-audit --no-fund
COPY blog-view-ui/ ./
# 同源部署：接口走根路径（VITE_API_URL=/）；
# 产物目录改为 assets（VITE_ASSETS_DIR=assets），避免与后台上传资源 /static/** 冲突。
RUN VITE_API_URL=/ VITE_ASSETS_DIR=assets npm run build

# ---------- 3. 构建内容管理后台（blog-cms-ui） ----------
FROM node:18 AS cms-build
WORKDIR /workspace/blog-cms-ui
COPY blog-cms-ui/package.json ./
RUN npm install --no-audit --no-fund
COPY blog-cms-ui/ ./
# 同源部署：后台挂载在 /cms/（--base=/cms/），接口走 /admin/（VITE_API_URL=/admin/）。
RUN VITE_API_URL=/admin/ npm run build -- --base=/cms/

# ---------- 4. 运行时（mysql:8.0 作为基础镜像 + 拷贝 JRE） ----------
FROM eclipse-temurin:8-jre-focal AS jre
FROM mysql:8.0 AS runtime

# 从 Temurin 镜像拷贝 JRE（Temurin 8-jre-focal 基于 glibc 2.31，兼容 mysql 镜像的 Oracle Linux）。
COPY --from=jre /opt/java/openjdk /opt/java/openjdk

ENV JAVA_HOME=/opt/java/openjdk \
    PATH=/opt/java/openjdk/bin:$PATH \
    TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    APP_HOME=/opt/blogloom

# 预建 conf / lib / bin 目录结构（static 由下方前端产物填充）。
RUN mkdir -p ${APP_HOME}/bin ${APP_HOME}/conf/logs ${APP_HOME}/conf/upload ${APP_HOME}/lib \
    ${APP_HOME}/sql/increment ${APP_HOME}/sql/local

# 启动与增量升级脚本。
COPY docker/container/bin/ ${APP_HOME}/bin/
# 镜像内置一份默认配置；compose 会用仓库 conf/ 下的同名文件挂载覆盖，便于运行时修改。
COPY conf/application.properties ${APP_HOME}/conf/application.properties
COPY conf/logback-spring.xml ${APP_HOME}/conf/logback-spring.xml
# 内置增量 SQL：standalone（仅拉镜像）部署无需挂载；仓库部署仍可挂载 ../sql/increment 覆盖。
COPY sql/increment/ ${APP_HOME}/sql/increment/

# 后端 jar 与两个前端产物。
COPY --from=backend-build /workspace/blog-backend/target/blog-backend-0.0.1.jar ${APP_HOME}/lib/blog-backend.jar
COPY --from=view-build /workspace/blog-view-ui/dist/ ${APP_HOME}/conf/static/view/
COPY --from=cms-build /workspace/blog-cms-ui/dist/ ${APP_HOME}/conf/static/cms/

RUN chmod +x ${APP_HOME}/bin/*.sh

# 以 APP_HOME 为工作目录：应用会据此定位 conf（user.dir.conf = /opt/blogloom/conf）。
WORKDIR ${APP_HOME}
EXPOSE 8090

# 默认启动命令：等待 MySQL → 执行增量 SQL → 启动后端服务。
ENTRYPOINT ["/opt/blogloom/bin/start.sh"]
