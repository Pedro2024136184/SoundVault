package com.soundvault.model;

public record DownloadTransfer(
    String username,
    String filename,
    String state,
    long sizeBytes,
    double progressPercent,
    double averageSpeed,
    long bytesTransferred
) {
    public DownloadTransfer(String username, String filename, String state, long sizeBytes, double progressPercent) {
        this(username, filename, state, sizeBytes, progressPercent, 0, 0);
    }

    public String getCleanFileName() {
        if (filename == null) return "Unknown";
        String norm = filename.replace('\\', '/');
        int lastSlash = norm.lastIndexOf('/');
        return lastSlash >= 0 ? norm.substring(lastSlash + 1) : norm;
    }

    public String getDirectoryName() {
        if (filename == null) return "";
        String norm = filename.replace('/', '\\');
        int lastSlash = norm.lastIndexOf('\\');
        if (lastSlash < 0) return "";
        String parent = norm.substring(0, lastSlash);
        int prevSlash = parent.lastIndexOf('\\');
        return prevSlash >= 0 ? parent.substring(prevSlash + 1) : parent;
    }

    public String getFormattedSpeed() {
        if (averageSpeed <= 0) return "0 KB/s";
        if (averageSpeed >= 1_000_000) {
            return String.format(java.util.Locale.US, "%.1f MB/s", averageSpeed / 1_000_000.0);
        }
        return String.format(java.util.Locale.US, "%.0f KB/s", averageSpeed / 1000.0);
    }

    public String getFormattedProgress() {
        return String.format(java.util.Locale.US, "%.0f%%", Math.min(100.0, Math.max(0.0, progressPercent)));
    }

    public String getFormattedSize() {
        if (sizeBytes >= 1_000_000_000) {
            return String.format(java.util.Locale.US, "%.2f GB", sizeBytes / 1_000_000_000.0);
        }
        return String.format(java.util.Locale.US, "%.1f MB", sizeBytes / 1_000_000.0);
    }

    public boolean isCompleted() {
        if (state == null) return false;
        String s = state.toLowerCase();
        return s.contains("completed") || s.contains("succeeded");
    }

    public boolean isInProgress() {
        if (state == null) return false;
        return state.equalsIgnoreCase("InProgress");
    }

    public boolean isQueued() {
        if (state == null) return false;
        String s = state.toLowerCase();
        return s.contains("queue") || s.contains("requested") || s.contains("initializing");
    }

    public boolean isErrored() {
        if (state == null) return false;
        String s = state.toLowerCase();
        return s.contains("error") || s.contains("cancel") || s.contains("timedout") || s.contains("rejected");
    }
}
