# 🚀 Guia Passo-a-Passo para Iniciantes (Como colocar o SoundVault a funcionar)

Se deste um clone (ou descarregaste) o projeto e queres pô-lo a funcionar rapidamente, vieste ao sítio certo! Tornámos este projeto o mais **portátil e automático** possível. Não precisas de instalar programas de programação complexos.

Segue este guia com calma, passo a passo.

---

## Passo 1: Instalar o Java (A "Linguagem" do Projeto)
O teu computador precisa de ter o Java instalado para conseguir arrancar a aplicação.
*(Nota: Se já tens o IntelliJ ou outro editor instalado que use Java, o nosso projeto vai tentar encontrá-lo sozinho! Mas o ideal é instalares no sistema).*

1. Vai ao site oficial do Adoptium: [https://adoptium.net/](https://adoptium.net/)
2. Clica no botão para transferir o **JDK 21** para Windows.
3. Abre o ficheiro que descarregaste e instala-o.
4. **MUITO IMPORTANTE:** Durante a instalação, certifica-te de que ativas a opção **"Add to PATH"** (Adicionar à variável PATH do sistema). Isto garante que o Windows encontra o Java facilmente.
5. Conclui a instalação.

---

## Passo 2: Criar o ficheiro de Configurações Secretas (`.env`)
O projeto precisa de saber as tuas chaves de acesso do Spotify e a pasta onde guardas as músicas. Por questões de segurança, este ficheiro não vem pronto no GitHub.

1. Abre a pasta do projeto `SoundVault` no teu Explorador de Ficheiros.
2. Vais ver um ficheiro chamado `.env.example`.
3. Faz uma **cópia** desse ficheiro.
4. Muda o nome dessa cópia para `.env` (apaga o ".example").
   - *Atenção: o nome do ficheiro é só mesmo `.env` (começa com um ponto).*
5. Abre esse ficheiro `.env` no **Bloco de Notas** (Notepad). Vamos preenchê-lo nos próximos passos!

---

## Passo 3: Obter as chaves do Spotify (Passo-a-passo detalhado)
Para que o SoundVault consiga ler as tuas Playlists e Músicas Gostadas, precisamos de gerar uma chave privada na tua conta do Spotify. É grátis e seguro.

1. Vai a [developer.spotify.com](https://developer.spotify.com/) e clica em **"Log in"** (usa a tua conta normal do Spotify).
2. No canto superior direito, clica no teu perfil e vai a **"Dashboard"**.
3. Clica no botão **"Create app"** (Criar aplicação).
4. Preenche os campos assim:
   - **App name:** `SoundVault`
   - **App description:** `A minha biblioteca de música`
   - **Website:** (podes deixar em branco)
   - **Redirect URIs:** *(MUITO IMPORTANTE)* Escreve exatamente `http://127.0.0.1:8080/callback` e clica em "Add".
5. Seleciona a caixa de verificação dos Termos de Serviço e clica em **"Save"**.
6. Agora, na página da tua nova App, clica no botão **"Settings"** (Definições).
7. Lá dentro, vais ver o teu **Client ID**. Copia essa mistura de letras e números.
   - Vai ao teu ficheiro `.env` e cola à frente de `SPOTIPY_CLIENT_ID=...`
8. Logo por baixo do Client ID, clica em **"View client secret"** para revelar a tua chave secreta. Copia.
   - Vai ao teu ficheiro `.env` e cola à frente de `SPOTIPY_CLIENT_SECRET=...`

---

## Passo 4: O slskd (Motor do Soulseek)
O SoundVault usa o motor invisível do Soulseek (chamado `slskd`) para descarregar músicas com máxima qualidade (FLAC).

1. Descarrega a versão mais recente do `slskd` para Windows no Github oficial: [https://github.com/slskd/slskd/releases](https://github.com/slskd/slskd/releases) (Procura o ficheiro `slskd-x.x.x-win-x64.zip`).
2. Extrai a pasta para onde quiseres (ex: `C:\slskd`) e executa o `slskd.exe`.
3. Vai abrir uma janela preta. Depois disso, abre o teu navegador de internet e vai a `http://localhost:5030`.
4. Faz a configuração inicial do slskd e faz login com a tua conta do Soulseek.
5. Na página web do slskd, vai a **Options** -> **Web/API**.
6. Clica em **"Generate Key"** (Gerar Chave de API). Copia essa chave.
7. Vai ao teu ficheiro `.env` e preenche:
   - `SLSKD_API_KEY=...` (cola a chave aqui)
   - `SLSKD_EXE_PATH=C:\caminho\para\a\pasta\slskd.exe` (põe aqui o caminho completo onde guardaste o ficheiro exe no passo 2).

---

## Passo 5: A tua Pasta de Músicas
No ficheiro `.env`, só falta preencher a configuração da tua biblioteca local.

- Procura a linha `LOCAL_LIBRARY_PATH=`
- Coloca à frente o caminho completo onde costumas guardar as tuas músicas descarregadas. Exemplo: `C:\Users\O_Teu_Nome\Music`

**Guarda as alterações no Bloco de Notas e podes fechá-lo!** O `.env` está pronto.

---

## Passo 6: Iniciar o SoundVault! 🎉

Já fizeste a parte difícil. Como o projeto é portátil e inteligente, a partir de agora é incrivelmente simples iniciar a aplicação para o resto da vida!

1. Na pasta do projeto, dá dois cliques no ficheiro **`iniciar.bat`**.
2. O script vai verificar automaticamente se tens tudo o que é preciso (na primeira vez, ele vai descarregar o motor Maven e organizar tudo sozinho — pode demorar uns segundinhos).
3. E pronto! A janela do **SoundVault** vai abrir-se automaticamente à tua frente.

Sempre que quiseres usar o programa de futuro, basta dar duplo clique no `iniciar.bat`! Se te esqueceres do ecrã preto aberto, não faz mal, é o "motor" do programa a funcionar.
