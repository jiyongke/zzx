@echo off
chcp 65001 >nul
cd /d "%~dp0"
echo 正在启动橙心助农前端 (http://localhost:3000) ...
echo 关闭本窗口即可停止服务
echo.
npm run dev
pause
