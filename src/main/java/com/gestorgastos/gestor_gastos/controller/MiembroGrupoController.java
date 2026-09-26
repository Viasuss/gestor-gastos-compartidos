package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;


import java.util.Optional;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class MiembroGrupoController {

    private final MiembroGrupoRepository miembroGrupoRepository;
    private final GrupoRepository grupoRepository;
    private final GastoRepository gastoRepository;

    public MiembroGrupoController(
            MiembroGrupoRepository miembroGrupoRepository,
            GrupoRepository grupoRepository,
            GastoRepository gastoRepository) {

        this.miembroGrupoRepository = miembroGrupoRepository;
        this.grupoRepository = grupoRepository;
        this.gastoRepository = gastoRepository;
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
    @GetMapping("/grupo/{id}")
    public String verGrupo(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        Optional<MiembroGrupo> relacion =
                miembroGrupoRepository.findByUsuarioIdAndGrupoId(
                        usuario.getId(),
                        id
                );

        if (relacion.isEmpty() ||
                relacion.get().getEstado() != EstadoMiembro.MIEMBRO) {

            return "redirect:/mis-grupos";
        }

        Grupo grupo =
                grupoRepository.findById(id).orElse(null);

        if (grupo == null) {
            return "redirect:/mis-grupos";
        }

        List<MiembroGrupo> miembros =
                miembroGrupoRepository.findByGrupoId(id);

        List<Gasto> gastos =
                gastoRepository.findByGrupoOrderByFechaDesc(grupo);

        model.addAttribute("grupo", grupo);
        model.addAttribute("miembros", miembros);
        model.addAttribute("gastos", gastos);

        return "grupo";
    }
}