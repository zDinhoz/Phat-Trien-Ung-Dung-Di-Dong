@echo off
cd /d "%~dp0"
set "BBB_HOST=%~1"
if not defined BBB_HOST set "BBB_HOST=192.168.137.118"
echo Ket noi BBB tai %BBB_HOST% - 7 FPS - toi da 2 tay
py -3.12 laptop_stream_client.py --host %BBB_HOST% --max-fps 7 --max-hands 2
if errorlevel 1 pause
