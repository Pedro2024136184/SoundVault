$ErrorActionPreference = "Stop"
$ScriptDir = $PSScriptRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "   🎵 BEM-VINDO AO INSTALADOR AUTOMATICO SOUNDVAULT 🎵" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "Este assistente vai preparar o teu projeto automaticamente."
Write-Host "Nao precisas de instalar nada no computador, vou tornar isto portatil!"
Write-Host ""
Start-Sleep -Seconds 3

# Limpar downloads incompletos
if (Test-Path "$ScriptDir\.jdk\java.zip") { Remove-Item "$ScriptDir\.jdk\java.zip" -Force }

# 1. Setup Java 21 Portable
$jdkDir = "$ScriptDir\.jdk"
$javaFound = $false

# Verifica se ja sacamos a versao portatil antes
if (Test-Path "$jdkDir\jdk-21*\bin\java.exe") {
    $javaFound = $true
}

if (-not $javaFound) {
    Write-Host "[1/5] A transferir motor Java 21 Portatil... (Demora um bocadinho)" -ForegroundColor Yellow
    if (-not (Test-Path $jdkDir)) { New-Item -ItemType Directory -Path $jdkDir | Out-Null }
    
    # Download Adoptium JDK 21
    $javaUrl = "https://github.com/adoptium/temurin21-binaries/releases/download/jdk-21.0.6%2B7/OpenJDK21U-jdk_x64_windows_hotspot_21.0.6_7.zip"
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $javaUrl -OutFile "$jdkDir\java.zip"
    
    Write-Host "      A extrair o Java..." -ForegroundColor DarkGray
    Expand-Archive -Path "$jdkDir\java.zip" -DestinationPath $jdkDir -Force
    Remove-Item "$jdkDir\java.zip"
    Write-Host "      [OK] Java Portatil instalado com sucesso!" -ForegroundColor Green
} else {
    Write-Host "[1/5] Java Portatil ja detetado." -ForegroundColor Green
}

# 2. Setup slskd Portable
$slskdDir = "$ScriptDir\.slskd"
$slskdExe = "$slskdDir\slskd.exe"

if (-not (Test-Path $slskdExe)) {
    Write-Host ""
    Write-Host "[2/5] A transferir Motor Soulseek (slskd)..." -ForegroundColor Yellow
    if (-not (Test-Path $slskdDir)) { New-Item -ItemType Directory -Path $slskdDir | Out-Null }
    
    $slskdUrl = "https://github.com/slskd/slskd/releases/download/0.21.2/slskd-0.21.2-win-x64.zip"
    Invoke-WebRequest -Uri $slskdUrl -OutFile "$slskdDir\slskd.zip"
    
    Write-Host "      A extrair o slskd..." -ForegroundColor DarkGray
    Expand-Archive -Path "$slskdDir\slskd.zip" -DestinationPath $slskdDir -Force
    Remove-Item "$slskdDir\slskd.zip"
    Write-Host "      [OK] slskd Portatil instalado com sucesso!" -ForegroundColor Green
} else {
    Write-Host ""
    Write-Host "[2/5] Motor Soulseek ja detetado." -ForegroundColor Green
}

# 3. Criar .env
Write-Host ""
$envFile = "$ScriptDir\.env"
if (-not (Test-Path $envFile)) {
    Write-Host "[3/5] A gerar ficheiro de configuracoes .env..." -ForegroundColor Yellow
    Copy-Item "$ScriptDir\.env.example" $envFile
} else {
    Write-Host "[3/5] Ficheiro .env ja existe. Vou atualiza-lo." -ForegroundColor Yellow
}

$envContent = Get-Content $envFile -Raw

function Prompt-User($message) {
    $val = ""
    while ([string]::IsNullOrWhiteSpace($val)) {
        Write-Host -NoNewline "$($message): " -ForegroundColor Magenta
        $val = Read-Host
    }
    return $val
}

# 4. Spotify API
Write-Host ""
Write-Host "[4/5] Configuracao do Spotify" -ForegroundColor Cyan
Write-Host "Vou abrir o site para programadores do Spotify (dashboard)."
Write-Host "Inicia sessao, clica em 'Create App' com o nome 'SoundVault'."
Write-Host "IMPORTANTE: Em 'Redirect URIs' adiciona EXATAMENTE isto:"
Write-Host "http://127.0.0.1:8080/callback" -ForegroundColor Yellow
Write-Host "(A aguardar 5 segundos antes de abrir o browser...)" -ForegroundColor DarkGray
Start-Sleep -Seconds 5
Start-Process "https://developer.spotify.com/dashboard"

$clientId = Prompt-User "-> Cola aqui o teu SPOTIPY_CLIENT_ID (das Settings da App)"
$clientSecret = Prompt-User "-> Cola aqui o teu SPOTIPY_CLIENT_SECRET (clica em View Client Secret)"

# 5. slskd API e Pastas
Write-Host ""
Write-Host "[5/5] Configuracao do Soulseek (slskd.yml) e Pastas" -ForegroundColor Cyan
$slskdYmlDir = "$env:LOCALAPPDATA\slskd"
$slskdYmlPath = "$slskdYmlDir\slskd.yml"

# Fechar slskd se estiver aberto para podermos modificar o ficheiro
Stop-Process -Name "slskd" -Force -ErrorAction SilentlyContinue

$utf8NoBom = New-Object System.Text.UTF8Encoding($False)

Write-Host "Vou pedir-te todos os dados necessários para o Soulseek e para a aplicação."
$slskdUser = Prompt-User "-> Username Soulseek"
$slskdPass = Prompt-User "-> Password Soulseek"

$apiKey = ""
while ($apiKey.Length -lt 16) {
    $apiKey = Prompt-User "-> Soulseek API Key (MINIMO 16 CARACTERES OBRIGATORIO)"
    if ($apiKey.Length -lt 16) {
        Write-Host "Erro: A API Key introduzida tem menos de 16 caracteres. Tenta de novo." -ForegroundColor Red
    }
}

$libPathRaw = Prompt-User "-> Pasta de Downloads / Biblioteca Local (Ex: C:\Users\username\Music\Albums)"
$incPathRaw = Prompt-User "-> Pasta de Incompletos (Ex: C:\Users\username\Music\Incomplete)"

# Formatar caminhos para o YAML (usar duplas barras) e para o .env (usar barras normais)
$libPathYaml = $libPathRaw.Replace("\", "\\")
$incPathYaml = $incPathRaw.Replace("\", "\\")
$libPathEnv = $libPathRaw.Replace("\", "/")

# Assumimos a partilha como a pasta pai dos Downloads ou a propria pasta de downloads
$sharePathYaml = $libPathYaml

if (-not (Test-Path $slskdYmlDir)) { New-Item -ItemType Directory -Path $slskdYmlDir | Out-Null }
    
$ymlContent = @"
directories:
  incomplete: $incPathYaml
  downloads: $libPathYaml
shares:
  directories:
    - $sharePathYaml
soulseek:
  username: $slskdUser
  password: '$slskdPass'
  listenPort: 50300
web:
  authentication:
    disabled: false
    username: slskd
    password: slskd
    apiKeys:
      myapikey:
        key: $apiKey
        role: administrator
        cidr: 0.0.0.0/0,::/0
global:
  download:
    slots: 100
    speedLimit: 2000000
  upload:
    slots: 5
    speedLimit: 2000000
"@

[System.IO.File]::WriteAllText($slskdYmlPath, $ymlContent, $utf8NoBom)
Write-Host "      [OK] slskd.yml recriado do ZERO com todos os teus campos!" -ForegroundColor Green

# Substituir valores no .env
$envContent = $envContent -replace "(?m)^SPOTIPY_CLIENT_ID=.*", "SPOTIPY_CLIENT_ID=$clientId"
$envContent = $envContent -replace "(?m)^SPOTIPY_CLIENT_SECRET=.*", "SPOTIPY_CLIENT_SECRET=$clientSecret"
$envContent = $envContent -replace "(?m)^SLSKD_API_KEY=.*", "SLSKD_API_KEY=$apiKey"
$envContent = $envContent -replace "(?m)^SLSKD_EXE_PATH=.*", "SLSKD_EXE_PATH=$($slskdExe.Replace('\', '/'))"
$envContent = $envContent -replace "(?m)^LOCAL_LIBRARY_PATH=.*", "LOCAL_LIBRARY_PATH=$libPathEnv"
$envContent = $envContent -replace "(?m)^SLSKD_PORT=.*", "SLSKD_PORT=5030"
$envContent = $envContent -replace "(?m)^SLSKD_HOST=.*", "SLSKD_HOST=http://localhost"

# Se as chaves do HOST/PORT nao existissem de todo no .env antigo, adiciona-as no fundo
if ($envContent -notmatch "SLSKD_PORT=") { $envContent += "`r`nSLSKD_PORT=5030" }
if ($envContent -notmatch "SLSKD_HOST=") { $envContent += "`r`nSLSKD_HOST=http://localhost" }

Set-Content -Path $envFile -Value $envContent

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "   🚀 INSTALACAO AUTOMATICA CONCLUIDA COM SUCESSO! 🚀" -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
Write-Host "Tudo configurado! Podes fechar esta janela."
Write-Host "A partir de agora, abre o 'iniciar.bat' e os campos vao aparecer preenchidos."
Write-Host ""
