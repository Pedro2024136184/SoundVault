package com.soundvault;

import com.soundvault.config.AppConfig;
import com.soundvault.service.LibraryService;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class LibraryServiceTest {

    @Test
    void testLibraryIndexing() {
        AppConfig config = AppConfig.getInstance();
        config.load();
        String path = config.getLocalLibraryPath();
        System.out.println("Configured Library Path: " + path);
        File dir = new File(path);
        assertTrue(dir.exists(), "A pasta de música configurada deve existir no sistema!");

        LibraryService library = new LibraryService();
        library.updateCache(true);

        System.out.println("Álbuns indexados: " + library.getAlbumCount());
        System.out.println("Artistas encontrados: " + library.getArtistCounts().size());
        assertTrue(library.getAlbumCount() > 0, "A biblioteca local deve indexar álbuns existentes!");
    }

    @Test
    void testCheckAlbumExistsWithRemasterSuffix() {
        LibraryService library = new LibraryService();
        library.updateCache(false);

        // Paranoid existe no disco em C:\Users\pedro\Music\Albums\Black Sabbath\Paranoid
        assertTrue(library.checkAlbumExists("Paranoid (Remastered 2023)"), "Deve encontrar Paranoid mesmo com sufixo (Remastered 2023)");
        assertTrue(library.checkAlbumExists("Paranoid (2020 Remaster)"), "Deve encontrar Paranoid mesmo com sufixo (2020 Remaster)");
        assertTrue(library.checkAlbumExists("Paranoid"), "Deve encontrar Paranoid com o nome exato");

        // Dirt existe no disco em C:\Users\pedro\Music\Albums\Alice In Chains\Dirt (2022 Remaster)
        assertTrue(library.checkAlbumExists("Dirt"), "Deve encontrar Dirt mesmo se a pasta tiver (2022 Remaster) e o Spotify só disser Dirt");
        assertTrue(library.checkAlbumExists("Dirt (Remastered)"), "Deve encontrar Dirt mesmo se o Spotify disser Dirt (Remastered)");
    }
}
