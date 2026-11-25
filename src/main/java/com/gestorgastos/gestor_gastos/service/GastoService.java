package com.gestorgastos.gestor_gastos.service;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.entity.GastoParticipante;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.PagoGasto;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.GastoParticipanteRepository;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.PagoGastoRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * Nucleo financiero reutilizable para registro determinista de gastos (B3).
 * Compartido por el formulario manual (GastoController) y la Bandeja Inteligente.
 */
@Service
public class GastoService {

    public static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999999999");
    public static final int DESCRIPCION_MAXIMA = 100;

    private final GastoRepository gastoRepository;
    private final GastoParticipanteRepository gastoParticipanteRepository;
    private final GrupoRepository grupoRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final PagoGastoRepository pagoGastoRepository;

    public GastoService(
            GastoRepository gastoRepository,
            GastoParticipanteRepository gastoParticipanteRepository,
            GrupoRepository grupoRepository,
            MiembroGrupoRepository miembroGrupoRepository,
            PagoGastoRepository pagoGastoRepository) {
        this.gastoRepository = gastoRepository;
        this.gastoParticipanteRepository = gastoParticipanteRepository;
        this.grupoRepository = grupoRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.pagoGastoRepository = pagoGastoRepository;
    }

    public static class GastoValidationException extends RuntimeException {
        private final String errorCode;

        public GastoValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }

        public String getErrorCode() {
            return errorCode;
        }
    }

    @Transactional
    public Gasto registrarGasto(
            Long grupoId,
            Usuario usuarioAutenticado,
            String descripcion,
            BigDecimal montoTotal,
            LocalDate fechaGasto,
            List<Long> pagadoresId,
            List<BigDecimal> montosPagados,
            List<Long> participantes,
            String idempotencyKey) {

        if (usuarioAutenticado == null || usuarioAutenticado.getId() == null) {
            throw new GastoValidationException("login", "Usuario no autenticado.");
        }

        List<MiembroGrupo> activos = miembroGrupoRepository.findByGrupoIdAndEstado(grupoId, EstadoMiembro.MIEMBRO);
        Map<Long, Usuario> miembrosPorId = new HashMap<>();
        for (MiembroGrupo m : activos) {
            miembrosPorId.put(m.getUsuario().getId(), m.getUsuario());
        }

        if (!miembrosPorId.containsKey(usuarioAutenticado.getId())) {
            throw new GastoValidationException("acceso", "No eres miembro activo del grupo.");
        }

        Grupo grupo = grupoRepository.findById(grupoId)
                .orElseThrow(() -> new GastoValidationException("acceso", "Grupo no encontrado."));

        String cleanKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey.trim() : null;
        if (cleanKey != null) {
            Optional<Gasto> existente = gastoRepository.findByGrupoIdAndIdempotencyKey(grupoId, cleanKey);
            if (existente.isPresent()) {
                return existente.get();
            }
        }

        String descripcionLimpia = descripcion == null ? "" : descripcion.trim();
        if (descripcionLimpia.isEmpty() || descripcionLimpia.length() > DESCRIPCION_MAXIMA) {
            throw new GastoValidationException("descripcion", "Descripción inválida.");
        }

        if (montoTotal == null || montoTotal.signum() <= 0 || montoTotal.compareTo(MONTO_MAXIMO) > 0) {
            throw new GastoValidationException("monto", "Monto total inválido.");
        }

        if (fechaGasto == null || fechaGasto.isAfter(LocalDate.now().plusDays(1))) {
            throw new GastoValidationException("fecha", "Fecha inválida.");
        }

        if (pagadoresId == null || pagadoresId.isEmpty()) {
            throw new GastoValidationException("pagadores", "Debes seleccionar al menos un pagador.");
        }

        if (montosPagados == null
                || pagadoresId.size() != montosPagados.size()
                || new HashSet<>(pagadoresId).size() != pagadoresId.size()) {
            throw new GastoValidationException("pagos", "Lista de pagadores y montos inconsistente.");
        }

        BigDecimal sumaPagos = BigDecimal.ZERO;
        for (int i = 0; i < pagadoresId.size(); i++) {
            if (!miembrosPorId.containsKey(pagadoresId.get(i))) {
                throw new GastoValidationException("pagos", "Pagador no pertenece al grupo.");
            }
            BigDecimal montoPago = montosPagados.get(i);
            if (montoPago == null || montoPago.signum() <= 0) {
                throw new GastoValidationException("pagomonto", "Todo pagador debe aportar más de $0.");
            }
            sumaPagos = sumaPagos.add(montoPago);
        }

        if (sumaPagos.compareTo(montoTotal) != 0) {
            throw new GastoValidationException("suma", "La suma de los pagos no coincide con el total.");
        }

        if (participantes == null || participantes.isEmpty()) {
            throw new GastoValidationException("participantes", "Selecciona al menos un participante.");
        }

        Set<Long> participantesUnicos = new LinkedHashSet<>(participantes);
        for (Long participanteId : participantesUnicos) {
            if (!miembrosPorId.containsKey(participanteId)) {
                throw new GastoValidationException("participantes", "Participante no pertenece al grupo.");
            }
        }

        Gasto gasto = new Gasto();
        gasto.setDescripcion(descripcionLimpia);
        gasto.setMonto(montoTotal);
        gasto.setFecha(fechaGasto);
        gasto.setGrupo(grupo);
        gasto.setIdempotencyKey(cleanKey);

        try {
            gastoRepository.save(gasto);
        } catch (DataIntegrityViolationException ex) {
            if (cleanKey != null) {
                return gastoRepository.findByGrupoIdAndIdempotencyKey(grupoId, cleanKey)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }

        for (int i = 0; i < pagadoresId.size(); i++) {
            PagoGasto pago = new PagoGasto();
            pago.setGasto(gasto);
            pago.setUsuario(miembrosPorId.get(pagadoresId.get(i)));
            pago.setMonto(montosPagados.get(i));
            pagoGastoRepository.save(pago);
        }

        for (Long participanteId : participantesUnicos) {
            GastoParticipante gp = new GastoParticipante();
            gp.setUsuario(miembrosPorId.get(participanteId));
            gp.setGrupo(grupo);
            gp.setGasto(gasto);
            gastoParticipanteRepository.save(gp);
        }

        return gasto;
    }
}
