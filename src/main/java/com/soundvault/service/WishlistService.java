package com.soundvault.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.soundvault.model.WishlistItem;
import com.soundvault.util.AppLogger;
import com.soundvault.util.StringNormalizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class WishlistService {

    private static final Path CACHE_FILE = Paths.get("cache", "wishlist.json");
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final List<WishlistItem> items = new CopyOnWriteArrayList<>();
    private final List<Consumer<WishlistItem>> addListeners = new CopyOnWriteArrayList<>();

    public WishlistService() {
        loadItems();
    }

    public synchronized void loadItems() {
        items.clear();
        if (Files.exists(CACHE_FILE)) {
            try {
                List<WishlistItem> loaded = mapper.readValue(CACHE_FILE.toFile(), new TypeReference<List<WishlistItem>>() {});
                if (loaded != null) {
                    items.addAll(loaded);
                    AppLogger.log("Wishlist carregada do disco: " + items.size() + " itens.");
                }
            } catch (Exception e) {
                AppLogger.log("Erro ao carregar wishlist do disco: " + e.getMessage());
            }
        }
    }

    public synchronized void saveItems() {
        try {
            Files.createDirectories(CACHE_FILE.getParent());
            mapper.writeValue(CACHE_FILE.toFile(), new ArrayList<>(items));
        } catch (IOException e) {
            AppLogger.log("Erro ao gravar wishlist no disco: " + e.getMessage());
        }
    }

    public List<WishlistItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public int getItemCount() {
        return items.size();
    }

    public synchronized WishlistItem addItem(String artist, String title, String format, String notes, String imageUrl, String spotifyId, String year) {
        // Verificar se já existe para evitar duplicações
        for (WishlistItem existing : items) {
            if (isSameAlbum(existing.artist(), existing.title(), artist, title)) {
                return existing;
            }
        }

        WishlistItem item = WishlistItem.of(artist.trim(), title.trim(), format, notes, imageUrl, spotifyId, year);
        items.add(0, item); // adiciona no topo
        saveItems();
        AppLogger.log("Item adicionado à Wishlist: " + artist + " - " + title);

        for (Consumer<WishlistItem> listener : addListeners) {
            try {
                listener.accept(item);
            } catch (Exception ignored) {}
        }
        return item;
    }

    public synchronized boolean removeItem(String id) {
        boolean removed = items.removeIf(i -> i.id().equals(id));
        if (removed) {
            saveItems();
            AppLogger.log("Item removido da Wishlist [ID: " + id + "]");
        }
        return removed;
    }

    public synchronized boolean toggleWishlist(String artist, String title, String imageUrl, String spotifyId, String year, String notes) {
        for (WishlistItem item : items) {
            if (isSameAlbum(item.artist(), item.title(), artist, title)) {
                removeItem(item.id());
                return false; // agora não está na wishlist
            }
        }
        addItem(artist, title, "CD", notes, imageUrl, spotifyId, year);
        return true; // adicionado à wishlist
    }

    public synchronized boolean toggleWishlist(String artist, String title) {
        return toggleWishlist(artist, title, null, null, null, null);
    }

    public boolean isInWishlist(String artist, String title) {
        if (artist == null || title == null) return false;
        for (WishlistItem item : items) {
            if (isSameAlbum(item.artist(), item.title(), artist, title)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void markAsAcquired(String id, boolean acquired, PhysicalLibraryService physicalService) {
        for (int i = 0; i < items.size(); i++) {
            WishlistItem item = items.get(i);
            if (item.id().equals(id)) {
                WishlistItem updated = item.withAcquired(acquired);
                items.set(i, updated);
                saveItems();

                // Se foi adquirido e o utilizador tem physicalService, adicionar automaticamente à Biblioteca Física
                if (acquired && physicalService != null) {
                    boolean alreadyInPhysical = physicalService.getAlbums().stream()
                            .anyMatch(p -> isSameAlbum(p.artist(), p.title(), item.artist(), item.title()));
                    if (!alreadyInPhysical) {
                        var added = physicalService.addAlbum(item.artist(), item.title(), item.format(), item.notes());
                        if (item.imageUrl() != null && !item.imageUrl().isBlank()) {
                            physicalService.updateAlbum(added.withSpotifyDetails(item.imageUrl(), item.spotifyId(), item.releaseYear(), 0));
                        }
                        AppLogger.log("Álbum adquirido transferido para a Biblioteca Física: " + item.artist() + " - " + item.title());
                    }
                }
                return;
            }
        }
    }

    public void addOnItemAddedListener(Consumer<WishlistItem> listener) {
        if (listener != null) addListeners.add(listener);
    }

    private boolean isSameAlbum(String a1, String t1, String a2, String t2) {
        String nA1 = StringNormalizer.normalize(a1);
        String nT1 = StringNormalizer.normalize(StringNormalizer.cleanAlbumTitle(t1));
        String nA2 = StringNormalizer.normalize(a2);
        String nT2 = StringNormalizer.normalize(StringNormalizer.cleanAlbumTitle(t2));

        return nA1.equals(nA2) && nT1.equals(nT2);
    }
}
