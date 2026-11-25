package com.gestorgastos.gestor_gastos.service.telegram;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
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

    private static final Logger log = LoggerFactory.getLogger(TelegramRateLimiter.class);
    private static final int MAX_PER_CHAT_PER_MINUTE = 20;
    private static final int MAX_GLOBAL_PER_MINUTE = 120;

    private final Clock clock;
    private final Deque<Instant> globalWindow = new ConcurrentLinkedDeque<>();
    private final Map<Long, Deque<Instant>> chatWindows = new ConcurrentHashMap<>();
    private final Map<Long, Deque<Instant>> linkingFailures = new ConcurrentHashMap<>();

    public TelegramRateLimiter() {
        this(Clock.systemUTC());
    }

    public TelegramRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean allowLinkingAttempt(long telegramUserId) {
        Instant cutoff = clock.instant().minusSeconds(900);
        Deque<Instant> failures = linkingFailures.computeIfAbsent(telegramUserId, k -> new ConcurrentLinkedDeque<>());
        prune(failures, cutoff);
        return failures.size() < 5;
    }

    public void recordLinkingFailure(long telegramUserId) {
        linkingFailures.computeIfAbsent(telegramUserId, k -> new ConcurrentLinkedDeque<>()).addLast(clock.instant());
    }

    public synchronized boolean allow(long chatId) {
        Instant now = clock.instant();
        Instant cutoff = now.minusSeconds(60);

        int expiredGlobal = prune(globalWindow, cutoff);
        if (expiredGlobal > 0) {
            log.debug("TG_TELEMETRY event=TELEGRAM_RATE_LIMIT_EXPIRED_ENTRIES scope=GLOBAL expired={}", expiredGlobal);
        }
        if (globalWindow.size() >= MAX_GLOBAL_PER_MINUTE) {
            long waitSec = remainingSeconds(globalWindow.peekFirst(), now);
            log.warn("TG_TELEMETRY event=TELEGRAM_RATE_LIMIT_GLOBAL globalCount={} retryAfterSec={}",
                    globalWindow.size(), waitSec);
            return false;
        }

        Deque<Instant> chatDeque = chatWindows.computeIfAbsent(chatId, k -> new ConcurrentLinkedDeque<>());
        int expiredChat = prune(chatDeque, cutoff);
        if (expiredChat > 0) {
            log.debug("TG_TELEMETRY event=TELEGRAM_RATE_LIMIT_EXPIRED_ENTRIES scope=CHAT expired={}", expiredChat);
        }
        if (chatDeque.size() >= MAX_PER_CHAT_PER_MINUTE) {
            long waitSec = remainingSeconds(chatDeque.peekFirst(), now);
            log.warn("TG_TELEMETRY event=TELEGRAM_RATE_LIMIT_CHAT chatCount={} globalCount={} retryAfterSec={}",
                    chatDeque.size(), globalWindow.size(), waitSec);
            return false;
        }

        globalWindow.addLast(now);
        chatDeque.addLast(now);
        log.debug("TG_TELEMETRY event=TELEGRAM_RATE_LIMIT_ALLOWED chatCount={} globalCount={}",
                chatDeque.size(), globalWindow.size());
        return true;
    }

    private long remainingSeconds(Instant oldest, Instant now) {
        if (oldest == null) {
            return 0L;
        }
        long elapsed = Duration.between(oldest, now).getSeconds();
        return Math.max(1L, 60L - elapsed);
    }

    private int prune(Deque<Instant> deque, Instant cutoff) {
        int removed = 0;
        while (!deque.isEmpty() && deque.peekFirst().isBefore(cutoff)) {
            deque.pollFirst();
            removed++;
        }
        return removed;
    }
}
