package com.gestorgastos.gestor_gastos.service.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Servicio de Long Polling controlado para Telegram Bot API (B4).
 * Solo inicia si TELEGRAM_ENABLED=true y existe botToken valido.
 */
@Service
public class TelegramPollingService implements SmartLifecycle {

    private final TelegramBotClient botClient;
    private final TelegramUpdateRouter updateRouter;

    @Value("${nexus.telegram.poll-timeout:25}")
    private int pollTimeoutSeconds;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;
    private long nextOffset = 0L;

    public TelegramPollingService(
            TelegramBotClient botClient,
            TelegramUpdateRouter updateRouter) {
        this.botClient = botClient;
        this.updateRouter = updateRouter;
    }

    @Override
    public void start() {
        if (!botClient.isConfigured()) {
            return;
        }
        if (running.compareAndSet(false, true)) {
            workerThread = new Thread(this::pollLoop, "nexus-telegram-poller");
            workerThread.setDaemon(true);
            workerThread.start();
        }
    }

    private void pollLoop() {
        long backoffMs = 2000L;
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            TelegramBotClient.PollResult result = botClient.getUpdates(nextOffset, pollTimeoutSeconds);
            if (result.ok()) {
                backoffMs = 2000L;
                for (JsonNode update : result.updates()) {
                    long updateId = update.path("update_id").asLong(0);
                    try {
                        updateRouter.procesarUpdate(update);
                        if (updateId >= nextOffset) {
                            nextOffset = updateId + 1;
                        }
                    } catch (Exception ex) {
                        // No avanzar offset si falla el procesamiento critico; backoff breve
                        sleepQuietly(1000L);
                    }
                }
            } else {
                int status = result.httpStatus();
                if (status == 401 || status == 403 || status == 409) {
                    // Token invalido (401/403) o conflicto de webhook existente (409): detener sin destruir webhook
                    running.set(false);
                    break;
                }
                long waitMs = (status == 429 && result.retryAfterSeconds() > 0)
                        ? result.retryAfterSeconds() * 1000L
                        : backoffMs;
                sleepQuietly(waitMs);
                backoffMs = Math.min(backoffMs * 2, 30000L);
            }
        }
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void stop() {
        running.set(false);
        if (workerThread != null) {
            workerThread.interrupt();
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }
}
