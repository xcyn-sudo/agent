# ============================================
# Agent-QR 后端 Dockerfile (Spring Boot)
# 多阶段构建 — P1 阶段
# ============================================

# ---- 阶段1: Maven 构建 ----
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# 一次性复制所有 Maven 相关文件
COPY pom.xml ./
COPY .mvn .mvn
COPY mvnw mvnw.cmd ./

COPY agent-qr-common/pom.xml agent-qr-common/
COPY agent-qr-auth/pom.xml    agent-qr-auth/
COPY agent-qr-user/pom.xml    agent-qr-user/
COPY agent-qr-knowledge/pom.xml agent-qr-knowledge/
COPY agent-qr-rag/pom.xml     agent-qr-rag/
COPY agent-qr-statistics/pom.xml agent-qr-statistics/
COPY agent-qr-web/pom.xml     agent-qr-web/

# 复制全部源码
COPY agent-qr-common/src agent-qr-common/src
COPY agent-qr-auth/src    agent-qr-auth/src
COPY agent-qr-user/src    agent-qr-user/src
COPY agent-qr-knowledge/src agent-qr-knowledge/src
COPY agent-qr-rag/src     agent-qr-rag/src
COPY agent-qr-statistics/src agent-qr-statistics/src
COPY agent-qr-web/src     agent-qr-web/src

# 一步构建：package 会自动下载依赖
# -pl agent-qr-web -am: 只构建 web 模块及其依赖模块
# -Dmaven.test.skip=true: 跳过测试
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

ENTRYPOINT ["java", "-jar", "app.jar"]
