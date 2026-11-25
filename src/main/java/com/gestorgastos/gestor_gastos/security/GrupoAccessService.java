package com.gestorgastos.gestor_gastos.security;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.Optional;

/**
 * Servicio centralizado de autorizacion de acceso a grupos e invitaciones (B1.3).
 */
@Service
public class GrupoAccessService {

    private final MiembroGrupoRepository miembroGrupoRepository;
    private final GrupoRepository grupoRepository;

    public GrupoAccessService(
            MiembroGrupoRepository miembroGrupoRepository,
            GrupoRepository grupoRepository) {
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.grupoRepository = grupoRepository;
    }

    public Optional<Grupo> findGrupoSiEsMiembroActivo(Long grupoId, Usuario usuario) {
        if (grupoId == null || usuario == null || usuario.getId() == null) {
            return Optional.empty();
        }
        Optional<MiembroGrupo> relacion = miembroGrupoRepository.findByUsuarioIdAndGrupoId(usuario.getId(), grupoId);
        if (relacion.isEmpty() || relacion.get().getEstado() != EstadoMiembro.MIEMBRO) {
            return Optional.empty();
        }
        return grupoRepository.findById(grupoId);
    }

    public Optional<MiembroGrupo> findInvitacionPendienteDeUsuario(Long invitacionId, Usuario usuario) {
        if (invitacionId == null || usuario == null || usuario.getId() == null) {
            return Optional.empty();
        }
        return miembroGrupoRepository.findById(invitacionId)
                .filter(inv -> Objects.equals(inv.getUsuario().getId(), usuario.getId()))
                .filter(inv -> inv.getEstado() == EstadoMiembro.PENDIENTE);
    }
}
