package com.soundvault.model;

public record SearchHistoryItem(
    String id,
    String searchText,
    String state,
    int responseCount
) {
}
