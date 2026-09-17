package com.soundvault.model;

public record Album(
    String id,
    String name,
    String artist,
    int totalTracks,
    String imageUrl,
    String releaseDate
) {
}
