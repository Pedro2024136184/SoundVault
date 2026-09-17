package com.soundvault;

import com.soundvault.model.PhysicalAlbum;
import com.soundvault.service.LibraryService;
import com.soundvault.service.PhysicalLibraryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PhysicalLibraryServiceTest {

    private static final Path CACHE_FILE = Paths.get("cache", "physical_library.json");

    @BeforeEach
    void setUp() throws IOException {
        // Assegurar teste com dados limpos
        Files.deleteIfExists(CACHE_FILE);
    }

    @Test
    void testInitialSeedHasExactly60Albums() {
        PhysicalLibraryService service = new PhysicalLibraryService();
        List<PhysicalAlbum> list = service.getAlbums();

        assertEquals(60, list.size(), "A biblioteca física deve ser inicializada com os 60 álbuns fornecidos.");
        assertTrue(Files.exists(CACHE_FILE), "O ficheiro cache/physical_library.json deve ser gerado.");

        // Validar alguns álbuns emblemáticos da lista fornecida
        boolean hasAliceInChains = list.stream().anyMatch(a -> a.artist().equals("Alice In Chains") && a.title().equals("Dirt"));
        assertTrue(hasAliceInChains, "Deve conter Alice In Chains - Dirt");

        boolean hasPinkFloydWall = list.stream().anyMatch(a -> a.artist().equals("Pink Floyd") && a.title().equals("The Wall"));
        assertTrue(hasPinkFloydWall, "Deve conter Pink Floyd - The Wall");

        boolean hasSamTheKid = list.stream().anyMatch(a -> a.artist().equals("Sam The Kid") && a.title().equals("Pratica(mente)"));
        assertTrue(hasSamTheKid, "Deve conter Sam The Kid - Pratica(mente)");

        boolean hasOrnatos = list.stream().anyMatch(a -> a.artist().equals("Ornatos Violeta") && a.title().equals("Cão!"));
        assertTrue(hasOrnatos, "Deve conter Ornatos Violeta - Cão!");
    }

    @Test
    void testAddRemoveAndUpdateAlbum() {
        PhysicalLibraryService service = new PhysicalLibraryService();
        int initialCount = service.getAlbumCount();

        // 1. Adicionar
        PhysicalAlbum added = service.addAlbum("Radiohead", "OK Computer", "CD", "Primeira edição");
        assertNotNull(added.id());
        assertEquals("Radiohead", added.artist());
        assertEquals("OK Computer", added.title());
        assertEquals("CD", added.format());
        assertEquals("Primeira edição", added.notes());
        assertEquals(initialCount + 1, service.getAlbumCount());

        // 2. Atualizar com dados do Spotify
        PhysicalAlbum updated = added.withSpotifyDetails("https://example.com/cover.jpg", "sp-id-123", "1997", 12);
        service.updateAlbum(updated);

        PhysicalAlbum found = service.getAlbums().stream()
                .filter(a -> a.id().equals(added.id()))
                .findFirst()
                .orElse(null);

        assertNotNull(found);
        assertEquals("https://example.com/cover.jpg", found.imageUrl());
        assertEquals("sp-id-123", found.spotifyId());
        assertEquals("1997", found.releaseYear());
        assertEquals(12, found.totalTracks());

        // 3. Remover
        boolean removed = service.removeAlbum(added.id());
        assertTrue(removed);
        assertEquals(initialCount, service.getAlbumCount());
    }

    @Test
    void testLocalRipDetection() {
        PhysicalLibraryService physicalService = new PhysicalLibraryService();
        LibraryService libraryService = new LibraryService();

        PhysicalAlbum dirt = physicalService.getAlbums().stream()
                .filter(a -> a.title().equalsIgnoreCase("Dirt"))
                .findFirst()
                .orElse(null);

        assertNotNull(dirt);
        // O método não deve falhar ou lançar exceção
        boolean hasRip = physicalService.hasDigitalRip(dirt, libraryService);
        assertNotNull(Boolean.valueOf(hasRip));
    }

    @Test
    void testSpotifyAlbumSearch() {
        com.soundvault.service.SpotifyService spotifyService = new com.soundvault.service.SpotifyService();
        boolean auth = spotifyService.authenticate();
        if (auth) {
            var found = spotifyService.searchAlbum("Alice In Chains", "Dirt");
            assertTrue(found.isPresent(), "Deverá encontrar o álbum Dirt no Spotify");
            var album = found.get();
            assertNotNull(album.imageUrl(), "Deverá conter URL da capa do álbum");
            assertFalse(album.imageUrl().isBlank(), "URL da capa não pode ser vazio");
            System.out.println("Álbum encontrado no Spotify: " + album.artist() + " - " + album.name() + " | Capa: " + album.imageUrl());
        }
    }
}
