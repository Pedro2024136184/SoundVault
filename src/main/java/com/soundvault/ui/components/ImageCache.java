package com.soundvault.ui.components;

import com.soundvault.util.AppLogger;
import javafx.application.Platform;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class ImageCache {

    private static final int MAX_RAM_CACHE = 300;
    private static final ImageCache INSTANCE = new ImageCache();

    private final Map<String, Image> ramCache = Collections.synchronizedMap(
            new LinkedHashMap<>(MAX_RAM_CACHE, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Image> eldest) {
                    return size() > MAX_RAM_CACHE;
                }
            }
    );

    private final ExecutorService imagePool = Executors.newVirtualThreadPerTaskExecutor();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public static ImageCache getInstance() {
        return INSTANCE;
    }

    public void loadImageAsync(String url, double requestedWidth, double requestedHeight, Consumer<Image> callback) {
        if (url == null || url.isBlank()) {
            callback.accept(null);
            return;
        }

        String cacheKey = url + "_" + (int) requestedWidth + "x" + (int) requestedHeight;

        // 1. RAM Cache hit
        Image fromRam = ramCache.get(cacheKey);
        if (fromRam != null) {
            callback.accept(fromRam);
            return;
        }

        // 2. Assíncrono via Disco / Internet
        CompletableFuture.supplyAsync(() -> {
            try {
                String hash = md5(url);
                Path diskPath = Paths.get("cache", "covers", hash + ".png");

                if (Files.exists(diskPath)) {
                    try (InputStream is = Files.newInputStream(diskPath)) {
                        return new Image(is, requestedWidth, requestedHeight, true, true);
                    }
                }

                // Descarregar da internet
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();

                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200) {
                    byte[] bytes = resp.body();
                    Files.createDirectories(diskPath.getParent());
                    Files.write(diskPath, bytes);

                    try (InputStream is = new ByteArrayInputStream(bytes)) {
                        return new Image(is, requestedWidth, requestedHeight, true, true);
                    }
                }
            } catch (Exception ignored) {}
            return null;
        }, imagePool).thenAccept(img -> {
            if (img != null) {
                ramCache.put(cacheKey, img);
                Platform.runLater(() -> callback.accept(img));
            }
        });
    }

    private String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(input.hashCode());
        }
    }
}
