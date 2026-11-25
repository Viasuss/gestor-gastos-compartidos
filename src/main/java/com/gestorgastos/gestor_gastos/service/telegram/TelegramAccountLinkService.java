package com.gestorgastos.gestor_gastos.service.telegram;

import com.gestorgastos.gestor_gastos.entity.CodigoVinculacion;
import com.gestorgastos.gestor_gastos.entity.CuentaCanal;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.CodigoVinculacionRepository;
import com.gestorgastos.gestor_gastos.repository.CuentaCanalRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

/**
 * Vinculacion segura de cuentas NEXUS <-> Telegram (B4).
 * Tokens aleatorios de un solo uso almacenados como hash SHA-256 con expiracion corta.
 */
@Service
public class TelegramAccountLinkService {

    private static final String CANAL_TELEGRAM = "TELEGRAM";
    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_EXPIRATION_MINUTES = 10;

    private final SecureRandom secureRandom = new SecureRandom();
    private final CodigoVinculacionRepository codigoRepository;
    private final CuentaCanalRepository cuentaCanalRepository;

    public TelegramAccountLinkService(
            CodigoVinculacionRepository codigoRepository,
            CuentaCanalRepository cuentaCanalRepository) {
        this.codigoRepository = codigoRepository;
        this.cuentaCanalRepository = cuentaCanalRepository;
    }

    @Transactional
    public String generarCodigoTemporal(Usuario usuario) {
        // Invalidar codigos previos no usados del usuario
        List<CodigoVinculacion> previos = codigoRepository.findByUsuarioIdAndCanalAndUsadoFalse(usuario.getId(), CANAL_TELEGRAM);
        for (CodigoVinculacion prev : previos) {
            prev.setUsado(true);
            codigoRepository.save(prev);
        }

        String rawCode = "NX-" + randomToken(8);
        CodigoVinculacion cv = new CodigoVinculacion();
        cv.setUsuario(usuario);
        cv.setCanal(CANAL_TELEGRAM);
        cv.setCodigoHash(sha256(rawCode));
        cv.setUsado(false);
        cv.setFechaCreacion(LocalDateTime.now());
        cv.setFechaExpiracion(LocalDateTime.now().plusMinutes(CODE_EXPIRATION_MINUTES));
        codigoRepository.save(cv);

        return rawCode;
    }

    @Transactional
    public Optional<CuentaCanal> vincularPorCodigo(Long telegramUserId, String codigoPlano) {
        if (telegramUserId == null || codigoPlano == null || codigoPlano.isBlank()) {
            return Optional.empty();
        }

        String hash = sha256(codigoPlano.trim().toUpperCase());
        Optional<CodigoVinculacion> cvOpt = codigoRepository.findByCodigoHash(hash);
        if (cvOpt.isEmpty()) {
            return Optional.empty();
        }

        CodigoVinculacion cv = cvOpt.get();
        if (cv.isUsado() || cv.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            return Optional.empty();
        }

        // Impedir duplicidad: si ese telegramUserId ya pertenece a otro usuario activo, rechazar
        Optional<CuentaCanal> existentePorTelegram =
                cuentaCanalRepository.findByCanalAndIdentificadorExternoAndActivaTrue(CANAL_TELEGRAM, telegramUserId);
        if (existentePorTelegram.isPresent()
                && !existentePorTelegram.get().getUsuario().getId().equals(cv.getUsuario().getId())) {
            return Optional.empty();
        }

        cv.setUsado(true);
        codigoRepository.save(cv);

        // Desactivar vinculacion previa del mismo usuario si existia con otro telegramUserId
        cuentaCanalRepository.findByCanalAndUsuarioIdAndActivaTrue(CANAL_TELEGRAM, cv.getUsuario().getId())
                .ifPresent(prev -> {
                    if (!prev.getIdentificadorExterno().equals(telegramUserId)) {
                        prev.setActiva(false);
                        cuentaCanalRepository.save(prev);
                    }
                });

        CuentaCanal cuenta = existentePorTelegram.orElseGet(CuentaCanal::new);
        cuenta.setUsuario(cv.getUsuario());
        cuenta.setCanal(CANAL_TELEGRAM);
        cuenta.setIdentificadorExterno(telegramUserId);
        cuenta.setActiva(true);
        cuenta.setFechaVinculacion(LocalDateTime.now());

        return Optional.of(cuentaCanalRepository.save(cuenta));
    }

    @Transactional
    public void desvincularUsuario(Usuario usuario) {
        cuentaCanalRepository.findByCanalAndUsuarioIdAndActivaTrue(CANAL_TELEGRAM, usuario.getId())
                .ifPresent(c -> {
                    c.setActiva(false);
                    cuentaCanalRepository.save(c);
                });
    }

    public Optional<CuentaCanal> obtenerCuentaActivaPorUsuario(Long usuarioId) {
        return cuentaCanalRepository.findByCanalAndUsuarioIdAndActivaTrue(CANAL_TELEGRAM, usuarioId);
    }

    public Optional<CuentaCanal> obtenerCuentaActivaPorTelegramId(Long telegramUserId) {
        return cuentaCanalRepository.findByCanalAndIdentificadorExternoAndActivaTrue(CANAL_TELEGRAM, telegramUserId);
    }

    private String randomToken(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }

    public static String sha256(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
