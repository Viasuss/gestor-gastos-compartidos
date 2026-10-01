package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;

import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class GrupoController {

    private final GrupoRepository grupoRepository;
    private final UsuarioRepository usuarioRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;

    public GrupoController(
            GrupoRepository grupoRepository,
            UsuarioRepository usuarioRepository,
            MiembroGrupoRepository miembroGrupoRepository) {

        this.grupoRepository = grupoRepository;
        this.usuarioRepository = usuarioRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
    }


    // ==========================================
    // MOSTRAR PANTALLA PARA CREAR GRUPO
    // ==========================================

    @GetMapping("/grupo/crear")
    public String mostrarCrearGrupo(
            HttpSession session,
            Model model) {

        if (session.getAttribute("usuario") == null) {
            return "redirect:/login";
        }

        List<String> usuariosTemporales =
                (List<String>) session.getAttribute("usuariosTemporales");

        if (usuariosTemporales == null) {

            usuariosTemporales = new ArrayList<>();

            session.setAttribute(
                    "usuariosTemporales",
                    usuariosTemporales
            );
        }

        model.addAttribute(
                "usuariosTemporales",
                usuariosTemporales
        );

        return "crear-grupo";
    }


    // ==========================================
    // AGREGAR USUARIO TEMPORALMENTE
    // ==========================================

    @PostMapping("/grupo/agregar-usuario")
    public String agregarUsuarioTemporal(
            @RequestParam String email,
            HttpSession session) {

        Usuario usuarioActual =
                (Usuario) session.getAttribute("usuario");

        if (usuarioActual == null) {
            return "redirect:/login";
        }

        // NUEVO (bug corregido): no dejar agregar el propio correo.
        // Antes solo se validaba que el correo no estuviera repetido
        // EN LA LISTA, pero nunca se comparaba contra el usuario en
        // sesión. Por eso alguien podía "invitarse a sí mismo": el
        // creador ya queda como MIEMBRO automáticamente al crear el
        // grupo, así que además terminaba duplicado como PENDIENTE.
        // equalsIgnoreCase para que "Johan@Gmail.com" y "johan@gmail.com"
        // se traten como el mismo correo.
        if (email.equalsIgnoreCase(usuarioActual.getEmail())) {
            return "redirect:/grupo/crear?error=propio";
        }

        Usuario usuario =
                usuarioRepository.findByEmail(email).orElse(null);

        if (usuario == null) {
            return "redirect:/grupo/crear?error=usuario";
        }

        List<String> usuariosTemporales =
                (List<String>) session.getAttribute("usuariosTemporales");

        if (usuariosTemporales == null) {

            usuariosTemporales = new ArrayList<>();

            session.setAttribute(
                    "usuariosTemporales",
                    usuariosTemporales
            );
        }

        if (!usuariosTemporales.contains(email)) {

            usuariosTemporales.add(email);
        }

        return "redirect:/grupo/crear";
    }


    // ==========================================
    // QUITAR USUARIO DE LA LISTA TEMPORAL (NUEVO)
    // ==========================================

    // Botón "x" del Figma: permite quitar a alguien de "Personas agregadas"
    // antes de crear el grupo, sin tener que recargar todo el formulario.
    @PostMapping("/grupo/quitar-usuario")
    public String quitarUsuarioTemporal(
            @RequestParam String email,
            HttpSession session) {

        if (session.getAttribute("usuario") == null) {
            return "redirect:/login";
        }

        List<String> usuariosTemporales =
                (List<String>) session.getAttribute("usuariosTemporales");

        if (usuariosTemporales != null) {
            usuariosTemporales.remove(email);
        }

        return "redirect:/grupo/crear";
    }


    // ==========================================
    // CREAR GRUPO
    // ==========================================

    @PostMapping("/grupo/crear")
    public String crearGrupo(
            @RequestParam String name,
            HttpSession session) {

        Usuario usuarioActual =
                (Usuario) session.getAttribute("usuario");

        if (usuarioActual == null) {
            return "redirect:/login";
        }

        Grupo grupo = new Grupo();

        grupo.setName(name);
        grupo.setCreador(usuarioActual);
        grupo.setFecha_creacion(LocalDate.now());

        grupoRepository.save(grupo);


        // ==========================================
        // AGREGAR AL CREADOR COMO MIEMBRO
        // ==========================================

        MiembroGrupo creador = new MiembroGrupo();

        creador.setUsuario(usuarioActual);
        creador.setGrupo(grupo);
        creador.setEstado(EstadoMiembro.MIEMBRO);

        miembroGrupoRepository.save(creador);


        // ==========================================
        // AGREGAR USUARIOS INVITADOS
        // ==========================================

        List<String> usuariosTemporales =
                (List<String>) session.getAttribute("usuariosTemporales");

        if (usuariosTemporales != null) {

            for (String email : usuariosTemporales) {

                Usuario usuario =
                        usuarioRepository
                                .findByEmail(email)
                                .orElse(null);

                if (usuario != null) {

                    MiembroGrupo miembro =
                            new MiembroGrupo();

                    miembro.setUsuario(usuario);
                    miembro.setGrupo(grupo);
                    miembro.setEstado(
                            EstadoMiembro.PENDIENTE
                    );

                    miembroGrupoRepository.save(miembro);
                }
            }
        }

        session.removeAttribute("usuariosTemporales");

        return "redirect:/inicio";
    }
}