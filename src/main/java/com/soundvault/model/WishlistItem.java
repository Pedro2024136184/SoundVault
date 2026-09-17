package com.soundvault.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WishlistItem(
        String id,
        String artist,
        String title,
        String format,         // e.g. "CD", "Vinil", "Cassete"
        String imageUrl,       // URL da capa oficial (Spotify)
        String spotifyId,      // ID do Spotify
        String releaseYear,    // Ano de lançamento
        String notes,          // Observações (ex: "Prensagem japonesa", "Prioridade alta")
        boolean acquired,      // Se já foi comprado/adquirido
        long dateAdded         // Timestamp em milissegundos
) {
    public WishlistItem {
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

    public static WishlistItem of(String artist, String title, String format, String notes, String imageUrl, String spotifyId, String year) {
        return new WishlistItem(
                UUID.randomUUID().toString(),
                artist,
                title,
                format,
                imageUrl != null ? imageUrl : "",
                spotifyId != null ? spotifyId : "",
                year != null ? year : "",
                notes != null ? notes : "",
                false,
                System.currentTimeMillis()
        );
    }

    public WishlistItem withAcquired(boolean acquired) {
        return new WishlistItem(this.id, this.artist, this.title, this.format, this.imageUrl, this.spotifyId, this.releaseYear, this.notes, acquired, this.dateAdded);
    }

    public WishlistItem withSpotifyDetails(String imageUrl, String spotifyId, String releaseYear) {
        return new WishlistItem(this.id, this.artist, this.title, this.format, imageUrl, spotifyId, releaseYear, this.notes, this.acquired, this.dateAdded);
    }
}
