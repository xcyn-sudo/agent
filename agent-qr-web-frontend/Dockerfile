# ============================================
# Agent-QR 前端 Dockerfile (Vue 3 + Vite)
# 多阶段构建 — 生产部署
# ============================================

# ---- 阶段1: Node 构建 ----
FROM node:24-alpine AS build

WORKDIR /app

# 构建参数：API 基础路径
ARG VITE_API_BASE_URL=/api
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

# 安装依赖（利用 Docker 缓存层）
COPY package.json package-lock.json* ./
RUN npm ci --prefer-offline --no-audit --no-fund

# 复制源码并构建
COPY . .
RUN npm run build-only

# ---- 阶段2: Nginx 运行时 ----
FROM nginx:1.27-alpine

# 复制构建产物
COPY --from=build /app/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 健康检查
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
  CMD wget -q --spider http://localhost:80/ || exit 1

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
