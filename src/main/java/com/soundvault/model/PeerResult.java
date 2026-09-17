package com.soundvault.model;

import java.util.List;

public record PeerResult(
    String username,
    String directory,
    List<SoulseekFile> files,
    double uploadSpeed,
    boolean hasFreeSlots
) {
    public int getTrackCount() {
        return files != null ? files.size() : 0;
    }

    public String getCleanDirectoryName() {
        if (directory == null || directory.isBlank()) return "Unknown";
        String normalized = directory.replace('\\', '/');
        int lastSlash = normalized.lastIndexOf('/');
        return lastSlash >= 0 ? normalized.substring(lastSlash + 1) : normalized;
    }
}
