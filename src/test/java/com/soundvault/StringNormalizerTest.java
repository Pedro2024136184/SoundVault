package com.soundvault;

import com.soundvault.util.StringNormalizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StringNormalizerTest {

    @Test
    void testNormalizeRemovesAccentsAndLowercases() {
        assertEquals("musica", StringNormalizer.normalize("Música"));
        assertEquals("cancao", StringNormalizer.normalize("Canção"));
        assertEquals("bjork", StringNormalizer.normalize("Björk"));
        assertEquals("sigur ros", StringNormalizer.normalize("Sigur Rós"));
    }

    @Test
    void testNormalizeHandlesBlankAndNull() {
        assertEquals("", StringNormalizer.normalize(null));
        assertEquals("", StringNormalizer.normalize("   "));
    }

    @Test
    void testNormalizeTrimsWhitespace() {
        assertEquals("fleetwood mac", StringNormalizer.normalize("  Fleetwood Mac   "));
    }

    @Test
    void testCleanAlbumTitleRemovesRemastersAndExpanded() {
        assertEquals("Paranoid", StringNormalizer.cleanAlbumTitle("Paranoid (Remastered 2023)"));
        assertEquals("Paranoid", StringNormalizer.cleanAlbumTitle("Paranoid (2020 Remaster)"));
        assertEquals("Paranoid", StringNormalizer.cleanAlbumTitle("Paranoid - 2020 Remaster"));
        assertEquals("OK Computer", StringNormalizer.cleanAlbumTitle("OK Computer (Expanded Edition)"));
        assertEquals("In Rainbows", StringNormalizer.cleanAlbumTitle("In Rainbows (Disk 2 / Expanded)"));
        assertEquals("Dirt", StringNormalizer.cleanAlbumTitle("Dirt (2022 Remaster)"));
        assertEquals("The Dark Side of the Moon", StringNormalizer.cleanAlbumTitle("The Dark Side of the Moon (50th Anniversary Remaster)"));
        assertEquals("Abbey Road", StringNormalizer.cleanAlbumTitle("Abbey Road - 2019 Mix"));
        assertEquals("Bob Marley Legend", StringNormalizer.cleanAlbumTitle("Bob Marley Legend [Deluxe Edition] (CD1)"));
        assertEquals("Back in Black", StringNormalizer.cleanAlbumTitle("Back in Black - 2003 Remaster"));
        assertEquals("Thriller", StringNormalizer.cleanAlbumTitle("Thriller 25 Super Deluxe Edition"));
        assertEquals("The Slim Shady LP", StringNormalizer.cleanAlbumTitle("The Slim Shady LP (Expanded Edition)"));
        assertEquals("Born to Die", StringNormalizer.cleanAlbumTitle("Born to Die - The Paradise Edition"));
    }

    @Test
    void testCleanAlbumTitlePreservesOriginalWhenNoNoise() {
        assertEquals("ASTROWORLD", StringNormalizer.cleanAlbumTitle("ASTROWORLD"));
        assertEquals("Selected Ambient Works 85-92", StringNormalizer.cleanAlbumTitle("Selected Ambient Works 85-92"));
    }

    @Test
    void testCleanTrackTitleRemovesRemastersAndEdits() {
        assertEquals("Smells Like Teen Spirit", StringNormalizer.cleanTrackTitle("Smells Like Teen Spirit - Remastered"));
        assertEquals("Iron Man", StringNormalizer.cleanTrackTitle("Iron Man - 2014 Remaster"));
        assertEquals("Karma Police", StringNormalizer.cleanTrackTitle("Karma Police (Remastered)"));
        assertEquals("Comfortably Numb", StringNormalizer.cleanTrackTitle("Comfortably Numb - 2011 Remastered Version"));
        assertEquals("Paint It, Black", StringNormalizer.cleanTrackTitle("Paint It, Black - Mono"));
    }

    @Test
    void testCleanArtistSanitizesSlashes() {
        assertEquals("AC DC", StringNormalizer.cleanArtist("AC/DC"));
        assertEquals("Tyler The Creator", StringNormalizer.cleanArtist("Tyler, The Creator"));
    }

    @Test
    void testBuildSearchQueryProducesCleanSoulseekQuery() {
        assertEquals("Black Sabbath Paranoid flac",
                StringNormalizer.buildSearchQuery("Black Sabbath", "Paranoid (Remastered 2023)", true, true));

        assertEquals("Radiohead OK Computer flac",
                StringNormalizer.buildSearchQuery("Radiohead", "OK Computer (Expanded Edition)", true, true));

        assertEquals("AC DC Back in Black flac",
                StringNormalizer.buildSearchQuery("AC/DC", "Back in Black - 2003 Remaster", true, true));

        assertEquals("Nirvana Smells Like Teen Spirit flac",
                StringNormalizer.buildSearchQuery("Nirvana", "Smells Like Teen Spirit - Remastered", false, true));

        assertEquals("Radiohead In Rainbows flac",
                StringNormalizer.buildSearchQuery("Radiohead", "In Rainbows (Disk 2 / Expanded)", true, true));
    }
}
