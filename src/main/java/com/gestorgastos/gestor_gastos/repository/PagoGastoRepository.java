package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.PagoGasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagoGastoRepository extends JpaRepository<PagoGasto, Long> {

    // Todos los pagos hechos dentro de un gasto específico.
    // Si un gasto tuvo 2 pagadores, esto devuelve las 2 filas.
    List<PagoGasto> findByGastoId(Long gastoId);
}
