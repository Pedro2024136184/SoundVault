package com.soundvault;

import com.soundvault.config.AppConfig;
import com.soundvault.service.SpotifyService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SpotifyAuthTest {

    @Test
    void testCleanQuotes() {
        assertEquals("abc", AppConfig.cleanValue("'abc'"));
        assertEquals("abc", AppConfig.cleanValue("\"abc\""));
        assertEquals("abc", AppConfig.cleanValue("  'abc'  "));
    }

    @Test
    void testSpotifyTokenRefresh() {
        AppConfig config = AppConfig.getInstance();
        config.load();
        if (config.getSpotifyClientId().isBlank() || config.getSpotifyClientSecret().isBlank()) {
            org.junit.jupiter.api.Assumptions.assumeTrue(false, "Spotify API credentials não configuradas no .env - ignorando teste de integração.");
            return;
        }

        SpotifyService service = new SpotifyService();
        boolean authenticated = service.authenticate();
        org.junit.jupiter.api.Assumptions.assumeTrue(authenticated, "Autenticação Spotify indisponível no ambiente de teste - ignorando.");

        var playlists = service.getUserPlaylists();
        System.out.println("User playlists count: " + playlists.size());
        assertFalse(playlists.isEmpty(), "Deverá encontrar pelo menos uma playlist do utilizador!");

        // Test a playlist owned by user
        String ownedId = "";
        String ownedName = "";
        for (var pl : playlists) {
            if ("trap".equalsIgnoreCase(pl.get("name")) || "vocals".equalsIgnoreCase(pl.get("name"))) {
                ownedId = pl.get("id");
                ownedName = pl.get("name");
                break;
            }
        }
        if (ownedId.isBlank()) {
            ownedId = playlists.get(0).get("id");
            ownedName = playlists.get(0).get("name");
        }

        try {
            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                .uri(java.net.URI.create("https://api.spotify.com/v1/playlists/1dcB9CKVw3HmfORCIDAwtY"))
                .header("Authorization", "Bearer " + service.getAccessToken())
                .GET().build();
            var resp = client.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
            System.out.println("Direct GET /playlists/1dcB9CKVw3HmfORCIDAwtY STATUS: " + resp.statusCode() + " | Body: " + resp.body());
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("A testar getPlaylistTracks para playlist: " + ownedName + " (" + ownedId + ")");
        var tracks = service.getPlaylistTracks(ownedId);
        System.out.println("Faixas carregadas na playlist: " + tracks.size());
        assertFalse(tracks.isEmpty(), "A playlist " + ownedName + " deve conter faixas!");
        System.out.println("Primeira faixa: " + tracks.get(0).title() + " - " + tracks.get(0).artist());

        // Test Audio Analysis & Genres
        var topGenres = service.getUserTopGenres();
        System.out.println("Top Géneros detectados: " + topGenres);
        assertNotNull(topGenres);
        assertFalse(topGenres.isEmpty());

        var analysis = service.getAverageAudioFeatures(java.util.List.of("dummy-id"));
        System.out.println("Perfil Psicoacústico: Energia=" + analysis.energy() + ", Valence=" + analysis.valence() + ", Dance=" + analysis.danceability() + ", Acoustic=" + analysis.acousticness() + ", Instrumental=" + analysis.instrumentalness());
        assertTrue(analysis.energy() > 0, "Energia deve ser superior a 0");
        assertTrue(analysis.valence() > 0, "Valência deve ser superior a 0");
        assertTrue(analysis.danceability() > 0, "Dançabilidade deve ser superior a 0");
    }
}
