package com.soundvault;

import com.soundvault.config.AppConfig;
import com.soundvault.service.LibraryService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {

    private String originalPath;

    @BeforeEach
    void setUp(@TempDir Path tempDir) throws IOException {
        AppConfig config = AppConfig.getInstance();
        config.load();
        originalPath = config.getLocalLibraryPath();

        // Cria estrutura de álbuns simulada em pasta temporária isolada
        Path paranoidDir = tempDir.resolve("Black Sabbath").resolve("Paranoid");
        Files.createDirectories(paranoidDir);
        Files.createFile(paranoidDir.resolve("01 - War Pigs.flac"));

        Path dirtDir = tempDir.resolve("Alice In Chains").resolve("Dirt (2022 Remaster)");
        Files.createDirectories(dirtDir);
        Files.createFile(dirtDir.resolve("01 - Them Bones.flac"));

        config.setLocalLibraryPath(tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        if (originalPath != null) {
            AppConfig.getInstance().setLocalLibraryPath(originalPath);
        }
    }

    @Test
    void testLibraryIndexing() {
        LibraryService library = new LibraryService();
        library.updateCache(true);

        System.out.println("Álbuns indexados: " + library.getAlbumCount());
        System.out.println("Artistas encontrados: " + library.getArtistCounts().size());
        assertTrue(library.getAlbumCount() > 0, "A biblioteca local deve indexar álbuns existentes!");
    }

    @Test
    void testCheckAlbumExistsWithRemasterSuffix() {
        LibraryService library = new LibraryService();
        library.updateCache(true);

        assertTrue(library.checkAlbumExists("Paranoid (Remastered 2023)"), "Deve encontrar Paranoid mesmo com sufixo (Remastered 2023)");
        assertTrue(library.checkAlbumExists("Paranoid (2020 Remaster)"), "Deve encontrar Paranoid mesmo com sufixo (2020 Remaster)");
        assertTrue(library.checkAlbumExists("Paranoid"), "Deve encontrar Paranoid com o nome exato");

        assertTrue(library.checkAlbumExists("Dirt"), "Deve encontrar Dirt mesmo se a pasta tiver (2022 Remaster) e o Spotify só disser Dirt");
        assertTrue(library.checkAlbumExists("Dirt (Remastered)"), "Deve encontrar Dirt mesmo se o Spotify disser Dirt (Remastered)");
    }
}

