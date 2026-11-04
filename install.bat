@echo off
chcp 65001 >nul
echo A iniciar o Instalador Inteligente...
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0install.ps1"
pause
