@echo off
chcp 65001 >nul
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"
call mvnw.cmd javafx:run
