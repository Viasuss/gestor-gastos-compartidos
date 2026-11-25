package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.CodigoVinculacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CodigoVinculacionRepository extends JpaRepository<CodigoVinculacion, Long> {

    Optional<CodigoVinculacion> findByCodigoHash(String codigoHash);

    List<CodigoVinculacion> findByUsuarioIdAndCanalAndUsadoFalse(Long usuarioId, String canal);
}
