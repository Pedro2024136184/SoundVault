package com.soundvault;

import com.soundvault.model.PhysicalAlbum;
import com.soundvault.model.WishlistItem;
import com.soundvault.service.PhysicalLibraryService;
import com.soundvault.service.WishlistService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class WishlistServiceTest {

    private static final Path CACHE_FILE = Paths.get("cache", "wishlist.json");
    private static final Path BACKUP_FILE = Paths.get("cache", "wishlist.json.bak");

    @BeforeAll
    static void backupCache() throws IOException {
        if (Files.exists(CACHE_FILE)) {
            Files.copy(CACHE_FILE, BACKUP_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @AfterAll
    static void restoreCache() throws IOException {
        if (Files.exists(BACKUP_FILE)) {
            Files.move(BACKUP_FILE, CACHE_FILE, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @BeforeEach
    void setUp() throws IOException {
        Files.deleteIfExists(CACHE_FILE);
    }

    @Test
    void testAddAndRetrieveWishlistItem() {
        WishlistService service = new WishlistService();
        assertEquals(0, service.getItemCount());

        WishlistItem item = service.addItem("Daft Punk", "Discovery", "CD", "Edição Japonesa", "http://cover.jpg", "spot123", "2001");
        assertNotNull(item.id());
        assertEquals("Daft Punk", item.artist());
        assertEquals("Discovery", item.title());
        assertEquals("CD", item.format());
        assertEquals("Edição Japonesa", item.notes());
        assertEquals("2001", item.releaseYear());
        assertFalse(item.acquired());

        assertEquals(1, service.getItemCount());
        assertTrue(service.isInWishlist("Daft Punk", "Discovery"));
        assertTrue(service.isInWishlist("daft punk", "discovery (remastered)"));
    }

    @Test
    void testDuplicatePrevention() {
        WishlistService service = new WishlistService();
        service.addItem("Radiohead", "OK Computer", "CD", "", null, null, "1997");
        service.addItem("Radiohead", "OK Computer (Collector's Edition)", "CD", "", null, null, "1997");

        // Deve impedir duplicação através da normalização de títulos
        assertEquals(1, service.getItemCount(), "Não deve duplicar álbuns com títulos normalizáveis equivalentes.");
    }

    @Test
    void testToggleWishlist() {
        WishlistService service = new WishlistService();

        // 1. Toggle On
        boolean added = service.toggleWishlist("The Cure", "Disintegration");
        assertTrue(added);
        assertTrue(service.isInWishlist("The Cure", "Disintegration"));
        assertEquals(1, service.getItemCount());

        // 2. Toggle Off
        boolean removed = service.toggleWishlist("The Cure", "Disintegration");
        assertFalse(removed);
        assertFalse(service.isInWishlist("The Cure", "Disintegration"));
        assertEquals(0, service.getItemCount());
    }

    @Test
    void testRemoveItem() {
        WishlistService service = new WishlistService();
        WishlistItem item = service.addItem("Massive Attack", "Mezzanine", "Vinil", "Duplo LP", null, null, "1998");
        assertEquals(1, service.getItemCount());

        boolean ok = service.removeItem(item.id());
        assertTrue(ok);
        assertEquals(0, service.getItemCount());
        assertFalse(service.isInWishlist("Massive Attack", "Mezzanine"));
    }

    @Test
    void testMarkAsAcquiredAndTransferToPhysicalLibrary() {
        WishlistService service = new WishlistService();
        PhysicalLibraryService physicalService = new PhysicalLibraryService();

        WishlistItem item = service.addItem("Portishead", "Dummy", "CD", "Comprado na Fnac", "http://dummy.jpg", "sp_portishead", "1994");
        assertFalse(item.acquired());

        // Marcar como adquirido e transferir
        service.markAsAcquired(item.id(), true, physicalService);

        WishlistItem updated = service.getItems().stream()
                .filter(i -> i.id().equals(item.id()))
                .findFirst()
                .orElse(null);

        assertNotNull(updated);
        assertTrue(updated.acquired());

        // Deve ter sido inserido na Biblioteca Física automaticamente
        boolean inPhysical = physicalService.getAlbums().stream()
                .anyMatch(a -> a.artist().equalsIgnoreCase("Portishead") && a.title().equalsIgnoreCase("Dummy"));
        assertTrue(inPhysical, "Álbum adquirido deve ser automaticamente transferido para a Biblioteca Física.");
    }

    @Test
    void testDiskPersistence() {
        WishlistService service1 = new WishlistService();
        service1.addItem("Nirvana", "In Utero", "CD", "20th Anniversary", "http://inutero.jpg", "spot_nirv", "1993");

        // Criar nova instância para ler do disco
        WishlistService service2 = new WishlistService();
        assertEquals(1, service2.getItemCount());
        WishlistItem loaded = service2.getItems().get(0);
        assertEquals("Nirvana", loaded.artist());
        assertEquals("In Utero", loaded.title());
        assertEquals("1993", loaded.releaseYear());
    }
}
