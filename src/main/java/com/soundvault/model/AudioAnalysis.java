package com.soundvault.model;

public record AudioAnalysis(
    double energy,
    double valence,
    double danceability,
    double acousticness,
    double instrumentalness
) {
}
