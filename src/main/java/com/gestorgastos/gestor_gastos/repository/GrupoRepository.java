package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
public interface GrupoRepository  extends JpaRepository<Grupo, Long>{

    List<Grupo> findByCreador(Usuario creador);
}
