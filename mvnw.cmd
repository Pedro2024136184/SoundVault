@echo off
set "SCRIPT_DIR=%~dp0"
"%SCRIPT_DIR%.maven\apache-maven-3.9.9\bin\mvn.cmd" %*
