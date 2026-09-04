package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MiembroGrupoRepository extends JpaRepository<MiembroGrupo, Long> {

    List<MiembroGrupo> findByGrupoId(Long grupoId);

    Optional<MiembroGrupo> findByUsuarioIdAndGrupoId(Long usuarioId, Long grupoId);

}