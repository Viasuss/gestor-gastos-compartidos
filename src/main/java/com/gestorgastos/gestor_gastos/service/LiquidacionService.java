package com.gestorgastos.gestor_gastos.service;

import com.gestorgastos.gestor_gastos.entity.EstadoLiquidacion;
import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.Liquidacion;
import com.gestorgastos.gestor_gastos.entity.MetodoPago;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.LiquidacionRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.security.GrupoAccessService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LiquidacionService {

    private final LiquidacionRepository liquidacionRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final GrupoAccessService grupoAccessService;
    private final DeudaService deudaService;

    public LiquidacionService(
            LiquidacionRepository liquidacionRepository,
            MiembroGrupoRepository miembroGrupoRepository,
            GrupoAccessService grupoAccessService,
            DeudaService deudaService) {
        this.liquidacionRepository = liquidacionRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.grupoAccessService = grupoAccessService;
        this.deudaService = deudaService;
    }

    @Transactional
    public Liquidacion registrarSolicitud(
            Long grupoId,
            Usuario pagadorAutenticado,
            Long receptorId,
            BigDecimal monto,
            MetodoPago metodoPago,
            String nota,
            String idempotencyKey) {

        String cleanKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey.trim() : null;
        if (cleanKey != null) {
            Optional<Liquidacion> existente = liquidacionRepository.findByGrupoIdAndIdempotencyKey(grupoId, cleanKey);
            if (existente.isPresent()) {
                return existente.get();
            }
        }

        Grupo grupo = grupoAccessService.findGrupoSiEsMiembroActivo(grupoId, pagadorAutenticado)
                .orElseThrow(() -> new IllegalArgumentException("No tienes acceso activo a este grupo."));

        if (receptorId == null || receptorId.equals(pagadorAutenticado.getId())) {
            throw new IllegalArgumentException("El receptor debe ser otro miembro del grupo.");
        }

        MiembroGrupo relReceptor = miembroGrupoRepository.findByUsuarioIdAndGrupoId(receptorId, grupoId)
                .filter(m -> m.getEstado() == EstadoMiembro.MIEMBRO)
                .orElseThrow(() -> new IllegalArgumentException("El receptor no es miembro activo del grupo."));

        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("El monto de la liquidación debe ser mayor a $0.");
        }

        if (metodoPago == null) {
            throw new IllegalArgumentException("Debes seleccionar un método de pago.");
        }

        BigDecimal deudaActual = obtenerDeudaConReceptor(grupo, pagadorAutenticado, receptorId);
        BigDecimal totalPendiente = liquidacionRepository
                .findByGrupoIdAndPagadorIdAndReceptorIdAndEstado(
                        grupoId, pagadorAutenticado.getId(), receptorId, EstadoLiquidacion.PENDIENTE)
                .stream()
                .map(Liquidacion::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal deudaDisponible = deudaActual.subtract(totalPendiente);
        if (monto.compareTo(deudaDisponible) > 0) {
            throw new IllegalArgumentException(
                    "El monto supera tu deuda disponible ($" + deudaDisponible.toPlainString() + ")."
            );
        }

        Liquidacion liq = new Liquidacion();
        liq.setGrupo(grupo);
        liq.setPagador(pagadorAutenticado);
        liq.setReceptor(relReceptor.getUsuario());
        liq.setMonto(monto);
        liq.setEstado(EstadoLiquidacion.PENDIENTE);
        liq.setMetodoPago(metodoPago);
        liq.setNota(nota != null && !nota.isBlank() ? nota.trim().substring(0, Math.min(nota.trim().length(), 250)) : null);
        liq.setFechaCreacion(LocalDateTime.now());
        liq.setIdempotencyKey(cleanKey);

        try {
            return liquidacionRepository.save(liq);
        } catch (DataIntegrityViolationException ex) {
            if (cleanKey != null) {
                return liquidacionRepository.findByGrupoIdAndIdempotencyKey(grupoId, cleanKey)
                        .orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    @Transactional
    public Liquidacion confirmar(Long liquidacionId, Usuario receptorAutenticado) {
        Liquidacion liq = liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada."));

        if (receptorAutenticado == null || !liq.getReceptor().getId().equals(receptorAutenticado.getId())) {
            throw new SecurityException("Solo el receptor puede confirmar esta liquidación.");
        }

        if (liq.getEstado() == EstadoLiquidacion.CONFIRMADA) {
            return liq;
        }

        if (liq.getEstado() != EstadoLiquidacion.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden confirmar liquidaciones pendientes.");
        }

        BigDecimal deudaActual = obtenerDeudaConReceptor(liq.getGrupo(), liq.getPagador(), liq.getReceptor().getId());
        if (deudaActual.compareTo(liq.getMonto()) < 0) {
            throw new IllegalStateException("La deuda actual ya no cubre el monto de esta liquidación.");
        }

        liq.setEstado(EstadoLiquidacion.CONFIRMADA);
        liq.setFechaResolucion(LocalDateTime.now());
        return liquidacionRepository.save(liq);
    }

    @Transactional
    public Liquidacion rechazar(Long liquidacionId, Usuario receptorAutenticado) {
        Liquidacion liq = liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada."));

        if (receptorAutenticado == null || !liq.getReceptor().getId().equals(receptorAutenticado.getId())) {
            throw new SecurityException("Solo el receptor puede rechazar esta liquidación.");
        }

        if (liq.getEstado() == EstadoLiquidacion.RECHAZADA) {
            return liq;
        }

        if (liq.getEstado() != EstadoLiquidacion.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden rechazar liquidaciones pendientes.");
        }

        liq.setEstado(EstadoLiquidacion.RECHAZADA);
        liq.setFechaResolucion(LocalDateTime.now());
        return liquidacionRepository.save(liq);
    }

    @Transactional
    public Liquidacion cancelar(Long liquidacionId, Usuario pagadorAutenticado) {
        Liquidacion liq = liquidacionRepository.findById(liquidacionId)
                .orElseThrow(() -> new IllegalArgumentException("Liquidación no encontrada."));

        if (pagadorAutenticado == null || !liq.getPagador().getId().equals(pagadorAutenticado.getId())) {
            throw new SecurityException("Solo el pagador puede cancelar esta liquidación.");
        }

        if (liq.getEstado() == EstadoLiquidacion.CANCELADA) {
            return liq;
        }

        if (liq.getEstado() != EstadoLiquidacion.PENDIENTE) {
            throw new IllegalStateException("Solo se pueden cancelar liquidaciones pendientes.");
        }

        liq.setEstado(EstadoLiquidacion.CANCELADA);
        liq.setFechaResolucion(LocalDateTime.now());
        return liquidacionRepository.save(liq);
    }

    private BigDecimal obtenerDeudaConReceptor(Grupo grupo, Usuario pagador, Long receptorId) {
        return deudaService.calcularDeudas(grupo, pagador).stream()
                .filter(d -> d.getOtraPersona().getId().equals(receptorId))
                .filter(DeudaConPersona::leDebo)
                .map(d -> d.getMonto().abs())
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }
}
