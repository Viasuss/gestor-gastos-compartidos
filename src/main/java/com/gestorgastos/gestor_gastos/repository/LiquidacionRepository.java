package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.EstadoLiquidacion;
import com.gestorgastos.gestor_gastos.entity.Liquidacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LiquidacionRepository extends JpaRepository<Liquidacion, Long> {

    List<Liquidacion> findByGrupoIdOrderByFechaCreacionDesc(Long grupoId);

    List<Liquidacion> findByGrupoIdAndEstado(Long grupoId, EstadoLiquidacion estado);

    Optional<Liquidacion> findByGrupoIdAndIdempotencyKey(Long grupoId, String idempotencyKey);

    List<Liquidacion> findByGrupoIdAndPagadorIdAndReceptorIdAndEstado(
            Long grupoId,
            Long pagadorId,
            Long receptorId,
            EstadoLiquidacion estado
    );
}
