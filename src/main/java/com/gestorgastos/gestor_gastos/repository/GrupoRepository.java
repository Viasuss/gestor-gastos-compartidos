package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GrupoRepository  extends JpaRepository<Grupo, Long>{

    List<Grupo> findByCreador(Usuario creador);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM Grupo g WHERE g.id = :id")
    Optional<Grupo> findByIdForUpdate(@Param("id") Long id);
}
