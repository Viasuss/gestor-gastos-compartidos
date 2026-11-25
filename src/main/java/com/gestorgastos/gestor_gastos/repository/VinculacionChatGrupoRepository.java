package com.gestorgastos.gestor_gastos.repository;

import com.gestorgastos.gestor_gastos.entity.VinculacionChatGrupo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VinculacionChatGrupoRepository extends JpaRepository<VinculacionChatGrupo, Long> {

    Optional<VinculacionChatGrupo> findByChatIdAndActivaTrue(Long chatId);
}
