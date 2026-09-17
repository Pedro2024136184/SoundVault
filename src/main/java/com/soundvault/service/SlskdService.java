package com.soundvault.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundvault.config.AppConfig;
import com.soundvault.model.DownloadTransfer;
import com.soundvault.model.PeerResult;
import com.soundvault.model.SearchHistoryItem;
import com.soundvault.model.SoulseekFile;
import com.soundvault.util.AppLogger;
import com.soundvault.util.StringNormalizer;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

public class SlskdService {

    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Set<String> notifiedCompletedTransfers = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final List<java.util.function.Consumer<List<DownloadTransfer>>> transferListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    public SlskdService() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper();
        startTransferMonitor();
    }

    public void addTransferListener(java.util.function.Consumer<List<DownloadTransfer>> listener) {
        if (listener != null) {
            transferListeners.add(listener);
        }
    }

    public void removeTransferListener(java.util.function.Consumer<List<DownloadTransfer>> listener) {
        transferListeners.remove(listener);
    }

    private void startTransferMonitor() {
        Thread.ofVirtual().start(() -> {
            while (true) {
                try {
                    List<DownloadTransfer> transfers = getAllTransfers();
                    boolean hasActive = false;

                    for (DownloadTransfer t : transfers) {
                        if (t.isCompleted()) {
                            String key = t.username() + ":" + t.filename();
                            if (notifiedCompletedTransfers.add(key)) {
                                NotificationService.getInstance().notifyDownloadCompleted(t.getCleanFileName());
                            }
                        } else if (t.isInProgress() || t.isQueued()) {
                            hasActive = true;
                        }
                    }

                    // Notifica ouvintes de UI com as transferências atuais
                    for (java.util.function.Consumer<List<DownloadTransfer>> listener : transferListeners) {
                        try {
                            listener.accept(transfers);
                        } catch (Exception ignored) {}
                    }

                    // Se houver downloads em curso, atualiza a cada 1.8s para manter a barra de progresso suave;
                    // se estiver inativo, aguarda 6s para economizar recursos.
                    long sleepMs = hasActive ? 1800 : 6000;
                    Thread.sleep(sleepMs);

                } catch (InterruptedException e) {
                    break;
                } catch (Exception ignored) {}
            }
        });
    }

    private String getBaseUrl() {
        AppConfig config = AppConfig.getInstance();
        return config.getSlskdHost() + ":" + config.getSlskdPort() + "/api/v0";
    }

    private String getApiKey() {
        return AppConfig.getInstance().getSlskdApiKey();
    }

    public String initiateSearch(String query) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            AppLogger.log("AVISO: A SLSKD_API_KEY está vazia no ficheiro .env!");
            return null;
        }

        String searchId = UUID.randomUUID().toString();
        String payload = String.format("{\"id\":\"%s\",\"searchText\":\"%s\"}", searchId, query.replace("\"", "\\\""));

        try {
            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/searches"))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> postRes = httpClient.send(postReq, HttpResponse.BodyHandlers.ofString());
            if (postRes.statusCode() < 400) {
                AppLogger.log("Pesquisa Soulseek iniciada: '" + query + "' [ID: " + searchId + "]");
                return searchId;
            } else {
                AppLogger.log("Falha ao iniciar pesquisa Soulseek: Status " + postRes.statusCode() + " " + postRes.body());
            }
        } catch (Exception e) {
            AppLogger.log("Erro ao contactar slskd: " + e.getMessage());
        }
        return null;
    }

    public List<JsonNode> search(String query, int timeoutSeconds, int expectedTracks) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            AppLogger.log("AVISO: A SLSKD_API_KEY está vazia no ficheiro .env!");
            return Collections.emptyList();
        }

        String searchId = UUID.randomUUID().toString();
        String payload = String.format("{\"id\":\"%s\",\"searchText\":\"%s\"}", searchId, query.replace("\"", "\\\""));

        try {
            HttpRequest postReq = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/searches"))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .timeout(Duration.ofSeconds(5))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> postRes = httpClient.send(postReq, HttpResponse.BodyHandlers.ofString());
            if (postRes.statusCode() == 401) {
                AppLogger.log("Erro 401: A chave API no .env foi rejeitada pelo slskd.");
                return Collections.emptyList();
            } else if (postRes.statusCode() >= 400) {
                AppLogger.log("Erro na pesquisa Soulseek: Status " + postRes.statusCode() + " " + postRes.body());
                return Collections.emptyList();
            }

            AppLogger.log("Pesquisa Soulseek iniciada: '" + query + "'. A monitorizar resultados...");
            long startTime = System.currentTimeMillis();
            long maxWaitMs = timeoutSeconds * 1000L;

            while (System.currentTimeMillis() - startTime < maxWaitMs) {
                Thread.sleep(2500);

                // 1. Verifica respostas para ver se já temos um peer excelente (curto-circuito)
                List<JsonNode> currentResponses = fetchResponses(searchId);
                if (!currentResponses.isEmpty()) {
                    PeerResult candidate = selectBestPeer(currentResponses, expectedTracks);
                    if (candidate != null && candidate.uploadSpeed() > 100_000 && candidate.hasFreeSlots()) {
                        AppLogger.log("Par excelente encontrado nos primeiros segundos! A avançar sem esperar 90s.");
                        return currentResponses;
                    }
                }

                // 2. Verifica estado da pesquisa
                HttpRequest stateReq = HttpRequest.newBuilder()
                        .uri(URI.create(getBaseUrl() + "/searches/" + searchId))
                        .header("X-API-Key", apiKey)
                        .timeout(Duration.ofSeconds(4))
                        .GET()
                        .build();

                try {
                    HttpResponse<String> stateRes = httpClient.send(stateReq, HttpResponse.BodyHandlers.ofString());
                    if (stateRes.statusCode() == 200) {
                        JsonNode stateNode = mapper.readTree(stateRes.body());
                        String state = stateNode.path("state").asText("");
                        if ("Completed".equalsIgnoreCase(state)) {
                            long seconds = (System.currentTimeMillis() - startTime) / 1000;
                            AppLogger.log("Pesquisa concluída pelo slskd em " + seconds + "s.");
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }

            List<JsonNode> finalResponses = fetchResponses(searchId);
            AppLogger.log("Recebidos " + finalResponses.size() + " pares com resultados válidos do Soulseek.");
            return finalResponses;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Collections.emptyList();
        } catch (Exception e) {
            AppLogger.log("Erro ao comunicar com a API do slskd: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private List<JsonNode> fetchResponses(String searchId) {
        try {
            HttpRequest resReq = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/searches/" + searchId + "/responses"))
                    .header("X-API-Key", getApiKey())
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> resResp = httpClient.send(resReq, HttpResponse.BodyHandlers.ofString());
            if (resResp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resResp.body());
                List<JsonNode> list = new ArrayList<>();
                if (root.isArray()) {
                    for (JsonNode n : root) list.add(n);
                } else if (root.has("responses") && root.get("responses").isArray()) {
                    for (JsonNode n : root.get("responses")) list.add(n);
                }
                return list;
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    public PeerResult selectBestPeer(List<JsonNode> responses, int expectedTracks) {
        String bestUsername = null;
        String bestDir = null;
        List<SoulseekFile> bestFiles = new ArrayList<>();
        double bestSpeed = -1;
        boolean bestHasFreeSlots = false;

        for (JsonNode resp : responses) {
            String username = resp.path("username").asText("");
            if (username.isBlank()) continue;

            double speed = resp.path("uploadSpeed").asDouble(0);
            boolean hasFreeSlots = resp.path("hasFreeUploadSlots").asBoolean(false);
            double speedScore = hasFreeSlots ? speed : (speed / 10.0);

            JsonNode filesNode = resp.path("files");
            if (!filesNode.isArray()) continue;

            Map<String, List<SoulseekFile>> dirs = new HashMap<>();
            for (JsonNode f : filesNode) {
                String filename = f.path("filename").asText("");
                if (!filename.toLowerCase().endsWith(".flac")) continue;

                long size = f.path("size").asLong(0);
                int bitRate = f.path("bitRate").asInt(0);
                int length = f.path("length").asInt(0);

                String normSlash = filename.replace('/', '\\');
                int lastSlash = normSlash.lastIndexOf('\\');
                String dir = lastSlash >= 0 ? normSlash.substring(0, lastSlash) : "";

                dirs.computeIfAbsent(dir, k -> new ArrayList<>())
                        .add(new SoulseekFile(filename, size, bitRate, length));
            }

            for (Map.Entry<String, List<SoulseekFile>> entry : dirs.entrySet()) {
                List<SoulseekFile> dirFiles = entry.getValue();
                if (expectedTracks > 0 && dirFiles.size() < expectedTracks) {
                    continue;
                }

                if (speedScore > bestSpeed) {
                    bestSpeed = speedScore;
                    bestDir = entry.getKey();
                    bestFiles = dirFiles;
                    bestUsername = username;
                    bestHasFreeSlots = hasFreeSlots;
                } else if (Math.abs(speedScore - bestSpeed) < 0.001) {
                    if (expectedTracks > 0) {
                        int diffCurrent = Math.abs(dirFiles.size() - expectedTracks);
                        int diffBest = Math.abs(bestFiles.size() - expectedTracks);
                        if (diffCurrent < diffBest) {
                            bestDir = entry.getKey();
                            bestFiles = dirFiles;
                            bestUsername = username;
                            bestHasFreeSlots = hasFreeSlots;
                        }
                    } else if (dirFiles.size() > bestFiles.size()) {
                        bestDir = entry.getKey();
                        bestFiles = dirFiles;
                        bestUsername = username;
                        bestHasFreeSlots = hasFreeSlots;
                    }
                }
            }
        }

        if (bestUsername != null && !bestFiles.isEmpty()) {
            return new PeerResult(bestUsername, bestDir, bestFiles, bestSpeed, bestHasFreeSlots);
        }
        return null;
    }

    public boolean searchAndDownload(String query, int expectedTracks) {
        String cleanQuery = StringNormalizer.cleanSearchQuery(query);
        if (cleanQuery.isBlank()) cleanQuery = query;

        try {
            AppLogger.log("A iniciar pesquisa Soulseek para: '" + cleanQuery + "'");
            List<JsonNode> responses = search(cleanQuery, 45, expectedTracks);

            // Fallback 1: se não houver respostas e a query incluir "flac", pesquisa sem o termo "flac"
            // (muitos utilizadores não têm "flac" no caminho da pasta, mas têm faixas .flac que o selectBestPeer recolhe)
            if (responses.isEmpty() && cleanQuery.toLowerCase().endsWith(" flac")) {
                String fallbackQuery = cleanQuery.substring(0, cleanQuery.length() - 5).trim();
                AppLogger.log("Nenhum resultado direto com 'flac'. A tentar pesquisa alargada no Soulseek: '" + fallbackQuery + "'...");
                responses = search(fallbackQuery, 35, expectedTracks);
            }

            if (responses.isEmpty()) {
                AppLogger.log("Nenhuma resposta válida recebida no Soulseek para: " + cleanQuery);
                return false;
            }

            PeerResult best = selectBestPeer(responses, expectedTracks);
            if (best == null) {
                if (expectedTracks > 0) {
                    AppLogger.log("Nenhum álbum completo (" + expectedTracks + "+ faixas) encontrado para: " + cleanQuery);
                } else {
                    AppLogger.log("Nenhum ficheiro FLAC encontrado para: " + cleanQuery);
                }
                return false;
            }

            AppLogger.log("Melhor par selecionado: " + best.username() + " (" + best.files().size() + " faixas FLAC)");
            return downloadPeerFiles(best.username(), best.files());

        } catch (Exception e) {
            AppLogger.log("Erro ao processar download Soulseek: " + e.getMessage());
            return false;
        }
    }

    public record VerifiedAlbumFolder(
            String username,
            String folderPath,
            List<SoulseekFile> files,
            double uploadSpeed,
            boolean hasFreeSlots,
            int matchedTrackCount
    ) {}

    public boolean isFolderLegitimateAlbum(String artist, String albumTitle, String folderPath, List<SoulseekFile> flacFiles, List<String> knownAlbumTracks, int expectedTracks) {
        if (flacFiles == null || flacFiles.isEmpty()) return false;

        // 1. Verificação de extensão e quantidade mínima:
        int minAllowed = expectedTracks > 0 ? Math.min(3, expectedTracks) : 3;
        if (flacFiles.size() < minAllowed) {
            return false;
        }

        // 2. Verificação de Artista no Caminho ou Ficheiros:
        String normArtist = StringNormalizer.normalize(artist).toLowerCase();
        String normPath = StringNormalizer.normalize(folderPath).toLowerCase();
        boolean artistInPath = normPath.contains(normArtist);

        if (!artistInPath) {
            long filesWithArtist = flacFiles.stream()
                    .filter(f -> StringNormalizer.normalize(f.filename()).toLowerCase().contains(normArtist))
                    .count();
            if (filesWithArtist < 2) {
                return false; // Rejeitado: Artista não coincide
            }
        }

        // 3. Verificação Cruzada de Múltiplas Faixas do Álbum:
        String normAlbum = StringNormalizer.normalize(StringNormalizer.cleanAlbumTitle(albumTitle)).toLowerCase();
        boolean albumInPath = normPath.contains(normAlbum);

        if (knownAlbumTracks != null && !knownAlbumTracks.isEmpty()) {
            int matchedTracks = countMatchingAlbumTracks(flacFiles, knownAlbumTracks);
            if (matchedTracks >= 2) {
                return true;
            }
            if (matchedTracks == 1 && albumInPath) {
                return true;
            }
            return false; // Rejeitado: Apenas 1 faixa aleatória sem o nome do álbum no caminho
        }

        return albumInPath;
    }

    public int countMatchingAlbumTracks(List<SoulseekFile> flacFiles, List<String> knownAlbumTracks) {
        int count = 0;
        for (String track : knownAlbumTracks) {
            String normTrack = StringNormalizer.normalize(track).toLowerCase();
            if (normTrack.length() < 3) continue;
            boolean match = flacFiles.stream().anyMatch(f -> {
                String fName = StringNormalizer.normalize(f.getCleanFileName()).toLowerCase();
                return fName.contains(normTrack);
            });
            if (match) count++;
        }
        return count;
    }

    public VerifiedAlbumFolder findBestVerifiedFolder(List<JsonNode> responses, String artist, String albumTitle, List<String> albumTracks, int expectedTracks) {
        VerifiedAlbumFolder best = null;
        double bestScore = -1;

        for (JsonNode resp : responses) {
            String username = resp.path("username").asText("");
            if (username.isBlank()) continue;

            double speed = resp.path("uploadSpeed").asDouble(0);
            boolean hasFreeSlots = resp.path("hasFreeUploadSlots").asBoolean(false);

            JsonNode filesNode = resp.path("files");
            if (!filesNode.isArray()) continue;

            Map<String, List<SoulseekFile>> dirs = new HashMap<>();
            for (JsonNode f : filesNode) {
                String filename = f.path("filename").asText("");
                if (!filename.toLowerCase().endsWith(".flac")) continue;

                long size = f.path("size").asLong(0);
                int bitRate = f.path("bitRate").asInt(0);
                int length = f.path("length").asInt(0);

                String normSlash = filename.replace('/', '\\');
                int lastSlash = normSlash.lastIndexOf('\\');
                String dir = lastSlash >= 0 ? normSlash.substring(0, lastSlash) : "";

                dirs.computeIfAbsent(dir, k -> new ArrayList<>())
                        .add(new SoulseekFile(filename, size, bitRate, length));
            }

            for (Map.Entry<String, List<SoulseekFile>> entry : dirs.entrySet()) {
                String folderPath = entry.getKey();
                List<SoulseekFile> files = entry.getValue();

                if (!isFolderLegitimateAlbum(artist, albumTitle, folderPath, files, albumTracks, expectedTracks)) {
                    continue;
                }

                int matchedCount = (albumTracks != null && !albumTracks.isEmpty())
                        ? countMatchingAlbumTracks(files, albumTracks)
                        : files.size();

                double slotMultiplier = hasFreeSlots ? 1.0 : 0.2;
                double score = (matchedCount * 1_000_000.0) + (speed * slotMultiplier);

                if (score > bestScore) {
                    bestScore = score;
                    best = new VerifiedAlbumFolder(username, folderPath, files, speed, hasFreeSlots, matchedCount);
                }
            }
        }
        return best;
    }

    public boolean searchAndDownloadAlbumWithFallback(String artist, String albumTitle, List<String> knownTracks, SpotifyService spotifyService, int expectedTracks) {
        AppLogger.log("A iniciar pesquisa avançada para o álbum: '" + artist + " - " + albumTitle + "'");

        List<String> albumTracks = new ArrayList<>();
        if (knownTracks != null && !knownTracks.isEmpty()) {
            albumTracks.addAll(knownTracks);
        }
        if (albumTracks.size() < 3 && spotifyService != null) {
            try {
                List<String> spTracks = spotifyService.getAlbumTrackNames(artist, albumTitle);
                if (!spTracks.isEmpty()) {
                    albumTracks.clear();
                    albumTracks.addAll(spTracks);
                    AppLogger.log("Obtidas " + albumTracks.size() + " faixas oficiais do álbum via Spotify.");
                }
            } catch (Exception ignored) {}
        }

        boolean isSelfTitled = StringNormalizer.normalize(artist).equalsIgnoreCase(StringNormalizer.normalize(albumTitle));

        String query;
        if (isSelfTitled) {
            query = artist + " flac";
        } else {
            query = StringNormalizer.cleanSearchQuery(artist + " " + albumTitle) + " flac";
        }

        List<JsonNode> responses = search(query, 35, expectedTracks);

        if (!responses.isEmpty()) {
            VerifiedAlbumFolder best = findBestVerifiedFolder(responses, artist, albumTitle, albumTracks, expectedTracks);
            if (best != null) {
                AppLogger.log("SUCESSO: Álbum validado diretamente (" + best.matchedTrackCount() + " faixas do álbum). A descarregar de '" + best.username() + "'...");
                return downloadPeerFiles(best.username(), best.files());
            }
        }

        AppLogger.log("Pesquisa direta de álbum devolveu 0 pastas verificadas. A ativar Fallback por faixa...");
        String firstTrack = null;
        if (!albumTracks.isEmpty()) {
            firstTrack = albumTracks.get(0);
        }

        if (firstTrack == null || firstTrack.isBlank()) {
            AppLogger.log("Não foi possível determinar faixas do álbum para executar fallback.");
            return false;
        }

        String trackQuery = artist + " " + StringNormalizer.cleanSearchQuery(firstTrack);
        AppLogger.log("A executar pesquisa de Fallback Soulseek por faixa: '" + trackQuery + "'...");
        List<JsonNode> trackResponses = search(trackQuery, 35, expectedTracks);

        if (trackResponses.isEmpty() && trackQuery.toLowerCase().endsWith(" flac")) {
            trackResponses = search(trackQuery.substring(0, trackQuery.length() - 5).trim(), 25, expectedTracks);
        }

        if (trackResponses.isEmpty()) {
            AppLogger.log("Nenhum resultado de fallback encontrado para: " + trackQuery);
            return false;
        }

        VerifiedAlbumFolder bestFallback = findBestVerifiedFolder(trackResponses, artist, albumTitle, albumTracks, expectedTracks);
        if (bestFallback != null) {
            AppLogger.log("SUCESSO NO FALLBACK: Pasta verificada com " + bestFallback.matchedTrackCount() +
                    " faixas correspondentes ao álbum '" + albumTitle + "'. A descarregar " +
                    bestFallback.files().size() + " faixas FLAC de '" + bestFallback.username() + "'!");
            return downloadPeerFiles(bestFallback.username(), bestFallback.files());
        }

        AppLogger.log("AVISO: Foram encontrados ficheiros para '" + firstTrack + "', mas nenhuma pasta cumpriu as verificações estritas de segurança para o álbum '" + albumTitle + "'. Download abortado para evitar pasta errada.");
        return false;
    }

    public boolean downloadPeerFiles(String username, List<SoulseekFile> files) {
        String apiKey = getApiKey();
        if (apiKey == null || apiKey.isBlank() || username == null || files == null || files.isEmpty()) {
            return false;
        }

        try {
            List<Map<String, Object>> payload = new ArrayList<>();
            for (SoulseekFile f : files) {
                Map<String, Object> item = new HashMap<>();
                item.put("filename", f.filename());
                item.put("size", f.size());
                payload.add(item);
            }

            String jsonPayload = mapper.writeValueAsString(payload);
            AppLogger.log("A submeter pedido de download ao peer '" + username + "' (" + files.size() + " ficheiros)...");

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/transfers/downloads/" + username))
                    .header("Content-Type", "application/json")
                    .header("X-API-Key", apiKey)
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
                AppLogger.log("SUCESSO: Download de " + files.size() + " faixas iniciado de '" + username + "'.");
                return true;
            } else {
                AppLogger.log("O slskd rejeitou o download! Status: " + resp.statusCode() + " " + resp.body());
                return false;
            }
        } catch (Exception e) {
            AppLogger.log("Falha ao comunicar com slskd para download: " + e.getMessage());
            return false;
        }
    }

    public record RelicCandidate(
            String title,
            String folderPath,
            String username,
            double uploadSpeed,
            boolean hasFreeSlots,
            List<SoulseekFile> flacFiles,
            long totalBytes,
            String tagType
    ) {
        public String getFormattedSpeed() {
            if (uploadSpeed <= 0) return "N/A";
            if (uploadSpeed > 1_000_000) {
                return String.format("%.1f MB/s", uploadSpeed / 1_000_000.0);
            }
            return String.format("%.0f KB/s", uploadSpeed / 1000.0);
        }

        public String getFormattedSize() {
            if (totalBytes > 1_000_000_000) {
                return String.format("%.2f GB", totalBytes / 1_000_000_000.0);
            }
            return String.format("%.1f MB", totalBytes / 1_000_000.0);
        }
    }

    public List<RelicCandidate> searchRelics(String query, int timeoutSeconds, String tagType) {
        String cleanQuery = StringNormalizer.cleanSearchQuery(query);
        if (cleanQuery.isBlank()) cleanQuery = query;

        List<JsonNode> responses = search(cleanQuery, timeoutSeconds, 0);
        if (responses.isEmpty() && cleanQuery.toLowerCase().endsWith(" flac")) {
            String fallback = cleanQuery.substring(0, cleanQuery.length() - 5).trim();
            responses = search(fallback, Math.max(15, timeoutSeconds - 10), 0);
        }

        List<RelicCandidate> candidates = new ArrayList<>();
        Set<String> seenDirectories = new HashSet<>();

        for (JsonNode resp : responses) {
            String username = resp.path("username").asText("");
            if (username.isBlank()) continue;

            double speed = resp.path("uploadSpeed").asDouble(0);
            boolean hasFreeSlots = resp.path("hasFreeUploadSlots").asBoolean(false);

            JsonNode filesNode = resp.path("files");
            if (!filesNode.isArray()) continue;

            Map<String, List<SoulseekFile>> dirs = new HashMap<>();
            for (JsonNode f : filesNode) {
                String filename = f.path("filename").asText("");
                if (!filename.toLowerCase().endsWith(".flac")) continue;

                long size = f.path("size").asLong(0);
                int bitRate = f.path("bitRate").asInt(0);
                int length = f.path("length").asInt(0);

                String normSlash = filename.replace('/', '\\');
                int lastSlash = normSlash.lastIndexOf('\\');
                String dir = lastSlash >= 0 ? normSlash.substring(0, lastSlash) : "";

                dirs.computeIfAbsent(dir, k -> new ArrayList<>())
                        .add(new SoulseekFile(filename, size, bitRate, length));
            }

            for (Map.Entry<String, List<SoulseekFile>> entry : dirs.entrySet()) {
                String folderPath = entry.getKey();
                List<SoulseekFile> files = entry.getValue();
                if (files.isEmpty()) continue;

                String uniqueKey = username + "::" + folderPath;
                if (!seenDirectories.add(uniqueKey)) continue;

                int lastBackslash = folderPath.lastIndexOf('\\');
                String folderName = lastBackslash >= 0 ? folderPath.substring(lastBackslash + 1) : folderPath;
                if (folderName.isBlank()) folderName = query;

                long totalBytes = files.stream().mapToLong(SoulseekFile::size).sum();

                candidates.add(new RelicCandidate(
                        folderName,
                        folderPath,
                        username,
                        speed,
                        hasFreeSlots,
                        files,
                        totalBytes,
                        tagType
                ));
            }
        }

        candidates.sort((a, b) -> {
            if (a.hasFreeSlots() != b.hasFreeSlots()) {
                return a.hasFreeSlots() ? -1 : 1;
            }
            return Double.compare(b.uploadSpeed(), a.uploadSpeed());
        });

        return candidates;
    }

    public List<DownloadTransfer> getAllTransfers() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/transfers/downloads"))
                    .header("X-API-Key", getApiKey())
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resp.body());
                List<DownloadTransfer> transfers = new ArrayList<>();
                if (root.isArray()) {
                    for (JsonNode userNode : root) {
                        String user = userNode.path("username").asText("Unknown");
                        JsonNode dirs = userNode.path("directories");
                        if (dirs.isArray()) {
                            for (JsonNode dir : dirs) {
                                JsonNode files = dir.path("files");
                                if (files.isArray()) {
                                    for (JsonNode f : files) {
                                        String filename = f.path("filename").asText("");
                                        String state = f.path("state").asText("Unknown");
                                        long size = f.path("size").asLong(0);
                                        double progress = f.path("percentComplete").asDouble(0);
                                        double speed = f.path("averageSpeed").asDouble(0);
                                        long bytesTransferred = f.path("bytesTransferred").asLong(0);
                                        transfers.add(new DownloadTransfer(user, filename, state, size, progress, speed, bytesTransferred));
                                    }
                                }
                            }
                        }
                    }
                }
                return transfers;
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    public List<SearchHistoryItem> getRecentSearches() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/searches"))
                    .header("X-API-Key", getApiKey())
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() == 200) {
                JsonNode root = mapper.readTree(resp.body());
                List<SearchHistoryItem> items = new ArrayList<>();
                if (root.isArray()) {
                    for (JsonNode n : root) {
                        String id = n.path("id").asText("");
                        String text = n.path("searchText").asText("");
                        String state = n.path("state").asText("Unknown");
                        int count = n.path("responseCount").asInt(0);
                        items.add(new SearchHistoryItem(id, text, state, count));
                    }
                }
                return items;
            }
        } catch (Exception ignored) {}
        return Collections.emptyList();
    }

    public boolean testConnection() {
        try {
            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(getBaseUrl() + "/options"))
                    .header("X-API-Key", getApiKey())
                    .timeout(Duration.ofSeconds(3))
                    .GET()
                    .build();
            HttpResponse<String> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            return resp.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }
}
