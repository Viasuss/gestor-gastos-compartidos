package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface GastoRepository extends JpaRepository<Gasto, Long> {

    List<Gasto> findByGrupoOrderByFechaDesc(Grupo grupo);
}
