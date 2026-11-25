package com.gestorgastos.gestor_gastos.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestorgastos.gestor_gastos.entity.*;
import com.gestorgastos.gestor_gastos.repository.BorradorOperacionRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.security.GrupoAccessService;
import com.gestorgastos.gestor_gastos.service.ia.InteligenciaFinancieraService;
import com.gestorgastos.gestor_gastos.service.ia.InterpretacionGastoRequest;
import com.gestorgastos.gestor_gastos.service.ia.InterpretacionGastoResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

@Service
public class BorradorOperacionService {

    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Bogota");

    private final BorradorOperacionRepository borradorRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final GrupoAccessService grupoAccessService;
    private final InteligenciaFinancieraService iaService;
    private final GastoService gastoService;
    private final ObjectMapper objectMapper;

    @Value("${nexus.borradores.expiracion-dias:7}")
    private int expiracionDias;

    public BorradorOperacionService(
            BorradorOperacionRepository borradorRepository,
            MiembroGrupoRepository miembroGrupoRepository,
            GrupoAccessService grupoAccessService,
            InteligenciaFinancieraService iaService,
            GastoService gastoService) {
        this.borradorRepository = borradorRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.grupoAccessService = grupoAccessService;
        this.iaService = iaService;
        this.gastoService = gastoService;
        this.objectMapper = new ObjectMapper();
    }

    public BorradorOperacion crearDesdeTexto(Usuario usuario, Long grupoId, String texto, String idempotencyKey) {
        Grupo grupo = grupoAccessService.findGrupoSiEsMiembroActivo(grupoId, usuario)
                .orElseThrow(() -> new IllegalArgumentException("No tienes acceso activo a este grupo."));

        String cleanKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey.trim() : null;
        if (cleanKey != null) {
            Optional<BorradorOperacion> existente =
                    borradorRepository.findByUsuarioIdAndIdempotencyKey(usuario.getId(), cleanKey);
            if (existente.isPresent()) {
                return existente.get();
            }
        }

        List<MiembroGrupo> miembros = miembroGrupoRepository.findByGrupoIdAndEstado(grupoId, EstadoMiembro.MIEMBRO);
        List<String> nombres = miembros.stream().map(m -> m.getUsuario().getName()).toList();

        // Llamada fuera de transaccion BD prolongada
        InterpretacionGastoResponse resp = iaService.interpretarGasto(
                new InterpretacionGastoRequest(
                        grupoId,
                        texto,
                        miembros.size(),
                        nombres,
                        LocalDate.now(BUSINESS_ZONE)
                ),
                usuario.getId()
        );

        LocalDateTime ahora = LocalDateTime.now();
        BorradorOperacion b = new BorradorOperacion();
        b.setUsuario(usuario);
        b.setGrupo(grupo);
        b.setTipoOperacion("REGISTRO_GASTO");
        b.setConcepto(resp.concepto() != null && !resp.concepto().isBlank() ? resp.concepto() : "Gasto compartido");
        b.setMontoCop(resp.montoCop());
        b.setCategoriaSugerida(resp.categoriaSugerida());
        b.setFechaSugerida(resp.fechaSugerida() != null ? resp.fechaSugerida() : LocalDate.now(BUSINESS_ZONE));
        b.setMensajeOriginal(texto != null && texto.length() > 500 ? texto.substring(0, 500) : texto);
        b.setDatosEstructurados(serializarSeguro(resp));
        b.setEstado(resp.requiereAclaracion() ? EstadoBorrador.REQUIERE_ACLARACION : EstadoBorrador.PENDIENTE);
        b.setOrigen("WEB_ASISTENTE");
        b.setProveedorUtilizado(resp.origenProveedor());
        b.setFechaCreacion(ahora);
        b.setFechaActualizacion(ahora);
        b.setFechaExpiracion(ahora.plusDays(Math.max(1, expiracionDias)));
        b.setIdempotencyKey(cleanKey);

        return borradorRepository.save(b);
    }

    @Transactional
    public Gasto confirmarBorrador(
            Long borradorId,
            Usuario usuario,
            String concepto,
            BigDecimal montoCop,
            LocalDate fecha,
            Long pagadorId,
            List<Long> participantesIds,
            String confirmIdempotencyKey) {

        BorradorOperacion b = borradorRepository.findByIdAndUsuarioId(borradorId, usuario.getId())
                .orElseThrow(() -> new SecurityException("No tienes permiso sobre este borrador."));

        if (b.getEstado() == EstadoBorrador.CONFIRMADO && b.getGastoConfirmadoId() != null) {
            throw new IllegalStateException("Este borrador ya fue confirmado.");
        }

        if (b.getEstado() == EstadoBorrador.DESCARTADO || b.getEstado() == EstadoBorrador.EXPIRADO) {
            throw new IllegalStateException("El borrador ya no se encuentra activo.");
        }

        if (b.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            b.setEstado(EstadoBorrador.EXPIRADO);
            b.setFechaActualizacion(LocalDateTime.now());
            borradorRepository.save(b);
            throw new IllegalStateException("El borrador ha expirado.");
        }

        String gastoKey = (confirmIdempotencyKey != null && !confirmIdempotencyKey.isBlank())
                ? confirmIdempotencyKey.trim()
                : "borrador-" + b.getId();

        Gasto gasto = gastoService.registrarGasto(
                b.getGrupo().getId(),
                usuario,
                concepto,
                montoCop,
                fecha,
                List.of(pagadorId),
                List.of(montoCop),
                participantesIds,
                gastoKey
        );

        b.setConcepto(gasto.getDescripcion());
        b.setMontoCop(gasto.getMonto());
        b.setFechaSugerida(gasto.getFecha());
        b.setEstado(EstadoBorrador.CONFIRMADO);
        b.setGastoConfirmadoId(gasto.getId());
        b.setFechaResolucion(LocalDateTime.now());
        b.setFechaActualizacion(LocalDateTime.now());
        borradorRepository.save(b);

        return gasto;
    }

    @Transactional
    public void descartarBorrador(Long borradorId, Usuario usuario) {
        BorradorOperacion b = borradorRepository.findByIdAndUsuarioId(borradorId, usuario.getId())
                .orElseThrow(() -> new SecurityException("No tienes permiso sobre este borrador."));

        if (b.getEstado() == EstadoBorrador.CONFIRMADO) {
            throw new IllegalStateException("No se puede descartar un borrador ya confirmado.");
        }

        b.setEstado(EstadoBorrador.DESCARTADO);
        b.setFechaResolucion(LocalDateTime.now());
        b.setFechaActualizacion(LocalDateTime.now());
        borradorRepository.save(b);
    }

    private String serializarSeguro(InterpretacionGastoResponse resp) {
        try {
            String json = objectMapper.writeValueAsString(resp);
            return json.length() > 2000 ? json.substring(0, 2000) : json;
        } catch (Exception e) {
            return "{}";
        }
    }
}
