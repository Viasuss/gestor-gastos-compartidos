package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class MiembroGrupoController {

    private final MiembroGrupoRepository miembroGrupoRepository;

    public MiembroGrupoController(MiembroGrupoRepository miembroGrupoRepository) {
        this.miembroGrupoRepository = miembroGrupoRepository;
    }

    @GetMapping("/mis-grupos")
    public String mostrarMisGrupos(HttpSession session, Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        List<MiembroGrupo> miembros =
                miembroGrupoRepository.findByUsuarioId(usuario.getId());

        model.addAttribute("miembros", miembros);

        return "mis-grupos";
    }
}