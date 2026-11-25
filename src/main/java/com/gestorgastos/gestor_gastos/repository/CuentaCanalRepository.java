package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.CuentaCanal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CuentaCanalRepository extends JpaRepository<CuentaCanal, Long> {

    Optional<CuentaCanal> findByCanalAndIdentificadorExterno(String canal, Long identificadorExterno);

    Optional<CuentaCanal> findByCanalAndIdentificadorExternoAndActivaTrue(String canal, Long identificadorExterno);

    Optional<CuentaCanal> findByCanalAndUsuarioIdAndActivaTrue(String canal, Long usuarioId);
}
