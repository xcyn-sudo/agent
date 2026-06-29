# ============================================
# Agent-QR 后端 Dockerfile (Spring Boot)
# 多阶段构建 — 生产部署
# ============================================

# ---- 阶段1: Maven 构建 ----
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 设置 Maven 内存，避免 OOM（云服务器构建时）
ENV MAVEN_OPTS="-Xmx768m -XX:MaxMetaspaceSize=256m"

# 一次性复制所有 Maven 相关文件
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./

# 复制所有模块的 pom.xml（含 P2 新增模块）
COPY agent-qr-common/pom.xml       agent-qr-common/
COPY agent-qr-auth/pom.xml          agent-qr-auth/
COPY agent-qr-user/pom.xml          agent-qr-user/
COPY agent-qr-knowledge/pom.xml     agent-qr-knowledge/
COPY agent-qr-rag/pom.xml           agent-qr-rag/
COPY agent-qr-statistics/pom.xml    agent-qr-statistics/
COPY agent-qr-web/pom.xml           agent-qr-web/
COPY agent-qr-compensation/pom.xml  agent-qr-compensation/
COPY agent-qr-datasource/pom.xml    agent-qr-datasource/
COPY agent-qr-data-quality/pom.xml  agent-qr-data-quality/
COPY agent-qr-etl/pom.xml           agent-qr-etl/
COPY agent-qr-catalog/pom.xml       agent-qr-catalog/

# ★ 依赖缓存层：下载所有依赖到本地仓库，源码变更时不重复下载
RUN mvn dependency:go-offline -pl agent-qr-web -am -Dmaven.test.skip=true

# 复制全部模块源码（含 P2 新增模块）
COPY agent-qr-common/src       agent-qr-common/src
COPY agent-qr-auth/src          agent-qr-auth/src
COPY agent-qr-user/src          agent-qr-user/src
COPY agent-qr-knowledge/src     agent-qr-knowledge/src
COPY agent-qr-rag/src           agent-qr-rag/src
COPY agent-qr-statistics/src    agent-qr-statistics/src
COPY agent-qr-web/src           agent-qr-web/src
COPY agent-qr-compensation/src  agent-qr-compensation/src
COPY agent-qr-datasource/src    agent-qr-datasource/src
COPY agent-qr-data-quality/src  agent-qr-data-quality/src
COPY agent-qr-etl/src           agent-qr-etl/src
COPY agent-qr-catalog/src       agent-qr-catalog/src

# 构建：只构建 web 模块及其依赖模块，跳过测试
RUN mvn clean package -pl agent-qr-web -am -Dmaven.test.skip=true

# ---- 阶段2: 运行时镜像 ----
FROM eclipse-temurin:21-jre-alpine

# 安装 curl 用于健康检查
RUN apk add --no-cache curl

# 创建非 root 用户
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 复制构建产物
COPY --from=builder /build/agent-qr-web/target/*.jar app.jar

# 创建上传目录
RUN mkdir -p /app/uploads && chown -R appuser:appgroup /app

USER appuser

EXPOSE 9090

# 健康检查
HEALTHCHECK --interval=30s --timeout=10s --start-period=90s --retries=5 \
  CMD curl -f -s -o /dev/null http://localhost:9090/api/auth/login || exit 1

# JAVA_OPTS 通过 docker-compose 环境变量注入
# 2C4G 推荐: -Xms192m -Xmx384m -XX:MaxMetaspaceSize=128m
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:- -Xms256m -Xmx512m} -jar app.jar"]
