package com.soundvault.model;

public record SoulseekFile(
    String filename,
    long size,
    int bitRate,
    int length
) {
    public String getCleanFileName() {
        if (filename == null) return "";
        String norm = filename.replace('/', '\\');
        int lastSlash = norm.lastIndexOf('\\');
        return lastSlash >= 0 ? norm.substring(lastSlash + 1) : norm;
    }
}
