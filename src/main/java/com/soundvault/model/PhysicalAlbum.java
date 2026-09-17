package com.soundvault.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PhysicalAlbum(
        String id,
        String artist,
        String title,
        String format,         // e.g. "CD", "Vinil", "Cassete"
        String imageUrl,       // URL da capa no Spotify
        String spotifyId,      // ID do Spotify se associado
        String releaseYear,    // Ano de lançamento
        int totalTracks,       // Número de faixas
        String notes,          // Observações personalizadas (ex: Digipak, Duplo)
        long dateAdded         // Timestamp em milissegundos
) {
    public PhysicalAlbum {
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
        }
        if (format == null || format.isBlank()) {
            format = "CD";
        }
        if (notes == null) {
            notes = "";
        }
        if (dateAdded == 0) {
            dateAdded = System.currentTimeMillis();
        }
    }

    public static PhysicalAlbum of(String artist, String title) {
        return new PhysicalAlbum(UUID.randomUUID().toString(), artist, title, "CD", "", "", "", 0, "", System.currentTimeMillis());
    }

    public static PhysicalAlbum of(String artist, String title, String format, String notes) {
        return new PhysicalAlbum(UUID.randomUUID().toString(), artist, title, format, "", "", "", 0, notes, System.currentTimeMillis());
    }

    public PhysicalAlbum withSpotifyDetails(String imageUrl, String spotifyId, String releaseYear, int totalTracks) {
        return new PhysicalAlbum(this.id, this.artist, this.title, this.format, imageUrl, spotifyId, releaseYear, totalTracks, this.notes, this.dateAdded);
    }
}
