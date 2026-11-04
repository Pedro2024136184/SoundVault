@echo off
set "SCRIPT_DIR=%~dp0"
set "MAVEN_DIR=%SCRIPT_DIR%.maven\apache-maven-3.9.9"

if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
    echo [SoundVault] A transferir o Maven pela primeira vez... (Isto so acontece uma vez)
    if not exist "%SCRIPT_DIR%.maven" mkdir "%SCRIPT_DIR%.maven"
    powershell -Command "[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.zip' -OutFile '%SCRIPT_DIR%.maven\maven.zip'"
    powershell -Command "Expand-Archive -Path '%SCRIPT_DIR%.maven\maven.zip' -DestinationPath '%SCRIPT_DIR%.maven' -Force"
    del "%SCRIPT_DIR%.maven\maven.zip"
    echo [SoundVault] Maven transferido com sucesso!
)

"%MAVEN_DIR%\bin\mvn.cmd" %*
