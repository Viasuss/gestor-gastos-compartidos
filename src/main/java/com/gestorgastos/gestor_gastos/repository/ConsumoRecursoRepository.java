package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.ConsumoRecurso;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ConsumoRecursoRepository extends JpaRepository<ConsumoRecurso, Long> {

    Optional<ConsumoRecurso> findByProveedorAndUsuarioIdAndFecha(String proveedor, Long usuarioId, LocalDate fecha);

    List<ConsumoRecurso> findByProveedorAndFecha(String proveedor, LocalDate fecha);
}
