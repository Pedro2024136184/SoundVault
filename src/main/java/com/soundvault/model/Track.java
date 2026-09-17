package com.soundvault.model;

public record Track(
    String id,
    String title,
    String artist,
    String album,
    long durationMs,
    String imageUrl
) {
    public String getDisplayName() {
        return title + " - " + artist;
    }
}
