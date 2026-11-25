package com.gestorgastos.gestor_gastos.service.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestorgastos.gestor_gastos.entity.TelegramUpdateProcesado;
import com.gestorgastos.gestor_gastos.repository.*;
import com.gestorgastos.gestor_gastos.security.GrupoAccessService;
import com.gestorgastos.gestor_gastos.service.BorradorOperacionService;
import com.gestorgastos.gestor_gastos.service.DeudaService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TelegramRateLimiterAndPollingTest {

    private static final class MutableClock extends Clock {
        private final AtomicReference<Instant> now;

        MutableClock(Instant initial) {
            this.now = new AtomicReference<>(initial);
        }

        void advanceSeconds(long seconds) {
            now.updateAndGet(i -> i.plusSeconds(seconds));
        }

        @Override
        public ZoneId getZone() {
            return ZoneId.of("UTC");
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now.get();
        }
    }

    @Test
    void permite20MensajesRechazaEl21YRecuperaTrasExpirarVentanaSinProlongarBloqueo() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-25T12:00:00Z"));
        TelegramRateLimiter limiter = new TelegramRateLimiter(clock);

        long chatId = 1001L;
        for (int i = 0; i < 20; i++) {
            assertTrue(limiter.allow(chatId), "Debe permitir el mensaje " + (i + 1));
        }

        // Mensaje 21 rechazado
        assertFalse(limiter.allow(chatId));

        // Intentos rechazados dentro de la ventana no prolongan el bloqueo
        clock.advanceSeconds(30);
        assertFalse(limiter.allow(chatId));
        assertFalse(limiter.allow(chatId));

        // Otro chat es independiente
        assertTrue(limiter.allow(2002L));

        // Al pasar 61s desde el inicio, expira la ventana de los primeros 20
        clock.advanceSeconds(31);
        assertTrue(limiter.allow(chatId), "Debe recuperar acceso al expirar la ventana de 60s");
    }

    @Test
    void saturaYRecuperaLimiteGlobalDe120MensajesPorMinuto() {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-25T12:00:00Z"));
        TelegramRateLimiter limiter = new TelegramRateLimiter(clock);

        for (int i = 0; i < 120; i++) {
            assertTrue(limiter.allow(5000L + (i / 10)));
        }
        assertFalse(limiter.allow(9999L), "El mensaje 121 global debe ser rechazado");

        clock.advanceSeconds(61);
        assertTrue(limiter.allow(9999L), "Debe recuperar tras expirar la ventana global");
    }

    @Test
    void concurrenciaEnRateLimiterNoSuperaLimiteDe20PorChat() throws InterruptedException {
        MutableClock clock = new MutableClock(Instant.parse("2026-11-25T12:00:00Z"));
        TelegramRateLimiter limiter = new TelegramRateLimiter(clock);

        ExecutorService pool = Executors.newFixedThreadPool(8);
        CountDownLatch latch = new CountDownLatch(30);
        AtomicInteger allowed = new AtomicInteger(0);
        for (int i = 0; i < 30; i++) {
            pool.submit(() -> {
                try {
                    if (limiter.allow(777L)) {
                        allowed.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();
        assertEquals(20, allowed.get());
    }

    @Test
    void updatesDuplicadosSeDescartanAntesDeConsumirRateLimiter() throws Exception {
        TelegramUpdateProcesadoRepository updateRepo = mock(TelegramUpdateProcesadoRepository.class);
        when(updateRepo.existsById(42L)).thenReturn(true);

        TelegramRateLimiter spyLimiter = spy(new TelegramRateLimiter());
        TelegramUpdateRouter router = new TelegramUpdateRouter(
                mock(TelegramBotClient.class),
                spyLimiter,
                mock(TelegramAccountLinkService.class),
                mock(TelegramGroupBindingService.class),
                updateRepo,
                mock(CuentaCanalRepository.class),
                mock(MiembroGrupoRepository.class),
                mock(BorradorOperacionRepository.class),
                mock(BorradorOperacionService.class),
                mock(GrupoAccessService.class),
                mock(DeudaService.class)
        );

        JsonNode dup = new ObjectMapper().readTree("{\"update_id\":42,\"message\":{\"chat\":{\"id\":1,\"type\":\"private\"},\"from\":{\"id\":1},\"text\":\"/start\"}}");
        router.procesarUpdate(dup);

        verify(spyLimiter, never()).allow(anyLong());
        verify(updateRepo, never()).save(any(TelegramUpdateProcesado.class));
    }

    @Test
    void pollingAvanzaOffsetTrasTresFallosYProcesaSiguienteUpdateValido() throws Exception {
        TelegramBotClient botClient = mock(TelegramBotClient.class);
        TelegramUpdateRouter router = mock(TelegramUpdateRouter.class);
        TelegramPollingService polling = new TelegramPollingService(botClient, router);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode badUpdate = mapper.readTree("{\"update_id\":100}");
        JsonNode goodUpdate = mapper.readTree("{\"update_id\":101}");

        doThrow(new RuntimeException("Error simulado")).when(router).procesarUpdate(badUpdate);
        doAnswer(inv -> {
            polling.stop();
            return null;
        }).when(router).procesarUpdate(goodUpdate);

        when(botClient.getUpdates(anyLong(), anyInt()))
                .thenReturn(new TelegramBotClient.PollResult(true, 200, 0, java.util.List.of(badUpdate)))
                .thenReturn(new TelegramBotClient.PollResult(true, 200, 0, java.util.List.of(badUpdate)))
                .thenReturn(new TelegramBotClient.PollResult(true, 200, 0, java.util.List.of(badUpdate)))
                .thenReturn(new TelegramBotClient.PollResult(true, 200, 0, java.util.List.of(goodUpdate)));

        java.lang.reflect.Field runningField = TelegramPollingService.class.getDeclaredField("running");
        runningField.setAccessible(true);
        ((java.util.concurrent.atomic.AtomicBoolean) runningField.get(polling)).set(true);

        java.lang.reflect.Method pollLoop = TelegramPollingService.class.getDeclaredMethod("pollLoop");
        pollLoop.setAccessible(true);
        pollLoop.invoke(polling);

        verify(router, times(3)).procesarUpdate(badUpdate);
        verify(router, times(1)).procesarUpdate(goodUpdate);
    }
}
