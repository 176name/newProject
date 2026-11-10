@echo off
chcp 65001 >nul
title HealingStream 后端 (8081)
cd /d D:\HealingStream\Streaming\backend
"C:\Users\DWMDN\.jdks\corretto-1.8.0_482\bin\java.exe" -jar target\backend-1.0-SNAPSHOT.jar
