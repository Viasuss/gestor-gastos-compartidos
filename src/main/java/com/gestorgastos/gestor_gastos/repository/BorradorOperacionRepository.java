package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.BorradorOperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BorradorOperacionRepository extends JpaRepository<BorradorOperacion, Long> {

    List<BorradorOperacion> findByUsuarioIdOrderByFechaCreacionDesc(Long usuarioId);

    Optional<BorradorOperacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    Optional<BorradorOperacion> findByUsuarioIdAndIdempotencyKey(Long usuarioId, String idempotencyKey);
}
