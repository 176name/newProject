@echo off
chcp 65001 >nul
title HealingStream AI引擎 (5001)
cd /d D:\HealingStream\Streaming\ai_engine
set PYTHONIOENCODING=utf-8
python server.py
