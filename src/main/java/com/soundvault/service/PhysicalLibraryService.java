package com.soundvault.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.soundvault.model.Album;
import com.soundvault.model.PhysicalAlbum;
import com.soundvault.util.AppLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class PhysicalLibraryService {

    private static final Path CACHE_FILE = Paths.get("cache", "physical_library.json");
    private final ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final List<PhysicalAlbum> albums = new CopyOnWriteArrayList<>();

    public PhysicalLibraryService() {
        loadAlbums();
    }

    public synchronized void loadAlbums() {
        albums.clear();
        if (Files.exists(CACHE_FILE)) {
            try {
                List<PhysicalAlbum> loaded = mapper.readValue(CACHE_FILE.toFile(), new TypeReference<List<PhysicalAlbum>>() {});
                if (loaded != null && !loaded.isEmpty()) {
                    albums.addAll(loaded);
                    AppLogger.log("Biblioteca física carregada do disco: " + albums.size() + " álbuns conhecidos.");
                    return;
                }
            } catch (Exception e) {
                AppLogger.log("Erro ao carregar biblioteca física do disco: " + e.getMessage());
            }
        }

        // Se o ficheiro não existe ou estava vazio, semeia os 60 álbuns iniciais fornecidos
        seedInitialAlbums();
        saveAlbums();
    }

    public synchronized void saveAlbums() {
        try {
            Files.createDirectories(CACHE_FILE.getParent());
            mapper.writeValue(CACHE_FILE.toFile(), new ArrayList<>(albums));
            AppLogger.log("Biblioteca física guardada com sucesso: " + albums.size() + " álbuns.");
        } catch (IOException e) {
            AppLogger.log("Erro ao gravar biblioteca física: " + e.getMessage());
        }
    }

    public List<PhysicalAlbum> getAlbums() {
        return Collections.unmodifiableList(albums);
    }

    public int getAlbumCount() {
        return albums.size();
    }

    public synchronized PhysicalAlbum addAlbum(String artist, String title, String format, String notes) {
        PhysicalAlbum newAlbum = PhysicalAlbum.of(artist.trim(), title.trim(), format, notes);
        albums.add(newAlbum);
        saveAlbums();
        return newAlbum;
    }

    public synchronized boolean removeAlbum(String id) {
        boolean removed = albums.removeIf(a -> a.id().equals(id));
        if (removed) {
            saveAlbums();
        }
        return removed;
    }

    public synchronized void updateAlbum(PhysicalAlbum updated) {
        for (int i = 0; i < albums.size(); i++) {
            if (albums.get(i).id().equals(updated.id())) {
                albums.set(i, updated);
                saveAlbums();
                return;
            }
        }
    }

    public boolean hasDigitalRip(PhysicalAlbum album, LibraryService libraryService) {
        if (album == null || libraryService == null) return false;
        return libraryService.checkAlbumExists(album.title());
    }

    public void syncMissingCovers(SpotifyService spotifyService, Consumer<PhysicalAlbum> onUpdated, Runnable onComplete) {
        Thread.ofVirtual().start(() -> {
            AppLogger.log("A iniciar sincronização de capas de CDs físicos com o Spotify...");
            int updatedCount = 0;
            for (PhysicalAlbum album : albums) {
                if (album.imageUrl() == null || album.imageUrl().isBlank()) {
                    try {
                        Optional<Album> found = spotifyService.searchAlbum(album.artist(), album.title());
                        if (found.isPresent()) {
                            Album spAlbum = found.get();
                            String year = "";
                            if (spAlbum.releaseDate() != null && spAlbum.releaseDate().length() >= 4) {
                                year = spAlbum.releaseDate().substring(0, 4);
                            }
                            PhysicalAlbum enriched = album.withSpotifyDetails(
                                    spAlbum.imageUrl(),
                                    spAlbum.id(),
                                    year,
                                    spAlbum.totalTracks()
                            );
                            updateAlbum(enriched);
                            updatedCount++;
                            if (onUpdated != null) {
                                onUpdated.accept(enriched);
                            }
                        }
                        Thread.sleep(150);
                    } catch (Exception e) {
                        AppLogger.log("Erro ao sincronizar capa de " + album.artist() + " - " + album.title() + ": " + e.getMessage());
                    }
                }
            }
            AppLogger.log("Sincronização de capas concluída: " + updatedCount + " capas atualizadas.");
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void seedInitialAlbums() {
        AppLogger.log("A semear coleção física com os 60 álbuns em CD fornecidos...");
        List<String[]> seeds = List.of(
                new String[]{"ABBA", "Love Stories"},
                new String[]{"Alice In Chains", "Dirt"},
                new String[]{"Arctic Monkeys", "Favourite Worst Nightmare"},
                new String[]{"Arnaldo Antunes, Carlinhos Brown, Marisa Monte", "Tribalistas"},
                new String[]{"Bob Dylan", "The Bootleg Series Volumes 1-3 (Volume 1)"},
                new String[]{"Bob Dylan", "The Bootleg Series Volumes 1-3 (Volume 2)"},
                new String[]{"Bob Dylan", "Under the Red Sky"},
                new String[]{"Bruce Springsteen", "The Wild, the Innocent & the E Street Shuffle"},
                new String[]{"David Bowie", "The Next Day"},
                new String[]{"David Bowie", "Ziggy Stardust"},
                new String[]{"Deep Purple", "Abandon"},
                new String[]{"Deep Purple", "Made in Japan"},
                new String[]{"Deftones", "Diamond Eyes"},
                new String[]{"Dire Straits", "Brothers in Arms"},
                new String[]{"Dire Straits", "Communiqué"},
                new String[]{"Dire Straits", "Sultans of Swing: The Very Best of Dire Straits"},
                new String[]{"Eagles", "Hotel California"},
                new String[]{"Foo Fighters", "Greatest Hits"},
                new String[]{"GNR", "Grandes Êxitos"},
                new String[]{"Ivete Sangalo", "Perfil"},
                new String[]{"Jethro Tull", "Roots To Branches"},
                new String[]{"Jimi Hendrix", "The Ultimate Experience"},
                new String[]{"Julio Iglesias", "Minha Vida: Grandes Sucessos"},
                new String[]{"Kim Carnes", "Bette Davis Eyes"},
                new String[]{"Kings of Leon", "Only By The Night"},
                new String[]{"Limp Bizkit", "Chocolate Starfish and the Hot Dog Flavored Water"},
                new String[]{"Linkin Park", "Live in Texas"},
                new String[]{"LMFAO", "Sorry for Party Rocking"},
                new String[]{"Michael Bolton", "Only A Woman Like You"},
                new String[]{"Muse", "Drones"},
                new String[]{"Oasis", "(What's The Story) Morning Glory?"},
                new String[]{"Ornatos Violeta", "Cão!"},
                new String[]{"Pantera", "Vulgar Display of Power"},
                new String[]{"Pearl Jam", "Ten"},
                new String[]{"Pink Floyd", "Dark Side of the Moon"},
                new String[]{"Pink Floyd", "Meddle"},
                new String[]{"Pink Floyd", "Music from the Film More"},
                new String[]{"Pink Floyd", "The Wall"},
                new String[]{"Red Hot Chili Peppers", "By the Way"},
                new String[]{"Sam The Kid", "Pratica(mente)"},
                new String[]{"Scorpions", "Acoustica"},
                new String[]{"Scorpions", "Lovedrive"},
                new String[]{"Simon & Garfunkel", "The Essential"},
                new String[]{"Stevie Ray Vaughan and Double Trouble", "Couldn't Stand the Weather"},
                new String[]{"Stevie Ray Vaughan and Double Trouble", "Soul to Soul"},
                new String[]{"Stevie Ray Vaughan and Double Trouble", "Texas Flood"},
                new String[]{"Stevie Wonder", "Conversation Peace"},
                new String[]{"System of a Down", "Toxicity"},
                new String[]{"The Beach Boys", "Greatest Hits"},
                new String[]{"The Doors", "L.A. Woman"},
                new String[]{"The Doors", "Morrison Hotel"},
                new String[]{"The Doors", "Strange Days"},
                new String[]{"The Doors / Jim Morrison", "An American Prayer"},
                new String[]{"The Rolling Stones", "Goats Head Soup"},
                new String[]{"Tina Turner", "All The Best"},
                new String[]{"Tom Jones", "Reload"},
                new String[]{"U2", "War"},
                new String[]{"ZZ Top", "Greatest Hits"},
                new String[]{"Vários", "Dirty Dancing (Banda Sonora)"},
                new String[]{"Vários", "Fado nas Grandes Vozes"}
        );

        for (String[] seed : seeds) {
            albums.add(PhysicalAlbum.of(seed[0], seed[1]));
        }
    }
}
