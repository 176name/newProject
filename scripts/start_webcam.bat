@echo off
chcp 65001 >nul
title 摄像头推流 - 笔记本摄像头 -&gt; ZLMediaKit (rtsp://192.168.6.145:554/live/webcam)
echo [%date% %time%] 启动摄像头推流，断线自动重连，关闭本窗口即停止
:loop
ffmpeg -hide_banner -loglevel warning -f dshow -i video="Integrated Camera" -c:v libx264 -preset ultrafast -tune zerolatency -pix_fmt yuv420p -g 25 -f rtsp -rtsp_transport tcp rtsp://192.168.6.145:554/live/webcam
echo [%date% %time%] 推流中断，3秒后重连...
timeout /t 3 /nobreak >nul
goto loop
