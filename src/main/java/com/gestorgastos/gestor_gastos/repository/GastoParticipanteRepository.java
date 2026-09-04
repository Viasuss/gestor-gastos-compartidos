package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.GastoParticipante;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GastoParticipanteRepository extends JpaRepository<GastoParticipante, Long> {

    List<GastoParticipante> findByGastoId(Long gastoId);

}