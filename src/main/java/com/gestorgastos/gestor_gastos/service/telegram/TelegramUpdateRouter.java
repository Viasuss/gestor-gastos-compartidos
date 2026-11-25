package com.gestorgastos.gestor_gastos.service.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.gestorgastos.gestor_gastos.entity.*;
import com.gestorgastos.gestor_gastos.repository.*;
import com.gestorgastos.gestor_gastos.security.GrupoAccessService;
import com.gestorgastos.gestor_gastos.service.*;
import com.gestorgastos.gestor_gastos.service.telegram.TelegramBotClient.InlineButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class TelegramUpdateRouter {

    private static final Logger log = LoggerFactory.getLogger(TelegramUpdateRouter.class);
    private final TelegramBotClient botClient;
    private final TelegramRateLimiter rateLimiter;
    private final TelegramAccountLinkService linkService;
    private final TelegramGroupBindingService groupBindingService;
    private final TelegramUpdateProcesadoRepository updateProcesadoRepo;
    private final CuentaCanalRepository cuentaCanalRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final BorradorOperacionRepository borradorRepository;
    private final BorradorOperacionService borradorService;
    private final GrupoAccessService grupoAccessService;
    private final DeudaService deudaService;

    public TelegramUpdateRouter(
            TelegramBotClient botClient,
            TelegramRateLimiter rateLimiter,
            TelegramAccountLinkService linkService,
            TelegramGroupBindingService groupBindingService,
            TelegramUpdateProcesadoRepository updateProcesadoRepo,
            CuentaCanalRepository cuentaCanalRepository,
            MiembroGrupoRepository miembroGrupoRepository,
            BorradorOperacionRepository borradorRepository,
            BorradorOperacionService borradorService,
            GrupoAccessService grupoAccessService,
            DeudaService deudaService) {
        this.botClient = botClient;
        this.rateLimiter = rateLimiter;
        this.linkService = linkService;
        this.groupBindingService = groupBindingService;
        this.updateProcesadoRepo = updateProcesadoRepo;
        this.cuentaCanalRepository = cuentaCanalRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.borradorRepository = borradorRepository;
        this.borradorService = borradorService;
        this.grupoAccessService = grupoAccessService;
        this.deudaService = deudaService;
    }

    @Transactional
    public void procesarUpdate(JsonNode update) {
        if (update == null || !update.has("update_id")) {
            return;
        }
        long updateId = update.get("update_id").asLong();
        if (updateProcesadoRepo.existsById(updateId)) {
            log.info("TG_TELEMETRY event=TELEGRAM_UPDATE_DUPLICATE updateId={}", updateId);
            return;
        }

        if (update.has("callback_query")) {
            manejarCallback(update.get("callback_query"));
        } else if (update.has("message")) {
            manejarMensaje(update.get("message"), updateId);
        }

        updateProcesadoRepo.save(new TelegramUpdateProcesado(updateId, LocalDateTime.now()));
        log.info("TG_TELEMETRY event=TELEGRAM_UPDATE_PROCESSED updateId={}", updateId);
    }

    private void manejarMensaje(JsonNode message, long updateId) {
        long chatId = message.path("chat").path("id").asLong(0);
        String chatType = message.path("chat").path("type").asText("");
        long telegramUserId = message.path("from").path("id").asLong(0);
        String text = message.path("text").asText("").trim();

        if (chatId == 0 || telegramUserId == 0 || text.isEmpty()) {
            return;
        }
        if (!rateLimiter.allow(chatId)) {
            botClient.sendMessage(chatId, "Has enviado demasiadas solicitudes. Espera un momento.", null);
            return;
        }

        boolean esPrivado = "private".equals(chatType);

        if (!esPrivado) {
            manejarMensajeGrupal(chatId, telegramUserId, text, updateId);
            return;
        }

        // Comandos publicos / vinculacion en chat privado
        if (text.startsWith("/start")) {
            String[] parts = text.split("\\s+", 2);
            if (parts.length == 2 && !parts[1].isBlank()) {
                vincularYResponder(chatId, telegramUserId, parts[1].trim());
            } else {
                botClient.sendMessage(chatId,
                        "Bienvenido a NEXUS.\nUsa /vincular <CODIGO> con el código generado en la web, o /ayuda para ver los comandos.",
                        null);
            }
            return;
        }

        if (text.startsWith("/ayuda")) {
            botClient.sendMessage(chatId,
                    "Comandos disponibles:\n/vincular <CODIGO>\n/grupos\n/grupo <ID>\n/gasto <descripción>\n/saldo\n/deudas\n/pendientes\n/desvincular",
                    null);
            return;
        }

        if (text.startsWith("/vincular")) {
            String[] parts = text.split("\\s+", 2);
            if (parts.length < 2 || parts[1].isBlank()) {
                botClient.sendMessage(chatId, "Uso: /vincular <CODIGO>", null);
            } else {
                vincularYResponder(chatId, telegramUserId, parts[1].trim());
            }
            return;
        }

        CuentaCanal cuenta = linkService.obtenerCuentaActivaPorTelegramId(telegramUserId).orElse(null);
        if (cuenta == null) {
            botClient.sendMessage(chatId,
                    "Tu cuenta aún no está vinculada a NEXUS. Genera un código en la web y envía /vincular <CODIGO>.",
                    null);
            return;
        }

        if (text.equals("/desvincular")) {
            linkService.desvincularUsuario(cuenta.getUsuario());
            botClient.sendMessage(chatId, "Cuenta desvinculada de NEXUS.", null);
            return;
        }

        if (text.equals("/grupos")) {
            enviarListaGrupos(chatId, cuenta.getUsuario());
            return;
        }

        if (text.startsWith("/grupo")) {
            String[] parts = text.split("\\s+", 2);
            if (parts.length < 2) {
                botClient.sendMessage(chatId, "Uso: /grupo <ID>", null);
                return;
            }
            try {
                Long grupoId = Long.parseLong(parts[1].trim());
                seleccionarGrupo(chatId, cuenta, grupoId);
            } catch (NumberFormatException e) {
                botClient.sendMessage(chatId, "ID de grupo inválido.", null);
            }
            return;
        }

        if (text.equals("/saldo") || text.equals("/deudas")) {
            enviarResumenSaldos(chatId, cuenta.getUsuario());
            return;
        }

        if (text.equals("/pendientes")) {
            enviarPendientes(chatId, cuenta.getUsuario());
            return;
        }

        String instruccion = text.startsWith("/gasto")
                ? text.substring("/gasto".length()).trim()
                : (text.startsWith("/") ? "" : text);

        if (instruccion.isBlank()) {
            botClient.sendMessage(chatId, "Comando no reconocido o vacío. Usa /ayuda.", null);
            return;
        }

        if (cuenta.getGrupoActivoId() == null) {
            botClient.sendMessage(chatId, "Primero selecciona un grupo con /grupos.", null);
            return;
        }

        crearYEnviarBorrador(chatId, cuenta.getUsuario(), cuenta.getGrupoActivoId(), instruccion, updateId);
    }

    private void manejarMensajeGrupal(long chatId, long telegramUserId, String text, long updateId) {
        if (text.startsWith("/vincular ")) {
            botClient.sendMessage(chatId, "Por seguridad, los códigos de vinculación solo se aceptan en chat privado.", null);
            return;
        }
        if (text.startsWith("/saldo") || text.startsWith("/deudas") || text.startsWith("/pendientes")) {
            botClient.sendMessage(chatId, "Por privacidad, consulta tus saldos personales en el chat privado con el bot.", null);
            return;
        }
        if (text.startsWith("/vincular_grupo")) {
            String[] parts = text.split("\\s+", 2);
            if (parts.length < 2) {
                botClient.sendMessage(chatId, "Uso: /vincular_grupo <ID_GRUPO_NEXUS>", null);
                return;
            }
            try {
                Long grupoId = Long.parseLong(parts[1].trim().split("@")[0]);
                botClient.sendMessage(chatId, groupBindingService.vincularChat(chatId, telegramUserId, grupoId), null);
            } catch (NumberFormatException e) {
                botClient.sendMessage(chatId, "ID de grupo inválido.", null);
            }
            return;
        }
        if (text.startsWith("/gasto")) {
            String instruccion = text.replaceFirst("^/gasto(?:@\\S+)?", "").trim();
            if (instruccion.isEmpty()) {
                botClient.sendMessage(chatId, "Uso: /gasto <descripción y monto>", null);
                return;
            }
            CuentaCanal cuenta = linkService.obtenerCuentaActivaPorTelegramId(telegramUserId).orElse(null);
            if (cuenta == null) {
                botClient.sendMessage(chatId, "Vincula tu cuenta en chat privado antes de registrar gastos.", null);
                return;
            }
            VinculacionChatGrupo vinc = groupBindingService.obtenerVinculacionActiva(chatId).orElse(null);
            if (vinc == null) {
                botClient.sendMessage(chatId, "Este chat no está vinculado a ningún grupo NEXUS.", null);
                return;
            }
            crearYEnviarBorrador(chatId, cuenta.getUsuario(), vinc.getGrupo().getId(), instruccion, updateId);
        }
    }

    private void manejarCallback(JsonNode callback) {
        String callbackId = callback.path("id").asText("");
        long chatId = callback.path("message").path("chat").path("id").asLong(0);
        long telegramUserId = callback.path("from").path("id").asLong(0);
        String data = callback.path("data").asText("");

        CuentaCanal cuenta = linkService.obtenerCuentaActivaPorTelegramId(telegramUserId).orElse(null);
        if (cuenta == null) {
            botClient.answerCallbackQuery(callbackId, "Cuenta no vinculada.");
            return;
        }

        if (data.startsWith("GRP:")) {
            try {
                Long grupoId = Long.parseLong(data.substring(4));
                seleccionarGrupo(chatId, cuenta, grupoId);
                botClient.answerCallbackQuery(callbackId, "Grupo seleccionado.");
            } catch (NumberFormatException e) {
                botClient.answerCallbackQuery(callbackId, "Grupo inválido.");
            }
            return;
        }

        if (data.startsWith("CONF:")) {
            try {
                Long borradorId = Long.parseLong(data.substring(5));
                BorradorOperacion b = borradorRepository.findByIdAndUsuarioId(borradorId, cuenta.getUsuario().getId())
                        .orElse(null);
                if (b == null) {
                    botClient.answerCallbackQuery(callbackId, "No autorizado sobre este borrador.");
                    return;
                }
                if (b.getEstado() == EstadoBorrador.REQUIERE_ACLARACION || b.getMontoCop() == null) {
                    botClient.answerCallbackQuery(callbackId, "Completa los participantes o monto en NEXUS Web.");
                    botClient.sendMessage(chatId, "Este borrador requiere aclaración. Complétalo en la Bandeja Web de NEXUS.", null);
                    return;
                }
                List<Long> participantes = miembroGrupoRepository
                        .findByGrupoIdAndEstado(b.getGrupo().getId(), EstadoMiembro.MIEMBRO)
                        .stream().map(m -> m.getUsuario().getId()).toList();
                Gasto gasto = borradorService.confirmarBorrador(
                        borradorId, cuenta.getUsuario(), b.getConcepto(), b.getMontoCop(),
                        b.getFechaSugerida() != null ? b.getFechaSugerida() : LocalDate.now(),
                        cuenta.getUsuario().getId(), participantes, "borrador-" + borradorId);
                botClient.answerCallbackQuery(callbackId, "Gasto confirmado.");
                botClient.sendMessage(chatId, "Gasto confirmado: " + gasto.getDescripcion() + " ($" + gasto.getMonto().toPlainString() + ").", null);
            } catch (Exception ex) {
                botClient.answerCallbackQuery(callbackId, ex.getMessage());
            }
            return;
        }

        if (data.startsWith("DESC:")) {
            try {
                Long borradorId = Long.parseLong(data.substring(5));
                borradorService.descartarBorrador(borradorId, cuenta.getUsuario());
                botClient.answerCallbackQuery(callbackId, "Borrador descartado.");
                botClient.sendMessage(chatId, "Borrador #" + borradorId + " descartado.", null);
            } catch (Exception ex) {
                botClient.answerCallbackQuery(callbackId, ex.getMessage());
            }
        }
    }

    private void vincularYResponder(long chatId, long telegramUserId, String codigo) {
        if (!rateLimiter.allowLinkingAttempt(telegramUserId)) {
            botClient.sendMessage(chatId, "Demasiados intentos fallidos de vinculación. Espera unos minutos.", null);
            return;
        }
        Optional<CuentaCanal> res = linkService.vincularPorCodigo(telegramUserId, codigo);
        if (res.isPresent()) {
            botClient.sendMessage(chatId,
                    "Cuenta vinculada correctamente a " + res.get().getUsuario().getName() + ". Usa /grupos para comenzar.",
                    null);
        } else {
            rateLimiter.recordLinkingFailure(telegramUserId);
            botClient.sendMessage(chatId, "Código inválido, expirado o ya utilizado.", null);
        }
    }

    private void enviarListaGrupos(long chatId, Usuario usuario) {
        List<MiembroGrupo> grupos = miembroGrupoRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoMiembro.MIEMBRO);
        if (grupos.isEmpty()) {
            botClient.sendMessage(chatId, "No perteneces a ningún grupo activo en NEXUS.", null);
            return;
        }
        StringBuilder sb = new StringBuilder("Tus grupos activos:\n");
        List<List<InlineButton>> keyboard = new ArrayList<>();
        for (MiembroGrupo mg : grupos) {
            sb.append("• ").append(mg.getGrupo().getId()).append(": ").append(mg.getGrupo().getName()).append("\n");
            keyboard.add(List.of(new InlineButton("Seleccionar: " + mg.getGrupo().getName(), "GRP:" + mg.getGrupo().getId())));
        }
        botClient.sendMessage(chatId, sb.toString(), keyboard);
    }

    private void seleccionarGrupo(long chatId, CuentaCanal cuenta, Long grupoId) {
        Grupo g = grupoAccessService.findGrupoSiEsMiembroActivo(grupoId, cuenta.getUsuario()).orElse(null);
        if (g == null) {
            botClient.sendMessage(chatId, "No eres miembro activo de ese grupo.", null);
            return;
        }
        cuenta.setGrupoActivoId(g.getId());
        cuentaCanalRepository.save(cuenta);
        botClient.sendMessage(chatId, "Grupo activo seleccionado: " + g.getName() + ". Ya puedes enviar gastos en lenguaje natural.", null);
    }

    private void crearYEnviarBorrador(long chatId, Usuario usuario, Long grupoId, String texto, long updateId) {
        try {
            BorradorOperacion b = borradorService.crearDesdeTexto(usuario, grupoId, texto, "tg-upd-" + updateId);
            b.setOrigen("TELEGRAM");
            borradorRepository.save(b);

            String montoTxt = b.getMontoCop() != null ? "$" + b.getMontoCop().toPlainString() : "Por definir";
            String msg = "Borrador #" + b.getId() + " (" + b.getEstado() + ")\n"
                    + "Grupo: " + b.getGrupo().getName() + "\n"
                    + "Concepto: " + b.getConcepto() + "\n"
                    + "Monto: " + montoTxt;

            List<List<InlineButton>> kb = List.of(List.of(
                    new InlineButton("✓ Confirmar", "CONF:" + b.getId()),
                    new InlineButton("✕ Descartar", "DESC:" + b.getId())
            ));
            botClient.sendMessage(chatId, msg, kb);
        } catch (IllegalArgumentException ex) {
            botClient.sendMessage(chatId, ex.getMessage(), null);
        }
    }

    private void enviarResumenSaldos(long chatId, Usuario usuario) {
        List<Grupo> grupos = miembroGrupoRepository.findByUsuarioIdAndEstado(usuario.getId(), EstadoMiembro.MIEMBRO)
                .stream().map(MiembroGrupo::getGrupo).toList();
        if (grupos.isEmpty()) {
            botClient.sendMessage(chatId, "No tienes grupos activos.", null);
            return;
        }
        Map<Long, BigDecimal> balances = deudaService.calcularBalancesPorGrupo(grupos, usuario);
        BalanceGeneral general = deudaService.resumirBalances(balances.values());

        StringBuilder sb = new StringBuilder("Balance general NEXUS:\n")
                .append("Te deben: $").append(general.getTotalLeDeben().toPlainString()).append("\n")
                .append("Debes: $").append(general.getTotalDebe().toPlainString()).append("\n\nPor grupo:\n");
        for (Grupo g : grupos) {
            sb.append("• ").append(g.getName()).append(": $").append(balances.get(g.getId()).toPlainString()).append("\n");
        }
        botClient.sendMessage(chatId, sb.toString(), null);
    }

    private void enviarPendientes(long chatId, Usuario usuario) {
        List<BorradorOperacion> pendientes = borradorRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .filter(b -> b.getEstado() == EstadoBorrador.PENDIENTE || b.getEstado() == EstadoBorrador.REQUIERE_ACLARACION)
                .limit(5)
                .toList();
        if (pendientes.isEmpty()) {
            botClient.sendMessage(chatId, "No tienes borradores pendientes.", null);
            return;
        }
        StringBuilder sb = new StringBuilder("Borradores pendientes:\n");
        for (BorradorOperacion b : pendientes) {
            sb.append("#").append(b.getId()).append(" · ").append(b.getConcepto())
                    .append(" ($").append(b.getMontoCop() != null ? b.getMontoCop().toPlainString() : "?").append(")\n");
        }
        botClient.sendMessage(chatId, sb.toString(), null);
    }
}
