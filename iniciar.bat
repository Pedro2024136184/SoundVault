@echo off
chcp 65001 >nul
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

:: Tenta encontrar o Java caso o utilizador nao tenha configurado o JAVA_HOME no Windows
if "%JAVA_HOME%"=="" (
    :: Procura primeiro na pasta portatil criada pelo instalador
    for /d %%i in ("%SCRIPT_DIR%.jdk\*") do (
        if exist "%%i\bin\java.exe" (
            set "JAVA_HOME=%%i"
            echo [SoundVault] Java Portatil detetado em: %%i
            goto :javaset
        )
    )
    :: Depois procura nos JDKs do IntelliJ
    for /d %%i in ("%USERPROFILE%\.jdks\*") do (
        if exist "%%i\bin\java.exe" (
            set "JAVA_HOME=%%i"
            echo [SoundVault] Java detetado automaticamente em: %%i
            goto :javaset
        )
    )
)
:javaset

call mvnw.cmd javafx:run
pause
