package com.clutch.shopmoney.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputService {
    private final Map<UUID, Consumer<String>> waiting = new ConcurrentHashMap<>();

    public void waitFor(UUID uuid, Consumer<String> handler) { waiting.put(uuid, handler); }
    public boolean consume(UUID uuid, String message) {
        Consumer<String> c = waiting.remove(uuid);
        if (c == null) return false;
        c.accept(message);
        return true;
    }
}
