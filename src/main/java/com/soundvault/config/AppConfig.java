package com.soundvault.config;

import com.soundvault.util.AppLogger;
import io.github.cdimascio.dotenv.Dotenv;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class AppConfig {

    private static final String ENV_FILE_NAME = ".env";
    private static AppConfig instance;

    private String spotifyClientId = "";
    private String spotifyClientSecret = "";
    private String spotifyRedirectUri = "http://127.0.0.1:8080/callback";

    private String slskdHost = "http://localhost";
    private int slskdPort = 5030;
    private String slskdApiKey = "";
    private String slskdExePath = "";

    private String localLibraryPath = "";

    private AppConfig() {
        load();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public static String getDefaultMusicPath() {
        return Paths.get(System.getProperty("user.home"), "Music", "Albums").toAbsolutePath().toString();
    }

    public static String cleanValue(String val) {
        if (val == null) return "";
        val = val.trim();
        if ((val.startsWith("'") && val.endsWith("'")) || (val.startsWith("\"") && val.endsWith("\""))) {
            if (val.length() >= 2) {
                val = val.substring(1, val.length() - 1).trim();
            }
        }
        return val;
    }

    public synchronized void load() {
        Path envPath = Paths.get(ENV_FILE_NAME);
        if (!Files.exists(envPath)) {
            createDefaultEnvFile(envPath);
        }

        try {
            Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
            this.spotifyClientId = cleanValue(dotenv.get("SPOTIPY_CLIENT_ID", ""));
            this.spotifyClientSecret = cleanValue(dotenv.get("SPOTIPY_CLIENT_SECRET", ""));
            this.spotifyRedirectUri = cleanValue(dotenv.get("SPOTIPY_REDIRECT_URI", "http://127.0.0.1:8080/callback"));

            this.slskdHost = cleanValue(dotenv.get("SLSKD_HOST", "http://localhost"));
            String portStr = cleanValue(dotenv.get("SLSKD_PORT", "5030"));
            try {
                this.slskdPort = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                this.slskdPort = 5030;
            }

            this.slskdApiKey = cleanValue(dotenv.get("SLSKD_API_KEY", ""));
            this.slskdExePath = cleanValue(dotenv.get("SLSKD_EXE_PATH", ""));

            String lib = cleanValue(dotenv.get("LOCAL_LIBRARY_PATH", ""));
            if (!lib.isBlank() && Files.exists(Paths.get(lib))) {
                this.localLibraryPath = lib;
            } else {
                Path defaultAlbums = Paths.get(getDefaultMusicPath());
                if (Files.exists(defaultAlbums)) {
                    this.localLibraryPath = defaultAlbums.toAbsolutePath().toString();
                } else {
                    Path userMusic = Paths.get(System.getProperty("user.home"), "Music");
                    if (Files.exists(userMusic)) {
                        this.localLibraryPath = userMusic.toAbsolutePath().toString();
                    } else {
                        this.localLibraryPath = (!lib.isBlank()) ? lib : getDefaultMusicPath();
                    }
                }
            }

            AppLogger.log("Configurações do .env carregadas com sucesso. Biblioteca local: " + this.localLibraryPath);
        } catch (Exception e) {
            AppLogger.log("Aviso ao carregar .env: " + e.getMessage());
        }
    }

    public synchronized void save() throws IOException {
        Path envPath = Paths.get(ENV_FILE_NAME);
        Map<String, String> currentValues = new HashMap<>();

        if (Files.exists(envPath)) {
            for (String line : Files.readAllLines(envPath, StandardCharsets.UTF_8)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                    int eqIndex = trimmed.indexOf('=');
                    String key = trimmed.substring(0, eqIndex).trim();
                    String val = trimmed.substring(eqIndex + 1).trim();
                    currentValues.put(key, val);
                }
            }
        }

        currentValues.put("SPOTIPY_CLIENT_ID", spotifyClientId);
        currentValues.put("SPOTIPY_CLIENT_SECRET", spotifyClientSecret);
        currentValues.put("SPOTIPY_REDIRECT_URI", spotifyRedirectUri);
        currentValues.put("SLSKD_HOST", slskdHost);
        currentValues.put("SLSKD_PORT", String.valueOf(slskdPort));
        currentValues.put("SLSKD_API_KEY", slskdApiKey);
        currentValues.put("SLSKD_EXE_PATH", slskdExePath);
        currentValues.put("LOCAL_LIBRARY_PATH", localLibraryPath);

        StringBuilder sb = new StringBuilder();
        sb.append("# The SoundVault Environment Configuration\n");
        for (Map.Entry<String, String> entry : currentValues.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
        }

        Files.writeString(envPath, sb.toString(), StandardCharsets.UTF_8);
        AppLogger.log("Ficheiro .env gravado com sucesso.");
    }

    private void createDefaultEnvFile(Path envPath) {
        try {
            String template = """
                SPOTIPY_CLIENT_ID=
                SPOTIPY_CLIENT_SECRET=
                SPOTIPY_REDIRECT_URI=http://127.0.0.1:8080/callback
                SLSKD_HOST=http://localhost
                SLSKD_PORT=5030
                SLSKD_API_KEY=
                SLSKD_EXE_PATH=
                LOCAL_LIBRARY_PATH=%s
                """.formatted(getDefaultMusicPath().replace('\\', '/'));

            Files.writeString(envPath, template, StandardCharsets.UTF_8);
            AppLogger.log("Template .env criado automaticamente.");
        } catch (IOException e) {
            AppLogger.log("Erro ao criar .env inicial: " + e.getMessage());
        }
    }

    // Getters and Setters
    public String getSpotifyClientId() { return spotifyClientId; }
    public void setSpotifyClientId(String spotifyClientId) { this.spotifyClientId = spotifyClientId; }

    public String getSpotifyClientSecret() { return spotifyClientSecret; }
    public void setSpotifyClientSecret(String spotifyClientSecret) { this.spotifyClientSecret = spotifyClientSecret; }

    public String getSpotifyRedirectUri() { return spotifyRedirectUri; }
    public void setSpotifyRedirectUri(String spotifyRedirectUri) { this.spotifyRedirectUri = spotifyRedirectUri; }

    public String getSlskdHost() { return slskdHost; }
    public void setSlskdHost(String slskdHost) { this.slskdHost = slskdHost; }

    public int getSlskdPort() { return slskdPort; }
    public void setSlskdPort(int slskdPort) { this.slskdPort = slskdPort; }

    public String getSlskdApiKey() { return slskdApiKey; }
    public void setSlskdApiKey(String slskdApiKey) { this.slskdApiKey = slskdApiKey; }

    public String getSlskdExePath() { return slskdExePath; }
    public void setSlskdExePath(String slskdExePath) { this.slskdExePath = slskdExePath; }

    public String getLocalLibraryPath() { return localLibraryPath; }
    public void setLocalLibraryPath(String localLibraryPath) { this.localLibraryPath = localLibraryPath; }
}
