package com.soundvault.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public final class AppLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final int MAX_HISTORY = 1500;
    private static final Deque<String> HISTORY = new ArrayDeque<>(MAX_HISTORY);
    private static final List<Consumer<String>> LISTENERS = new CopyOnWriteArrayList<>();

    private AppLogger() {
        // Utility class
    }

    public static synchronized void log(String message) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String formatted = "[" + timestamp + "] " + message;

        System.out.println(formatted);

        if (HISTORY.size() >= MAX_HISTORY) {
            HISTORY.pollFirst();
        }
        HISTORY.addLast(formatted);

        for (Consumer<String> listener : LISTENERS) {
            try {
                listener.accept(formatted);
            } catch (Exception ignored) {
            }
        }
    }

    public static void addListener(Consumer<String> listener) {
        LISTENERS.add(listener);
    }

    public static void removeListener(Consumer<String> listener) {
        LISTENERS.remove(listener);
    }

    public static synchronized List<String> getHistory() {
        return new ArrayList<>(HISTORY);
    }
}
