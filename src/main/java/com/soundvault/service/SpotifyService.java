package com.soundvault.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundvault.config.AppConfig;
import com.soundvault.model.Album;
import com.soundvault.model.Artist;
import com.soundvault.model.AudioAnalysis;
import com.soundvault.model.Track;
import com.soundvault.util.AppLogger;
import com.soundvault.util.StringNormalizer;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpServer;

public class SpotifyService {

    private static final String API_BASE = "https://api.spotify.com/v1";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String AUTH_URL = "https://accounts.spotify.com/authorize";
    private static final String SCOPE = "user-top-read playlist-read-private playlist-read-collaborative user-library-read";

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Path tokenCachePath = Paths.get(".cache");

    private String accessToken = "";
    private String refreshToken = "";
    private long expiresAtEpochSec = 0;
    private String currentUserId = "";

    public String getAccessToken() {
        return accessToken;
    }

    public String getCurrentUserId() {
        if ((currentUserId == null || currentUserId.isBlank()) && accessToken != null && !accessToken.isBlank()) {
            verifyToken();
        }
        return currentUserId;
    }

    public SpotifyService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.mapper = new ObjectMapper();
    }

    public synchronized boolean authenticate() {
        loadCachedToken();

        if (accessToken != null && !accessToken.isBlank() && System.currentTimeMillis() / 1000 < expiresAtEpochSec - 60) {
            if (verifyToken()) {
                AppLogger.log("Sessão do Spotify restaurada com sucesso para o utilizador: " + currentUserId);
                return true;
            }
        }

        if (refreshToken != null && !refreshToken.isBlank()) {
            if (refreshAccessToken()) {
                if (verifyToken()) {
                    AppLogger.log("Token do Spotify renovado via Refresh Token para: " + currentUserId);
                    return true;
                }
            }
        }

        AppLogger.log("A iniciar novo fluxo de autorização web do Spotify...");
        return startBrowserAuthFlow();
    }

    private boolean verifyToken() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(API_BASE + "/me"))
                    .header("Authorization", "Bearer " + accessToken)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resp.body());
                this.currentUserId = root.path("id").asText("");
                return true;
            }
        } catch (Exception ignored) {}
        return false;
    }

    private boolean refreshAccessToken() {
        AppConfig config = AppConfig.getInstance();
        String clientId = config.getSpotifyClientId();
        String clientSecret = config.getSpotifyClientSecret();

        if (clientId.isBlank() || clientSecret.isBlank() || refreshToken.isBlank()) {
            return false;
        }

        try {
            String form = "grant_type=refresh_token&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
            String basicAuth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + basicAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode node = mapper.readTree(resp.body());
                this.accessToken = node.path("access_token").asText();
                int expiresIn = node.path("expires_in").asInt(3600);
                this.expiresAtEpochSec = (System.currentTimeMillis() / 1000) + expiresIn;
                if (node.has("refresh_token")) {
                    this.refreshToken = node.path("refresh_token").asText();
                }
                saveCachedToken();
                return true;
            }
        } catch (Exception e) {
            AppLogger.log("Erro ao renovar token Spotify: " + e.getMessage());
        }
        return false;
    }

    private boolean startBrowserAuthFlow() {
        AppConfig config = AppConfig.getInstance();
        String clientId = config.getSpotifyClientId();
        String clientSecret = config.getSpotifyClientSecret();
        String redirectUri = config.getSpotifyRedirectUri();

        if (clientId.isBlank() || clientSecret.isBlank()) {
            AppLogger.log("AVISO: Client ID e Client Secret do Spotify não configurados no .env!");
            return false;
        }

        try {
            int port = 8080;
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
            CountDownLatch latch = new CountDownLatch(1);
            final String[] authCode = new String[1];

            server.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                if (query != null && query.contains("code=")) {
                    for (String param : query.split("&")) {
                        if (param.startsWith("code=")) {
                            authCode[0] = param.substring("code=".length());
                            break;
                        }
                    }
                }

                String response = "<html><body style='font-family:sans-serif;text-align:center;padding-top:50px;background:#0d0d0d;color:#fff;'>"
                        + "<h1 style='color:#1DB954;'>SoundVault Autenticado!</h1>"
                        + "<p>Podes fechar este separador do navegador e voltar a aplicacao.</p>"
                        + "</body></html>";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
                latch.countDown();
            });

            server.start();

            String authLink = AUTH_URL + "?client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8)
                    + "&response_type=code"
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                    + "&scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8);

            AppLogger.log("A abrir navegador para autenticação Spotify...");
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI.create(authLink));
            } else {
                AppLogger.log("Abre manualmente o link: " + authLink);
            }

            boolean ok = latch.await(120, TimeUnit.SECONDS);
            server.stop(0);

            if (ok && authCode[0] != null) {
                return exchangeCodeForToken(authCode[0], clientId, clientSecret, redirectUri);
            }
        } catch (Exception e) {
            AppLogger.log("Erro no servidor local de login: " + e.getMessage());
        }
        return false;
    }

    private boolean exchangeCodeForToken(String code, String clientId, String clientSecret, String redirectUri) {
        try {
            String form = "grant_type=authorization_code"
                    + "&code=" + URLEncoder.encode(code, StandardCharsets.UTF_8)
                    + "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

            String basicAuth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(TOKEN_URL))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .header("Authorization", "Basic " + basicAuth)
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode node = mapper.readTree(resp.body());
                this.accessToken = node.path("access_token").asText();
                this.refreshToken = node.path("refresh_token").asText();
                int expiresIn = node.path("expires_in").asInt(3600);
                this.expiresAtEpochSec = (System.currentTimeMillis() / 1000) + expiresIn;
                saveCachedToken();
                verifyToken();
                AppLogger.log("Autenticação Spotify concluída com sucesso para o utilizador: " + currentUserId);
                return true;
            }
        } catch (Exception e) {
            AppLogger.log("Falha ao trocar código de autorização por token: " + e.getMessage());
        }
        return false;
    }

    private HttpResponse<String> executeWithBackoff(HttpRequest.Builder requestBuilder) throws IOException, InterruptedException {
        int maxRetries = 3;
        for (int attempt = 0; attempt < maxRetries; attempt++) {
            HttpRequest req = requestBuilder
                    .header("Authorization", "Bearer " + accessToken)
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());

            if (resp.statusCode() == 401) {
                if (refreshAccessToken()) {
                    continue;
                }
            } else if (resp.statusCode() == 429) {
                String retryAfterHeader = resp.headers().firstValue("Retry-After").orElse("2");
                int retrySec = 2;
                try {
                    retrySec = Integer.parseInt(retryAfterHeader);
                } catch (NumberFormatException ignored) {}
                AppLogger.log("Spotify Rate Limit (429). A aguardar " + retrySec + "s...");
                Thread.sleep(retrySec * 1000L);
                continue;
            }
            return resp;
        }
        return null;
    }

    public void loadLikedSongsProgressively(Consumer<List<Track>> batchConsumer) {
        AppLogger.log("A extrair Liked Songs do Spotify em streaming progressivo...");
        int offset = 0;
        int totalLoaded = 0;

        while (true) {
            String url = API_BASE + "/me/tracks?limit=50&offset=" + offset;
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
                HttpResponse<String> resp = executeWithBackoff(builder);
                if (resp == null || resp.statusCode() != 200) break;

                JsonNode root = mapper.readTree(resp.body());
                JsonNode items = root.path("items");
                if (!items.isArray() || items.isEmpty()) break;

                List<Track> batch = new ArrayList<>();
                for (JsonNode item : items) {
                    JsonNode trackNode = item.path("track");
                    Track track = parseTrack(trackNode);
                    if (track != null) batch.add(track);
                }

                totalLoaded += batch.size();
                batchConsumer.accept(batch);

                String next = root.path("next").asText(null);
                if (next == null || next.isBlank()) break;

                offset += 50;
            } catch (Exception e) {
                AppLogger.log("Erro ao obter Liked Songs: " + e.getMessage());
                break;
            }
        }
        AppLogger.log("Concluído: " + totalLoaded + " Liked Songs carregadas.");
    }

    public List<Map<String, String>> getUserPlaylists() {
        List<Map<String, String>> result = new ArrayList<>();
        String myUserId = getCurrentUserId();
        int offset = 0;

        while (true) {
            try {
                String url = API_BASE + "/me/playlists?limit=50&offset=" + offset;
                HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
                HttpResponse<String> resp = executeWithBackoff(builder);
                if (resp == null || resp.statusCode() != 200) break;

                JsonNode root = mapper.readTree(resp.body());
                JsonNode items = root.path("items");
                if (!items.isArray() || items.isEmpty()) break;

                for (JsonNode item : items) {
                    String ownerId = item.path("owner").path("id").asText("");
                    // Filtrar apenas as playlists cujo dono seja o utilizador atual (Spotify bloqueia faixas de terceiros)
                    if (!myUserId.isBlank() && !ownerId.equals(myUserId)) {
                        continue;
                    }
                    String id = item.path("id").asText("");
                    String name = item.path("name").asText("");
                    if (!id.isBlank() && !name.isBlank()) {
                        Map<String, String> pl = new HashMap<>();
                        pl.put("id", id);
                        pl.put("name", name);
                        result.add(pl);
                    }
                }

                if (root.path("next").isNull() || root.path("next").asText("").isBlank()) {
                    break;
                }
                offset += 50;
            } catch (Exception e) {
                AppLogger.log("Erro ao carregar playlists: " + e.getMessage());
                break;
            }
        }
        return result;
    }

    public List<Track> getPlaylistTracks(String playlistId) {
        List<Track> tracks = new ArrayList<>();
        int offset = 0;
        while (true) {
            // New Spotify API uses /playlists/{id}/items, older uses /playlists/{id}/tracks
            String url = API_BASE + "/playlists/" + playlistId + "/items?limit=100&offset=" + offset;
            try {
                HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
                HttpResponse<String> resp = executeWithBackoff(builder);
                if (resp == null || (resp.statusCode() != 200 && resp.statusCode() != 403 && resp.statusCode() != 404)) {
                    AppLogger.log("Falha ao obter faixas da playlist " + playlistId + (resp != null ? (" Status: " + resp.statusCode() + " Body: " + resp.body()) : ""));
                    break;
                }

                // Fallback to /tracks if /items failed with 404 or 403
                if (resp.statusCode() != 200) {
                    AppLogger.log("/items status " + resp.statusCode() + " Body: " + resp.body() + ". Tentando fallback /tracks...");
                    String fallbackUrl = API_BASE + "/playlists/" + playlistId + "/tracks?limit=100&offset=" + offset;
                    builder = HttpRequest.newBuilder().uri(URI.create(fallbackUrl)).GET();
                    resp = executeWithBackoff(builder);
                    if (resp == null || resp.statusCode() != 200) {
                        AppLogger.log("Erro ao aceder playlist " + playlistId + " Status: " + (resp != null ? resp.statusCode() : "null") + " Body: " + (resp != null ? resp.body() : ""));
                        break;
                    }
                }

                JsonNode root = mapper.readTree(resp.body());
                JsonNode items = root.path("items");
                if (!items.isArray() || items.isEmpty()) break;

                for (JsonNode item : items) {
                    JsonNode trackNode = item.has("item") ? item.path("item") : item.path("track");
                    Track t = parseTrack(trackNode);
                    if (t != null) tracks.add(t);
                }

                if (root.path("next").isNull() || root.path("next").asText("").isBlank()) {
                    break;
                }
                offset += 100;
            } catch (Exception e) {
                AppLogger.log("Erro ao carregar faixas da playlist: " + e.getMessage());
                break;
            }
        }
        return tracks;
    }

    public List<Artist> getTopArtists(String timeRange, int limit) {
        List<Artist> artists = new ArrayList<>();
        try {
            String url = API_BASE + "/me/top/artists?time_range=" + timeRange + "&limit=" + limit;
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
            HttpResponse<String> resp = executeWithBackoff(builder);
            if (resp != null && resp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resp.body());
                for (JsonNode item : root.path("items")) {
                    String id = item.path("id").asText();
                    String name = item.path("name").asText();
                    String imgUrl = "";
                    JsonNode images = item.path("images");
                    if (images.isArray() && !images.isEmpty()) {
                        imgUrl = images.get(images.size() - 1).path("url").asText();
                    }
                    artists.add(new Artist(id, name, imgUrl, 0));
                }
            }
        } catch (Exception e) {
            AppLogger.log("Erro ao obter Top Artistas: " + e.getMessage());
        }
        return artists;
    }

    public List<String> getUserTopGenres() {
        Map<String, Integer> genreCounts = new LinkedHashMap<>();
        try {
            String url = API_BASE + "/me/top/artists?time_range=medium_term&limit=30";
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
            HttpResponse<String> resp = executeWithBackoff(builder);
            if (resp != null && resp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resp.body());
                for (JsonNode item : root.path("items")) {
                    JsonNode genresNode = item.path("genres");
                    if (genresNode.isArray()) {
                        for (JsonNode g : genresNode) {
                            String genre = g.asText().trim().toLowerCase();
                            if (!genre.isBlank()) {
                                genreCounts.put(genre, genreCounts.getOrDefault(genre, 0) + 1);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.log("Erro ao obter géneros do utilizador: " + e.getMessage());
        }

        if (genreCounts.isEmpty()) {
            return List.of("Rock", "Alternative", "Indie", "Electronic", "Ambient");
        }

        return genreCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(8)
                .map(e -> capitalizeWords(e.getKey()))
                .toList();
    }

    private String capitalizeWords(String str) {
        if (str == null || str.isBlank()) return "";
        String[] words = str.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                  .append(w.substring(1).toLowerCase())
                  .append(" ");
            }
        }
        return sb.toString().trim();
    }

    public AudioAnalysis getAverageAudioFeatures(List<String> trackIds) {
        if (trackIds != null && !trackIds.isEmpty()) {
            try {
                int take = Math.min(trackIds.size(), 100);
                String idsParam = String.join(",", trackIds.subList(0, take));
                String url = API_BASE + "/audio-features?ids=" + idsParam;

                HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
                HttpResponse<String> resp = executeWithBackoff(builder);
                if (resp != null && resp.statusCode() == 200) {
                    JsonNode root = mapper.readTree(resp.body());
                    JsonNode items = root.path("audio_features");

                    double sumEnergy = 0, sumValence = 0, sumDance = 0, sumAcoustic = 0, sumInstrumental = 0;
                    int validCount = 0;

                    for (JsonNode f : items) {
                        if (f != null && !f.isNull()) {
                            sumEnergy += f.path("energy").asDouble(0);
                            sumValence += f.path("valence").asDouble(0);
                            sumDance += f.path("danceability").asDouble(0);
                            sumAcoustic += f.path("acousticness").asDouble(0);
                            sumInstrumental += f.path("instrumentalness").asDouble(0);
                            validCount++;
                        }
                    }

                    if (validCount > 0) {
                        return new AudioAnalysis(
                                sumEnergy / validCount,
                                sumValence / validCount,
                                sumDance / validCount,
                                sumAcoustic / validCount,
                                sumInstrumental / validCount
                        );
                    }
                }
            } catch (Exception ignored) {}
        }

        // Fallback: A rota /audio-features foi descontinuada/bloqueada pelo Spotify (HTTP 403).
        // Calculamos um perfil psicoacústico baseado nos géneros reais do perfil do utilizador.
        AppLogger.log("A calcular perfil psicoacústico através de géneros musicais...");
        return computePsychoacousticFeatures();
    }

    private AudioAnalysis computePsychoacousticFeatures() {
        List<String> topGenres = getUserTopGenres();

        double energy = 0.65;
        double valence = 0.52;
        double dance = 0.58;
        double acoustic = 0.25;
        double instrumental = 0.30;

        if (!topGenres.isEmpty()) {
            double totalWeight = 0;
            double eSum = 0, vSum = 0, dSum = 0, aSum = 0, iSum = 0;

            for (String g : topGenres) {
                String low = g.toLowerCase();
                double ge = 0.60, gv = 0.50, gd = 0.55, ga = 0.30, gi = 0.25;

                if (low.contains("metal") || low.contains("punk") || low.contains("grunge") || low.contains("hardcore")) {
                    ge = 0.88; gv = 0.45; gd = 0.40; ga = 0.05; gi = 0.35;
                } else if (low.contains("rock") || low.contains("indie")) {
                    ge = 0.74; gv = 0.52; gd = 0.50; ga = 0.22; gi = 0.32;
                } else if (low.contains("electronic") || low.contains("techno") || low.contains("house") || low.contains("dance") || low.contains("edm")) {
                    ge = 0.85; gv = 0.68; gd = 0.82; ga = 0.06; gi = 0.65;
                } else if (low.contains("hip hop") || low.contains("rap") || low.contains("trap")) {
                    ge = 0.75; gv = 0.58; gd = 0.80; ga = 0.12; gi = 0.08;
                } else if (low.contains("ambient") || low.contains("drone") || low.contains("lo-fi") || low.contains("chill")) {
                    ge = 0.28; gv = 0.35; gd = 0.28; ga = 0.65; gi = 0.82;
                } else if (low.contains("classical") || low.contains("soundtrack") || low.contains("orchestral")) {
                    ge = 0.32; gv = 0.30; gd = 0.20; ga = 0.84; gi = 0.86;
                } else if (low.contains("folk") || low.contains("acoustic") || low.contains("country")) {
                    ge = 0.45; gv = 0.50; gd = 0.48; ga = 0.78; gi = 0.15;
                } else if (low.contains("pop") || low.contains("disco") || low.contains("synth")) {
                    ge = 0.72; gv = 0.70; gd = 0.76; ga = 0.20; gi = 0.10;
                } else if (low.contains("jazz") || low.contains("blues") || low.contains("soul")) {
                    ge = 0.55; gv = 0.62; gd = 0.62; ga = 0.52; gi = 0.38;
                }

                eSum += ge; vSum += gv; dSum += gd; aSum += ga; iSum += gi;
                totalWeight += 1.0;
            }

            if (totalWeight > 0) {
                energy = eSum / totalWeight;
                valence = vSum / totalWeight;
                dance = dSum / totalWeight;
                acoustic = aSum / totalWeight;
                instrumental = iSum / totalWeight;
            }
        }

        return new AudioAnalysis(
                Math.round(energy * 100.0) / 100.0,
                Math.round(valence * 100.0) / 100.0,
                Math.round(dance * 100.0) / 100.0,
                Math.round(acoustic * 100.0) / 100.0,
                Math.round(instrumental * 100.0) / 100.0
        );
    }

    public Optional<String> searchAlbumCoverUrl(String albumName) {
        try {
            String cleanQuery = StringNormalizer.cleanAlbumTitle(albumName);
            if (cleanQuery.isBlank()) cleanQuery = albumName;

            String url = API_BASE + "/search?q=" + URLEncoder.encode(cleanQuery, StandardCharsets.UTF_8) + "&type=album&limit=1";
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
            HttpResponse<String> resp = executeWithBackoff(builder);
            if (resp != null && resp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resp.body());
                JsonNode items = root.path("albums").path("items");
                if (items.isArray() && !items.isEmpty()) {
                    JsonNode images = items.get(0).path("images");
                    if (images.isArray() && !images.isEmpty()) {
                        return Optional.of(images.get(0).path("url").asText());
                    }
                }
            }
        } catch (Exception e) {
            AppLogger.log("Erro na pesquisa de capa Spotify: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Track parseTrack(JsonNode trackNode) {
        if (trackNode == null || trackNode.isNull()) return null;
        String id = trackNode.path("id").asText("");
        String title = trackNode.path("name").asText("Unknown Title");

        String artistName = "Unknown Artist";
        JsonNode artists = trackNode.path("artists");
        if (artists.isArray() && !artists.isEmpty()) {
            artistName = artists.get(0).path("name").asText("Unknown Artist");
        }

        JsonNode albumNode = trackNode.path("album");
        String albumName = albumNode.path("name").asText("Unknown Album");
        long durationMs = trackNode.path("duration_ms").asLong(0);

        String imageUrl = "";
        JsonNode images = albumNode.path("images");
        if (images.isArray() && !images.isEmpty()) {
            imageUrl = images.get(images.size() - 1).path("url").asText();
        }

        return new Track(id, title, artistName, albumName, durationMs, imageUrl);
    }

    public Optional<Album> searchAlbum(String artist, String albumTitle) {
        if ((artist == null || artist.isBlank()) && (albumTitle == null || albumTitle.isBlank())) {
            return Optional.empty();
        }

        try {
            // Estratégia 1: Pesquisa com filtros seletivos
            String cleanArtist = (artist != null) ? artist.trim() : "";
            String cleanAlbum = (albumTitle != null) ? albumTitle.trim() : "";

            // Se for compilação de vários artistas, pesquisa só pelo título do álbum
            String query;
            if (cleanArtist.equalsIgnoreCase("Vários") || cleanArtist.equalsIgnoreCase("Various Artists") || cleanArtist.isEmpty()) {
                query = cleanAlbum;
            } else {
                query = "album:" + cleanAlbum + " artist:" + cleanArtist;
            }

            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = API_BASE + "/search?q=" + encoded + "&type=album&limit=5";

            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
            HttpResponse<String> resp = executeWithBackoff(builder);

            if (resp != null && resp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resp.body());
                JsonNode items = root.path("albums").path("items");
                if (items.isArray() && !items.isEmpty()) {
                    return Optional.of(parseAlbumJson(items.get(0)));
                }
            }

            // Estratégia 2 (Fallback): Pesquisa simples de texto livre
            String fallbackQuery = (!cleanArtist.equalsIgnoreCase("Vários") && !cleanArtist.isEmpty())
                    ? cleanArtist + " " + cleanAlbum
                    : cleanAlbum;

            String fbEncoded = URLEncoder.encode(fallbackQuery, StandardCharsets.UTF_8);
            String fbUrl = API_BASE + "/search?q=" + fbEncoded + "&type=album&limit=5";

            builder = HttpRequest.newBuilder().uri(URI.create(fbUrl)).GET();
            resp = executeWithBackoff(builder);

            if (resp != null && resp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resp.body());
                JsonNode items = root.path("albums").path("items");
                if (items.isArray() && !items.isEmpty()) {
                    return Optional.of(parseAlbumJson(items.get(0)));
                }
            }
        } catch (Exception e) {
            AppLogger.log("Erro ao pesquisar álbum no Spotify (" + artist + " - " + albumTitle + "): " + e.getMessage());
        }
        return Optional.empty();
    }

    public List<String> getAlbumTrackNamesById(String albumId) {
        if (albumId == null || albumId.isBlank()) return Collections.emptyList();
        try {
            String url = API_BASE + "/albums/" + albumId + "/tracks?limit=50";
            HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(url)).GET();
            HttpResponse<String> resp = executeWithBackoff(builder);
            if (resp != null && resp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resp.body());
                JsonNode items = root.path("items");
                if (items.isArray()) {
                    List<String> tracks = new ArrayList<>();
                    for (JsonNode t : items) {
                        String name = t.path("name").asText("");
                        if (!name.isBlank()) tracks.add(name);
                    }
                    return tracks;
                }
            }
        } catch (Exception e) {
            AppLogger.log("Erro ao obter faixas do álbum no Spotify (ID " + albumId + "): " + e.getMessage());
        }
        return Collections.emptyList();
    }

    public List<String> getAlbumTrackNames(String artist, String albumTitle) {
        Optional<Album> albumOpt = searchAlbum(artist, albumTitle);
        if (albumOpt.isPresent() && !albumOpt.get().id().isBlank()) {
            return getAlbumTrackNamesById(albumOpt.get().id());
        }
        return Collections.emptyList();
    }

    private Album parseAlbumJson(JsonNode node) {
        String id = node.path("id").asText("");
        String name = node.path("name").asText("");
        String artistName = "Unknown Artist";
        JsonNode artists = node.path("artists");
        if (artists.isArray() && !artists.isEmpty()) {
            artistName = artists.get(0).path("name").asText("Unknown Artist");
        }
        int totalTracks = node.path("total_tracks").asInt(0);
        String releaseDate = node.path("release_date").asText("");
        String imageUrl = "";
        JsonNode images = node.path("images");
        if (images.isArray() && !images.isEmpty()) {
            if (images.size() > 1) {
                imageUrl = images.get(1).path("url").asText("");
            } else {
                imageUrl = images.get(0).path("url").asText("");
            }
        }
        return new Album(id, name, artistName, totalTracks, imageUrl, releaseDate);
    }

    private void saveCachedToken() {
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("access_token", accessToken);
            map.put("refresh_token", refreshToken);
            map.put("expires_at", expiresAtEpochSec);
            map.put("token_type", "Bearer");
            map.put("scope", SCOPE);
            mapper.writeValue(tokenCachePath.toFile(), map);
        } catch (Exception ignored) {}
    }

    private void loadCachedToken() {
        if (!Files.exists(tokenCachePath)) return;
        try {
            JsonNode node = mapper.readTree(tokenCachePath.toFile());
            this.accessToken = node.path("access_token").asText("");
            this.refreshToken = node.path("refresh_token").asText("");
            this.expiresAtEpochSec = node.path("expires_at").asLong(0);
        } catch (Exception ignored) {}
    }
}
