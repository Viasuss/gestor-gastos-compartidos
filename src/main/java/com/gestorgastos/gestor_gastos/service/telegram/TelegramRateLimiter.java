package com.gestorgastos.gestor_gastos.service.telegram;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Limitador conservador de solicitudes entrantes de Telegram por chat y global (B4).
 */
@Component
public class TelegramRateLimiter {

    private static final int MAX_PER_CHAT_PER_MINUTE = 20;
    private static final int MAX_GLOBAL_PER_MINUTE = 120;

    private final Deque<Instant> globalWindow = new ConcurrentLinkedDeque<>();
    private final Map<Long, Deque<Instant>> chatWindows = new ConcurrentHashMap<>();

    public boolean allow(long chatId) {
        Instant now = Instant.now();
        Instant cutoff = now.minusSeconds(60);

        prune(globalWindow, cutoff);
        if (globalWindow.size() >= MAX_GLOBAL_PER_MINUTE) {
            return false;
        }

        Deque<Instant> chatDeque = chatWindows.computeIfAbsent(chatId, k -> new ConcurrentLinkedDeque<>());
        prune(chatDeque, cutoff);
        if (chatDeque.size() >= MAX_PER_CHAT_PER_MINUTE) {
            return false;
        }

        globalWindow.addLast(now);
        chatDeque.addLast(now);
        return true;
    }

    private void prune(Deque<Instant> deque, Instant cutoff) {
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
        }
    }
}
