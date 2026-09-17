package com.soundvault.model;

public record Artist(
    String id,
    String name,
    String imageUrl,
    int localAlbumCount
) {
    public Artist withLocalCount(int count) {
        return new Artist(id, name, imageUrl, count);
    }
}
