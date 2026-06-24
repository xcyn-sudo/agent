@echo off
REM ============================================
REM Agent-QR Docker 一键启动脚本 (Windows)
REM P1 阶段
REM
REM 用法:
REM   start.bat             启动所有服务
REM   start.bat stop         停止所有服务
REM   start.bat restart      重启所有服务
REM   start.bat logs         查看日志
REM   start.bat status       查看服务状态
REM   start.bat clean        清理所有容器和数据
REM ============================================

setlocal enabledelayedexpansion

set COMPOSE_FILE=docker-compose.yml
set PROJECT_NAME=agent-qr

REM 检查 Docker 环境
where docker >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo 错误: 未找到 Docker，请先安装 Docker Desktop
    exit /b 1
)

docker compose version >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo 错误: 需要 Docker Compose v2，请升级 Docker Desktop
    exit /b 1
)

REM 检查 .env 文件
if not exist .env (
    echo 提示: 未找到 .env 文件，从 .env.example 创建默认配置...
    if exist .env.example (
        copy .env.example .env >nul
        echo 已创建 .env 文件，请编辑其中的敏感配置后重新运行
        echo 尤其是: DEEPSEEK_API_KEY, JWT_SECRET
        exit /b 0
    ) else (
        echo 警告: 未找到 .env.example，将使用默认配置
    )
)

REM 创建上传目录
if not exist uploads mkdir uploads

goto :%1 2>nul || goto :start

:start
echo ==========================================
echo   Agent-QR 容器化部署 (P1 阶段^)
echo ==========================================
echo.
docker compose -f %COMPOSE_FILE% up -d --build
echo.
echo ==========================================
echo   服务启动中...
echo ==========================================
echo   MySQL:        localhost:3308
echo   ChromaDB:     localhost:8000
echo   Backend API:  http://localhost:9090
echo ==========================================
echo.
echo 提示: 使用 'start.bat logs' 查看实时日志
echo 提示: 使用 'start.bat status' 查看服务状态
goto :eof

:stop
echo 停止所有服务...
docker compose -f %COMPOSE_FILE% down
echo 服务已停止
goto :eof

:restart
call :stop
call :start
goto :eof

:logs
docker compose -f %COMPOSE_FILE% logs -f --tail=100
goto :eof

:status
docker compose -f %COMPOSE_FILE% ps
goto :eof

:clean
echo 警告: 将删除所有容器、数据卷和镜像!
set /p confirm="确认继续? (y/N) "
if /i "%confirm%"=="y" (
    docker compose -f %COMPOSE_FILE% down -v --rmi all
    echo 已清理所有容器、数据卷和镜像
    echo 注意: uploads 目录未被删除，请手动处理: rmdir /s uploads\
) else (
    echo 已取消
)
goto :eof

:default
echo 用法: start.bat {start^|stop^|restart^|logs^|status^|clean}
echo.
echo   start   启动所有服务（默认）
echo   stop    停止所有服务
echo   restart 重启所有服务
echo   logs    查看实时日志
echo   status  查看服务状态
echo   clean   清理所有容器和数据（不可逆）
goto :eof
