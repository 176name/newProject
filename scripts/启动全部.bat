@echo off
chcp 65001 >nul
title HealingStream 一键启动
echo ==========================================
echo   HealingStream 智能监控平台 - 一键启动
echo ==========================================
echo.
echo [前置检查]
ping -n 1 -w 800 192.168.6.145 >nul 2>&1 && (echo   虚拟机 192.168.6.145 在线  [OK]) || (echo   虚拟机不在线！请先在 VMware 中启动 CentOS 7 虚拟机)
netstat -an | findstr ":3306 " >nul 2>&1 && (echo   MySQL 3306 在线  [OK]) || (echo   MySQL 未启动！请以管理员身份运行:  net start MySQL80)
echo.
echo 正在依次启动 4 个服务（每个一个窗口）：
start "Backend-8081" cmd /k "D:\HealingStream\Streaming\scripts\start_backend.bat"
timeout /t 3 /nobreak >nul
start "AI-5001" cmd /k "D:\HealingStream\Streaming\scripts\start_ai.bat"
start "Front-5173" cmd /k "D:\HealingStream\Streaming\scripts\start_front.bat"
timeout /t 3 /nobreak >nul
start "Webcam-Push" cmd /k "D:\HealingStream\Streaming\scripts\start_webcam.bat"
echo.
echo 等约 30 秒后浏览器打开:  http://localhost:5173
echo 登录账号: admin  （密码见 docs\环境恢复与启动指南.md 或询问本人）
echo 关闭各窗口 = 停止对应服务
echo.
pause
