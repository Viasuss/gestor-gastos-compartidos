package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;
import java.util.Objects;

@Controller
public class InvitacionController {

    private final MiembroGrupoRepository miembroGrupoRepository;

    public InvitacionController(
            MiembroGrupoRepository miembroGrupoRepository) {

        this.miembroGrupoRepository = miembroGrupoRepository;
    }

    @GetMapping("/invitaciones")
    public String mostrarInvitaciones(
            HttpSession session,
            Model model) {

        Usuario usuario =
                (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        List<MiembroGrupo> invitaciones =
                miembroGrupoRepository.findByUsuarioIdAndEstado(
                        usuario.getId(),
                        EstadoMiembro.PENDIENTE
                );

        model.addAttribute("invitaciones", invitaciones);

        return "invitaciones";
    }
    @PostMapping("/invitacion/{id}/aceptar")
    public String aceptarInvitacion(
            @PathVariable Long id,
            HttpSession session) {

        Usuario usuario =
                (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        MiembroGrupo invitacion =
                miembroGrupoRepository.findById(id).orElse(null);

        if (invitacion == null) {
            return "redirect:/invitaciones";
        }

        if (!Objects.equals(invitacion.getUsuario().getId(), usuario.getId())) {
            return "redirect:/invitaciones";
        }

        if (invitacion.getEstado() != EstadoMiembro.PENDIENTE) {
            return "redirect:/invitaciones";
        }

        invitacion.setEstado(EstadoMiembro.MIEMBRO);

        miembroGrupoRepository.save(invitacion);

        return "redirect:/invitaciones";
    }
    @PostMapping("/invitacion/{id}/rechazar")
    public String rechazarInvitacion(
            @PathVariable Long id,
            HttpSession session) {

        Usuario usuario =
                (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        MiembroGrupo invitacion =
                miembroGrupoRepository.findById(id).orElse(null);

        if (invitacion == null) {
            return "redirect:/invitaciones";
        }

        if (!Objects.equals(invitacion.getUsuario().getId(), usuario.getId())) {
            return "redirect:/invitaciones";
        }

        if (invitacion.getEstado() != EstadoMiembro.PENDIENTE) {
            return "redirect:/invitaciones";
        }

        invitacion.setEstado(EstadoMiembro.RECHAZO);

        miembroGrupoRepository.save(invitacion);

        return "redirect:/invitaciones";
    }
}
