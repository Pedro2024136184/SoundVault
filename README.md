<p align="center">
  <img src="src/main/resources/icons/logo.png" alt="SoundVault Logo" width="120" height="120" style="border-radius: 20px;" />
</p>

<h1 align="center">SoundVault (Java Edition)</h1>

<p align="center">
  <strong>The Ultimate Hi-Res Music Vault, Library Organizer & Soulseek P2P Lossless Manager</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?logo=openjdk" alt="Java 21" />
  <img src="https://img.shields.io/badge/JavaFX-21-blue?logo=java" alt="JavaFX 21" />
  <img src="https://img.shields.io/badge/Soulseek-slskd%20API-red" alt="Soulseek slskd" />
  <img src="https://img.shields.io/badge/Spotify-Web%20API-1DB954?logo=spotify" alt="Spotify API" />
  <img src="https://img.shields.io/badge/Audio-FLAC%20Lossless-green" alt="Lossless FLAC" />
  <img src="https://img.shields.io/badge/Tests-31%20Passed-success" alt="Tests" />
  <img src="https://img.shields.io/badge/License-MIT-purple" alt="License" />
</p>

---

## Visão Geral

O **SoundVault** é uma aplicação desktop de alta performance desenvolvida em **Java 21** e **JavaFX 21** com recurso a **Virtual Threads (Project Loom)**. Foi desenhada para audiófilos, colecionadores e entusiastas musicais que pretendem catalogar a sua coleção física de CDs/Vinis, sincronizar a biblioteca do Spotify, indexar álbuns FLAC locais e descarregar automaticamente faixas e álbuns em qualidade Hi-Res através da rede descentralizada **Soulseek (slskd)**.

---

## Funcionalidades Principais

### 1. Sincronização & Análise Spotify
- **Liked Songs & Playlists**: Carrega faixas favoritas e playlists do utilizador através da API oficial do Spotify.
- **Deteção de Posse Local (`DISCO` / `FALTA`)**: Compara instantaneamente as faixas do Spotify com a tua pasta local de álbuns FLAC, indicando quais os álbuns que já possuis e quais faltam.
- **Perfil Psicoacústico**: Cálculo de perfis acústicos (Energia, Valence, Danceability, Acousticness, Instrumentalness) baseados nos géneros reais da tua conta.

### 2. Motor P2P Soulseek com Fallback Inteligente & Verificação Estrita
- **Pesquisa & Download Automático de FLACs**: Seleciona o melhor par na rede com base na velocidade, slots de envio livres e integridade do alinhamento do álbum.
- **Fallback para Álbuns Homónimos / 0 Resultados**:
  - Resolve problemas de álbuns homónimos (ex: *Deftones* dos *Deftones* ou *Metallica* dos *Metallica*).
  - Em caso de 0 resultados, obtém o alinhamento de faixas oficiais via Spotify e pesquisa pela primeira faixa (*Hexagram*).
  - **4 Filtros Rigorosos Anti-Erro**:
    1. **Verificação de Artista**: O caminho da pasta ou os ficheiros devem conter o nome do artista.
    2. **Multi-Track Cross Match**: A pasta tem de conter no mínimo 2 ou mais faixas coincidentes com o alinhamento oficial do álbum (rejeitando compilações acidentais como *Various Artists* ou *Greatest Hits* genéricos).
    3. **Mínimo de Faixas**: Rejeita ficheiros soltos e singles isolados.
    4. **Extensão FLAC**: Validação obrigatória de formato lossless.

### 3. Barra de Gestão de Downloads em Tempo Real (`DownloadManagerBar`)
- Barra permanente no fundo da aplicação com monitorização ao vivo:
  - Nome do ficheiro, diretório do álbum e peer atual.
  - Velocidade de transferência instantânea em tempo real (ex: `4.8 MB/s`).
  - Barra de progresso fluida com percentagem exata (`46%`).
  - Contadores ao vivo de ficheiros concluídos, a transferir e em fila.
  - **Gaveta Expansível (`▲ Detalhes`)**: Lista recolhível com progresso individual de cada ficheiro e botão de acesso direto ao Monitor Soulseek.

### 4. Coleção de Biblioteca Física (CDs & Vinis)
- Catálogo dedicado para exemplares físicos (CDs, Vinis, Edições Especiais, Digipaks).
- Deteção cruzada automática entre os CDs que tens em prateleira e os ficheiros digitais na tua biblioteca local (`FLAC LOCAL` a verde vs `FÍSICO`).
- Sincronização em segundo plano de capas originais e metadados via Spotify.
- Botão direto para pesquisar e descarregar a versão digital FLAC no Soulseek com 1 clique.

### 5. Lista de Desejos (Wishlist) Integrada
- Módulo de Wishlist persistente em disco (`cache/wishlist.json`).
- Botão **`+ Wishlist`** colocado imediatamente ao lado de todos os botões de download da aplicação (*Liked Songs*, *Playlists*, *Biblioteca Física*, *Relic Explorer*, *Modal de Capas*).
- **Migração Automática**: Ao marcar qualquer item na Wishlist como `Adquirido`, o álbum é transferido automaticamente para a tua **Biblioteca Física** com capas e metadados preenchidos.

### 6. Visualizador Modal de Artwork em Alta Resolução (`ArtworkModal`)
- Clicar na capa de qualquer álbum/música abre uma janela modal com fundo escurecido e a imagem oficial em resolução máxima (até **640x640**).
- Acesso rápido no próprio modal para adicionar à Wishlist e iniciar download FLAC via Soulseek.
- Fecho intuitivo com `ESC`, botão `✕` ou clique exterior.

### 7. Notificações Nativas do Windows
- Integração nativa com a barra de tarefas do Windows via `java.awt.SystemTray`.
- Emite alertas quando:
  - Uma transferência termina no Soulseek.
  - Um álbum é adicionado à Lista de Desejos.
  - Ocorre sincronização de capas.

### 8. Motor com 25 Temas Visuais
- Suporte a 25 estilos visuais completos com troca em tempo real:
  - *Neobrutalist*, *Swiss/International*, *Editorial*, *Glassmorphism*, *Retro-futuristic*, *Bauhaus*, *Art Deco*, *Minimal*, *Flat*, *Material*, *Neumorphic*, *Monochromatic*, *Scandinavian*, *Japandi*, *Cyberpunk*, *Synthwave*, *Dark OLED*, etc.
  - Botão de **Tema Aleatório** para alternar de forma dinâmica.

### 9. Relic Explorer & Deep Dive
- **Relic Explorer**: Pesquisa de raridades musicais no Soulseek divididas por categorias: Vinil (Vinyl Rips), Demos / Tapes, Faixas Inéditas, Bootlegs / Live e Edições Japonesas.
- **Deep Dive**: Exploração profunda da discografia completa de qualquer artista com busca automática de álbuns de estúdio e singles.

---

## Arquitetura & Tecnologias

| Componente | Tecnologia | Função |
|---|---|---|
| **Linguagem** | Java 21 (LTS) | Virtual Threads (`Project Loom`), Records, Pattern Matching |
| **Interface Gráfica** | JavaFX 21 | FXML-free, componentes modulares, CSS temático reativo |
| **P2P Backend** | slskd API (v0) | Cliente headless do Soulseek via REST API HTTP/2 |
| **Metadata & Streaming** | Spotify Web API | Autenticação OAuth2 PKCE com renovação de tokens |
| **Tagging & Metadados** | JAudiotagger | Leitura de tags FLAC/VorbisComment em disco |
| **Serialização** | Jackson 2.17 | JSON/YAML para persistência e cache |
| **Build & Testes** | Maven Wrapper & JUnit 5 | 31 testes unitários automatizados |

---

## Estrutura do Projeto

```
SoundVaultJava/
├── src/
│   ├── main/
│   │   ├── java/com/soundvault/
│   │   │   ├── Main.java                      # Ponto de entrada da aplicação
│   │   │   ├── config/                        # Configurações .env e YAML do slskd
│   │   │   ├── model/                         # Records imutáveis (Album, Track, WishlistItem, etc.)
│   │   │   ├── service/                       # Serviços assíncronos (Spotify, Slskd, Library, etc.)
│   │   │   ├── ui/
│   │   │   │   ├── SoundVaultApp.java         # Shell JavaFX e barra lateral
│   │   │   │   ├── components/                # DownloadManagerBar, ArtworkModal, TrackListCell
│   │   │   │   └── views/                     # Vistas principais (LikedSongs, Wishlist, PhysicalLibrary, etc.)
│   │   │   └── util/                          # StringNormalizer, AppLogger
│   │   └── resources/
│   │       ├── css/                           # Folha de estilo e variáveis de tema
│   │       └── icons/                         # Logotipo e ícone oficial
│   └── test/java/com/soundvault/              # 31 testes unitários (JUnit 5)
├── .env.example                               # Modelo de variáveis de ambiente
├── .gitignore                                 # Proteção de segredos e exclusão de binários
├── pom.xml                                    # Dependências e plugins Maven
└── iniciar.bat                                # Script de arranque rápido
```

---

## Como Configurar e Executar

### Pré-requisitos
- **Java JDK 21** ou superior instalado e configurado no `PATH`.
- Instância do **slskd** (Soulseek Daemon) a correr (normalmente na porta `5030`).

### 1. Clonar o Repositório
```bash
git clone https://github.com/Pedro2024136184/SoundVault.git
cd SoundVault
```

### 2. Configurar o Ficheiro `.env`
Copia o ficheiro de exemplo `.env.example` para `.env`:
```bash
copy .env.example .env
```

Edita o `.env` com as tuas definições:
```ini
# Credenciais da API do Spotify (developer.spotify.com)
SPOTIPY_CLIENT_ID=teu_client_id_aqui
SPOTIPY_CLIENT_SECRET=teu_client_secret_aqui
SPOTIPY_REDIRECT_URI=http://127.0.0.1:8080/callback

# Configuração do slskd (Soulseek)
SLSKD_HOST=http://localhost
SLSKD_PORT=5030
SLSKD_API_KEY=tua_slskd_api_key_aqui
SLSKD_EXE_PATH=C:\caminho\para\slskd.exe

# Caminho da tua biblioteca local de músicas
LOCAL_LIBRARY_PATH=C:/Users/teu_user/Music/Albums
```

### 3. Compilar e Executar os Testes
```powershell
.\mvnw.cmd test
```
> Todos os 31 testes unitários serão executados, validando a integridade das queries, modelos, persistência e filtros anti-erro de downloads.

### 4. Iniciar a Aplicação
Podes iniciar diretamente via Maven Wrapper:
```powershell
.\mvnw.cmd javafx:run
```
Ou executar o script:
```powershell
.\iniciar.bat
```

---

## Segurança

- O ficheiro `.gitignore` está configurado para **nunca** enviar ficheiros de credenciais (`.env`), tokens de autenticação (`.cache`) ou a cache de capas (`cache/`).
- Para partilhar o projeto ou colaborar, utiliza sempre o `.env.example`.

---

## Licença

Este projeto é disponibilizado sob a licença [MIT](LICENSE).
Desenvolvido para gestão e preservação audiófila de coleções musicais.
