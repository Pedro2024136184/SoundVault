package com.soundvault;

import com.soundvault.model.DownloadTransfer;
import com.soundvault.model.SoulseekFile;
import com.soundvault.service.SlskdService;
import com.soundvault.util.StringNormalizer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class AlbumFallbackVerificationTest {

    @Test
    void testLegitimateAlbumFolderVerification() {
        SlskdService slskd = new SlskdService();

        String artist = "Deftones";
        String album = "Deftones";
        String folderPath = "Music\\Deftones\\2003 - Deftones";

        List<SoulseekFile> files = List.of(
                new SoulseekFile("Music\\Deftones\\2003 - Deftones\\01 - Hexagram.flac", 35000000, 1000, 240),
                new SoulseekFile("Music\\Deftones\\2003 - Deftones\\02 - Needles and Pins.flac", 32000000, 1000, 200),
                new SoulseekFile("Music\\Deftones\\2003 - Deftones\\03 - Minerva.flac", 38000000, 1000, 260),
                new SoulseekFile("Music\\Deftones\\2003 - Deftones\\04 - Good Morning Beautiful.flac", 30000000, 1000, 210)
        );

        List<String> knownTracks = List.of("Hexagram", "Needles and Pins", "Minerva", "Good Morning Beautiful", "Deathblow");

        boolean isLegit = slskd.isFolderLegitimateAlbum(artist, album, folderPath, files, knownTracks, 11);
        assertTrue(isLegit, "A pasta com múltiplas faixas coincidentes do mesmo álbum deve ser aprovada.");

        int matches = slskd.countMatchingAlbumTracks(files, knownTracks);
        assertEquals(4, matches, "Devem ser contabilizadas 4 faixas coincidentes.");
    }

    @Test
    void testRejectMismatchedArtistFolder() {
        SlskdService slskd = new SlskdService();

        String artist = "Deftones";
        String album = "Deftones";
        // Outro artista com uma música chamada "Hexagram"
        String folderPath = "Music\\Armin van Buuren\\Hexagram Single";

        List<SoulseekFile> files = List.of(
                new SoulseekFile("Music\\Armin van Buuren\\Hexagram Single\\01 - Hexagram (Original Mix).flac", 40000000, 1000, 300),
                new SoulseekFile("Music\\Armin van Buuren\\Hexagram Single\\02 - Hexagram (Club Mix).flac", 42000000, 1000, 320),
                new SoulseekFile("Music\\Armin van Buuren\\Hexagram Single\\03 - Hexagram (Dub).flac", 39000000, 1000, 280)
        );

        List<String> knownTracks = List.of("Hexagram", "Needles and Pins", "Minerva");

        boolean isLegit = slskd.isFolderLegitimateAlbum(artist, album, folderPath, files, knownTracks, 11);
        assertFalse(isLegit, "Deve rejeitar pastas onde o artista pretendido não consta no caminho ou ficheiros.");
    }

    @Test
    void testRejectCompilationWithOnlyOneMatchingTrack() {
        SlskdService slskd = new SlskdService();

        String artist = "Deftones";
        String album = "Deftones";
        // Compilação genérica que contém apenas 1 música da banda
        String folderPath = "Compilations\\Various Artists - 2000s Alternative Hits";

        List<SoulseekFile> files = List.of(
                new SoulseekFile("Compilations\\Various Artists - 2000s Alternative Hits\\01 - Linkin Park - In the End.flac", 30000000, 1000, 210),
                new SoulseekFile("Compilations\\Various Artists - 2000s Alternative Hits\\02 - Deftones - Hexagram.flac", 35000000, 1000, 240),
                new SoulseekFile("Compilations\\Various Artists - 2000s Alternative Hits\\03 - System of a Down - Chop Suey.flac", 33000000, 1000, 210)
        );

        List<String> knownTracks = List.of("Hexagram", "Needles and Pins", "Minerva", "Good Morning Beautiful");

        boolean isLegit = slskd.isFolderLegitimateAlbum(artist, album, folderPath, files, knownTracks, 11);
        assertFalse(isLegit, "Deve rejeitar compilações que contenham apenas uma faixa isolada do álbum.");
    }

    @Test
    void testSelfTitledAlbumDetection() {
        String artist = "Deftones";
        String album = "Deftones";

        boolean isSelfTitled = StringNormalizer.normalize(artist).equalsIgnoreCase(StringNormalizer.normalize(album));
        assertTrue(isSelfTitled, "Deve reconhecer álbuns homónimos (Self-Titled).");

        String metallicaArt = "Metallica";
        String metallicaAlb = "Metallica (Remastered 2021)";
        String cleanAlb = StringNormalizer.cleanAlbumTitle(metallicaAlb);
        // "Metallica (Remastered 2021)" limpo torna-se "Metallica"
        assertTrue(StringNormalizer.normalize(metallicaArt).equalsIgnoreCase(StringNormalizer.normalize(cleanAlb)));
    }

    @Test
    void testDownloadTransferFormattingAndStates() {
        DownloadTransfer active = new DownloadTransfer(
                "peer123",
                "FLAC\\Deftones\\2003 - Deftones\\01 - Hexagram.flac",
                "InProgress",
                52428800L, // 50 MB
                45.5,
                4800000.0, // 4.8 MB/s
                23850000L
        );

        assertEquals("01 - Hexagram.flac", active.getCleanFileName());
        assertEquals("2003 - Deftones", active.getDirectoryName());
        assertEquals("4.8 MB/s", active.getFormattedSpeed());
        assertEquals("46%", active.getFormattedProgress());
        assertEquals("52.4 MB", active.getFormattedSize());

        assertTrue(active.isInProgress());
        assertFalse(active.isCompleted());
        assertFalse(active.isQueued());

        DownloadTransfer completed = new DownloadTransfer("peer123", "song.flac", "Completed, Succeeded", 1000, 100.0);
        assertTrue(completed.isCompleted());
        assertFalse(completed.isInProgress());

        DownloadTransfer queued = new DownloadTransfer("peer123", "song.flac", "QueuedRemotely", 1000, 0);
        assertTrue(queued.isQueued());
    }
}
