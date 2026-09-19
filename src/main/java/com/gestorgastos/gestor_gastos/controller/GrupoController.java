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

        // Verificar sesión
        if (session.getAttribute("usuario") == null) {
            return "redirect:/login";
        }

        // Obtener lista temporal de correos
        List<String> usuariosTemporales =
                (List<String>) session.getAttribute("usuariosTemporales");

        // Si todavía no existe la lista, crearla
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

        // Verificar sesión
        if (session.getAttribute("usuario") == null) {
            return "redirect:/login";
        }

        // Buscar usuario registrado
        Usuario usuario =
                usuarioRepository.findByEmail(email).orElse(null);

        // Si no existe, regresar a crear grupo
        if (usuario == null) {
            return "redirect:/grupo/crear?error=usuario";
        }

        // Obtener lista temporal
        List<String> usuariosTemporales =
                (List<String>) session.getAttribute("usuariosTemporales");

        // Crear lista si no existe
        if (usuariosTemporales == null) {

            usuariosTemporales = new ArrayList<>();

            session.setAttribute(
                    "usuariosTemporales",
                    usuariosTemporales
            );
        }

        // Evitar agregar el mismo correo dos veces
        if (!usuariosTemporales.contains(email)) {

            usuariosTemporales.add(email);
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

        // Obtener usuario actual
        Usuario usuarioActual =
                (Usuario) session.getAttribute("usuario");

        if (usuarioActual == null) {
            return "redirect:/login";
        }

        // Crear grupo
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

        // Limpiar lista temporal
        session.removeAttribute("usuariosTemporales");

        return "redirect:/inicio";
    }
}