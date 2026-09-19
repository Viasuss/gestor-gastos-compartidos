package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import org.springframework.data.jpa.repository.JpaRepository;
import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;

import java.util.List;
import java.util.Optional;

public interface MiembroGrupoRepository extends JpaRepository<MiembroGrupo, Long> {

    List<MiembroGrupo> findByGrupoId(Long grupoId);

    Optional<MiembroGrupo> findByUsuarioIdAndGrupoId(
            Long usuarioId,
            Long grupoId);

    List<MiembroGrupo> findByUsuarioId(Long usuarioId);

    List<MiembroGrupo> findByUsuarioIdAndEstado(
            Long usuarioId,
            EstadoMiembro estado);
}