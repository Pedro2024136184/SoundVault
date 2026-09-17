package com.soundvault.util;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class StringNormalizer {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

    // Palavras-chave de edições, remasters, versões comemorativas e extras
    private static final String EDITION_KEYWORDS =
            "remaster(?:ed)?|remastering|re-master(?:ed)?|expanded|deluxe|super\\s+deluxe|" +
            "anniversary|(?:[a-z0-9\\-']+\\s+)?(?:edition|version)|bonus|bonus\\s+track(?:s)?|" +
            "reissue|re-issue|mono|stereo|definitive|original|original\\s+album\\s+version|" +
            "special|collector'?s?|legacy|mix|live|disc\\s*\\d*|disk\\s*\\d*|cd\\s*\\d*|" +
            "audiophile|explicit|clean|remake|recording|master|hi-res|lossless";

    // 1. Padrão para parênteses e colchetes contendo anos ou palavras-chave de edições
    // Ex: (Remastered 2023), [Deluxe Edition], (2020 Remaster), (1973), (Disk 2 / Expanded), [CD1]
    private static final Pattern BRACKET_EDITION_PATTERN = Pattern.compile(
            "(?i)\\s*[\\[\\(]\\s*(?:(?:19|20)\\d{2}\\s*)?(?:(?:the|a|an)\\s+)?(?:\\d+(?:st|nd|rd|th)?\\s+)?(?:" +
            EDITION_KEYWORDS + "|(?:19|20)\\d{2}).*?[\\]\\)]"
    );

    // 2. Padrão para sufixos após hífen/travessão/dois-pontos/barra
    // Ex: - Remastered 2023, - 2014 Remaster, - 2019 Mix, - Deluxe Edition, - Expanded, - Mono, - The Paradise Edition
    private static final Pattern HYPHEN_EDITION_PATTERN = Pattern.compile(
            "(?i)\\s*[-–—:/]\\s*(?:(?:19|20)\\d{2}\\s*)?(?:(?:the|a|an)\\s+)?(?:\\d+(?:st|nd|rd|th)?\\s+)?(?:" +
            EDITION_KEYWORDS + ").*$"
    );

    // 3. Padrão para sufixos no fim da string sem hífen nem parênteses
    // Ex: Thriller 25 Super Deluxe Edition, Album Name 2023 Remaster, Album Deluxe Edition
    private static final Pattern TRAILING_EDITION_PATTERN = Pattern.compile(
            "(?i)\\s+(?:(?:19|20)\\d{2}\\s+)?(?:(?:the|a|an)\\s+)?(?:\\d+(?:st|nd|rd|th)?\\s+)?(?:" +
            "anniversary(?:\\s+edition)?|deluxe(?:\\s+edition|\\s+version)?|" +
            "expanded(?:\\s+edition|\\s+version)?|super\\s+deluxe(?:\\s+edition|\\s+version)?|" +
            "remaster(?:ed)?(?:\\s+version)?|special\\s+edition|collector'?s?\\s+edition|" +
            "(?:[a-z0-9\\-']+\\s+)?(?:edition|version)|bonus\\s+track(?:s)?|bonus\\s+version)\\s*$"
    );

    // 4. Caracteres que quebram a sintaxe de pesquisa P2P no Soulseek
    private static final Pattern SLSK_UNSAFE_CHARS = Pattern.compile("[\\(\\)\\[\\]\\{\\}\"\'`\\/\\\\:;,!?*~]");
    private static final Pattern MULTI_SPACES = Pattern.compile("\\s{2,}");

    private StringNormalizer() {
        // Utility class
    }

    public static String normalize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return withoutDiacritics.toLowerCase().trim();
    }

    /**
     * Limpa nomes de álbuns retirando edições comemorativas, menções de remaster, deluxe, etc.
     * Exemplo:
     *  "Paranoid (Remastered 2020)" -> "Paranoid"
     *  "The Dark Side of the Moon (50th Anniversary Remaster)" -> "The Dark Side of the Moon"
     *  "Abbey Road - 2019 Mix" -> "Abbey Road"
     *  "Bob Marley Legend [Deluxe Edition] (CD1)" -> "Bob Marley Legend"
     */
    public static String cleanAlbumTitle(String album) {
        if (album == null || album.isBlank()) {
            return "";
        }

        String cleaned = album;
        cleaned = BRACKET_EDITION_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = HYPHEN_EDITION_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = TRAILING_EDITION_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replaceAll("[\\s\\-–—:/]+$", "").trim();

        if (cleaned.isBlank()) {
            return album.trim();
        }
        return cleaned;
    }

    /**
     * Limpa títulos de faixas retirando sufixos de remaster, radio edit, etc.
     * Exemplo:
     *  "Smells Like Teen Spirit - Remastered" -> "Smells Like Teen Spirit"
     *  "Iron Man - 2014 Remaster" -> "Iron Man"
     *  "Karma Police (Remastered)" -> "Karma Police"
     */
    public static String cleanTrackTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }

        String cleaned = title;
        cleaned = BRACKET_EDITION_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = HYPHEN_EDITION_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = TRAILING_EDITION_PATTERN.matcher(cleaned).replaceAll("");
        cleaned = cleaned.replaceAll("[\\s\\-–—:/]+$", "").trim();

        if (cleaned.isBlank()) {
            return title.trim();
        }
        return cleaned;
    }

    /**
     * Limpa o nome do artista para evitar que quebre pesquisas Soulseek (ex: AC/DC -> AC DC).
     */
    public static String cleanArtist(String artist) {
        if (artist == null || artist.isBlank()) {
            return "";
        }
        String clean = artist.replace("/", " ").replace("\\", " ");
        clean = SLSK_UNSAFE_CHARS.matcher(clean).replaceAll(" ");
        return MULTI_SPACES.matcher(clean).replaceAll(" ").trim();
    }

    /**
     * Constrói uma query otimizada e limpa para o Soulseek (slskd).
     */
    public static String buildSearchQuery(String artist, String titleOrAlbum, boolean isAlbum, boolean includeFlac) {
        String cleanArt = cleanArtist(artist);
        String cleanItem = isAlbum ? cleanAlbumTitle(titleOrAlbum) : cleanTrackTitle(titleOrAlbum);

        String safeItem = SLSK_UNSAFE_CHARS.matcher(cleanItem).replaceAll(" ");
        safeItem = MULTI_SPACES.matcher(safeItem).replaceAll(" ").trim();

        StringBuilder query = new StringBuilder();
        if (!cleanArt.isBlank()) {
            query.append(cleanArt).append(" ");
        }
        if (!safeItem.isBlank()) {
            query.append(safeItem);
        }
        if (includeFlac) {
            query.append(" flac");
        }

        return MULTI_SPACES.matcher(query.toString()).replaceAll(" ").trim();
    }

    /**
     * Sanitiza uma query de pesquisa genérica (se já tiver sido fornecida como string completa).
     */
    public static String cleanSearchQuery(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return "";
        }
        boolean hasFlac = rawQuery.toLowerCase().endsWith(" flac");
        String base = hasFlac ? rawQuery.substring(0, rawQuery.length() - 5).trim() : rawQuery;

        String cleaned = cleanAlbumTitle(base);
        cleaned = SLSK_UNSAFE_CHARS.matcher(cleaned).replaceAll(" ");
        cleaned = MULTI_SPACES.matcher(cleaned).replaceAll(" ").trim();

        if (hasFlac) {
            cleaned = cleaned + " flac";
        }
        return cleaned;
    }
}
