package com.soundvault.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundvault.config.AppConfig;
import com.soundvault.util.AppLogger;
import com.soundvault.util.StringNormalizer;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class LibraryService {

    private final TaggingService taggingService = new TaggingService();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Path cacheFilePath = Paths.get("cache", "library_cache.json");

    private final Map<String, Integer> artistCounts = new ConcurrentHashMap<>();
    private final Set<String> albumSet = ConcurrentHashMap.newKeySet();
    private final Set<String> distinctAlbumFolders = ConcurrentHashMap.newKeySet();
    private final List<FolderTagIssue> missingTagFolders = Collections.synchronizedList(new ArrayList<>());
    private final List<FolderCoverIssue> missingCoverFolders = Collections.synchronizedList(new ArrayList<>());

    private volatile boolean isCached = false;
    private volatile boolean isScanning = false;

    public record FolderTagIssue(String folderName, String folderPath) {}
    public record FolderCoverIssue(String folderName, String folderPath) {}

    public static class LibraryCacheData {
        public Map<String, Integer> artistCounts = new HashMap<>();
        public Set<String> albumSet = new HashSet<>();
        public Set<String> distinctAlbumFolders = new HashSet<>();
        public Map<String, Long> folderTimestamps = new HashMap<>();
    }

    private Map<String, Long> folderTimestamps = new ConcurrentHashMap<>();

    static {
        java.util.logging.Logger.getLogger("org.jaudiotagger").setLevel(java.util.logging.Level.OFF);
    }

    public LibraryService() {
        loadCacheFromDisk();
    }

    public synchronized void updateCache(boolean forceFullRescan) {
        if (isScanning) return;
        isScanning = true;

        String baseFolder = AppConfig.getInstance().getLocalLibraryPath();
        Path basePath = Paths.get(baseFolder);

        if (!Files.exists(basePath)) {
            AppLogger.log("Aviso: Pasta da biblioteca local não encontrada: " + baseFolder);
            isScanning = false;
            isCached = true;
            return;
        }

        AppLogger.log("A indexar biblioteca de música em: " + baseFolder + "...");
        long startTime = System.currentTimeMillis();

        if (forceFullRescan) {
            artistCounts.clear();
            albumSet.clear();
            distinctAlbumFolders.clear();
            missingTagFolders.clear();
            missingCoverFolders.clear();
            folderTimestamps.clear();
        }

        try {
            Files.walkFileTree(basePath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(basePath)) return FileVisitResult.CONTINUE;

                    // Verifica se a pasta já foi examinada e não mudou
                    long lastModified = attrs.lastModifiedTime().toMillis();
                    String dirStr = dir.toAbsolutePath().toString();

                    Long cachedTime = folderTimestamps.get(dirStr);
                    boolean modified = cachedTime == null || cachedTime < lastModified;

                    if (!modified && !forceFullRescan) {
                        return FileVisitResult.CONTINUE;
                    }

                    inspectAlbumDirectory(dir.toFile());
                    folderTimestamps.put(dirStr, lastModified);
                    return FileVisitResult.CONTINUE;
                }
            });

            isCached = true;
            long elapsed = System.currentTimeMillis() - startTime;
            AppLogger.log("Indexação da biblioteca concluída em " + elapsed + "ms. Álbuns indexados: " + getAlbumCount());
            saveCacheToDisk();
        } catch (Exception e) {
            AppLogger.log("Erro ao varrer biblioteca local: " + e.getMessage());
        } finally {
            isScanning = false;
        }
    }

    private void inspectAlbumDirectory(File dir) {
        File[] files = dir.listFiles((d, name) -> {
            String lower = name.toLowerCase();
            return lower.endsWith(".flac") || lower.endsWith(".mp3") || lower.endsWith(".m4a") || lower.endsWith(".wav") || lower.endsWith(".ogg");
        });

        if (files == null || files.length == 0) return;

        distinctAlbumFolders.add(dir.getAbsolutePath());

        String folderName = dir.getName();
        albumSet.add(StringNormalizer.normalize(folderName));
        String cleanFolder = StringNormalizer.cleanAlbumTitle(folderName);
        if (!cleanFolder.isBlank()) {
            albumSet.add(StringNormalizer.normalize(cleanFolder));
        }

        // Determina o Artista associado à pasta (pasta pai na hierarquia Artista/Álbum)
        String parentArtist = null;
        File parent = dir.getParentFile();
        if (parent != null) {
            String parentName = parent.getName();
            if (!parentName.equalsIgnoreCase("albums") && !parentName.equalsIgnoreCase("music") && !parentName.equalsIgnoreCase("musica")) {
                parentArtist = StringNormalizer.normalize(parentName);
            }
        }

        File firstAudio = files[0];
        File firstFlac = null;
        for (File f : files) {
            if (f.getName().toLowerCase().endsWith(".flac")) {
                firstFlac = f;
                firstAudio = f;
                break;
            }
        }

        String tagArtist = null;
        try {
            org.jaudiotagger.audio.AudioFile af = org.jaudiotagger.audio.AudioFileIO.read(firstAudio);
            org.jaudiotagger.tag.Tag tag = af.getTag();
            if (tag != null) {
                String artist = tag.getFirst(org.jaudiotagger.tag.FieldKey.ARTIST);
                String album = tag.getFirst(org.jaudiotagger.tag.FieldKey.ALBUM);
                if (artist != null && !artist.isBlank()) {
                    tagArtist = StringNormalizer.normalize(artist);
                }
                if (album != null && !album.isBlank()) {
                    albumSet.add(StringNormalizer.normalize(album));
                    String cleanTagAlbum = StringNormalizer.cleanAlbumTitle(album);
                    if (!cleanTagAlbum.isBlank()) {
                        albumSet.add(StringNormalizer.normalize(cleanTagAlbum));
                    }
                }
            }
        } catch (Exception ignored) {}

        // Incrementa o contador de álbuns do artista UMA ÚNICA VEZ por pasta de álbum
        String resolvedArtist = (tagArtist != null && !tagArtist.isBlank()) ? tagArtist : parentArtist;
        if (resolvedArtist != null && !resolvedArtist.isBlank()) {
            artistCounts.merge(resolvedArtist, 1, Integer::sum);
        }

        if (firstFlac != null) {
            boolean hasTags = taggingService.hasAlbumArtistTags(firstFlac);
            if (!hasTags) {
                missingTagFolders.add(new FolderTagIssue(folderName, dir.getAbsolutePath()));
            }

            boolean hasCover = taggingService.hasCoverArt(firstFlac);
            if (!hasCover) {
                missingCoverFolders.add(new FolderCoverIssue(folderName, dir.getAbsolutePath()));
            }
        }
    }

    public boolean checkAlbumExists(String targetAlbum) {
        if (targetAlbum == null || targetAlbum.isBlank()) return false;
        String normTarget = StringNormalizer.normalize(targetAlbum);
        String cleanTarget = StringNormalizer.normalize(StringNormalizer.cleanAlbumTitle(targetAlbum));

        // 1. Verificação O(1) direta (nome original ou limpo)
        if (albumSet.contains(normTarget) || (!cleanTarget.isBlank() && albumSet.contains(cleanTarget))) {
            return true;
        }

        // 2. Busca flexível por substring
        for (String cached : albumSet) {
            if (cached.contains(normTarget) || normTarget.contains(cached)
                    || (!cleanTarget.isBlank() && (cached.contains(cleanTarget) || cleanTarget.contains(cached)))) {
                return true;
            }
        }
        return false;
    }

    public int getArtistAlbumCount(String artistName) {
        if (artistName == null) return 0;
        String norm = StringNormalizer.normalize(artistName);
        return artistCounts.getOrDefault(norm, 0);
    }

    public Map<String, Integer> getArtistCounts() {
        return Collections.unmodifiableMap(artistCounts);
    }

    public List<FolderTagIssue> getMissingTagFolders() {
        return new ArrayList<>(missingTagFolders);
    }

    public List<FolderCoverIssue> getMissingCoverFolders() {
        return new ArrayList<>(missingCoverFolders);
    }

    public int getAlbumCount() {
        return distinctAlbumFolders.isEmpty() ? albumSet.size() : distinctAlbumFolders.size();
    }

    public boolean isCached() {
        return isCached;
    }

    public boolean isScanning() {
        return isScanning;
    }

    private void saveCacheToDisk() {
        try {
            Files.createDirectories(cacheFilePath.getParent());
            LibraryCacheData data = new LibraryCacheData();
            data.artistCounts = new HashMap<>(artistCounts);
            data.albumSet = new HashSet<>(albumSet);
            data.distinctAlbumFolders = new HashSet<>(distinctAlbumFolders);
            data.folderTimestamps = new HashMap<>(folderTimestamps);
            mapper.writeValue(cacheFilePath.toFile(), data);
        } catch (IOException e) {
            AppLogger.log("Aviso ao guardar cache no disco: " + e.getMessage());
        }
    }

    private void loadCacheFromDisk() {
        if (!Files.exists(cacheFilePath)) return;
        try {
            LibraryCacheData data = mapper.readValue(cacheFilePath.toFile(), LibraryCacheData.class);
            if (data != null) {
                if (data.artistCounts != null) artistCounts.putAll(data.artistCounts);
                if (data.albumSet != null) {
                    albumSet.addAll(data.albumSet);
                    for (String a : data.albumSet) {
                        String clean = StringNormalizer.normalize(StringNormalizer.cleanAlbumTitle(a));
                        if (!clean.isBlank()) albumSet.add(clean);
                    }
                }
                if (data.distinctAlbumFolders != null) distinctAlbumFolders.addAll(data.distinctAlbumFolders);
                if (data.folderTimestamps != null) folderTimestamps.putAll(data.folderTimestamps);
                isCached = true;
                AppLogger.log("Cache da biblioteca carregada do disco: " + getAlbumCount() + " álbuns conhecidos.");
            }
        } catch (Exception e) {
            AppLogger.log("Cache de biblioteca corrompida ou antiga, será recriada.");
        }
    }
}
