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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class GrupoController {

    // Largo máximo del nombre de un grupo
    private static final int NOMBRE_MAXIMO = 60;

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
    // LISTA TEMPORAL DE CORREOS (en la sesión)
    // ==========================================

    // NUEVO: antes este mismo bloque (leer la lista de la sesión y crearla
    // si no existía) estaba copiado en 4 métodos, cada uno con su propio
    // cast sin verificar. Ahora vive en un solo lugar.
    @SuppressWarnings("unchecked")
    private List<String> obtenerUsuariosTemporales(HttpSession session) {

        List<String> usuariosTemporales =
                (List<String>) session.getAttribute("usuariosTemporales");

        if (usuariosTemporales == null) {

            usuariosTemporales = new ArrayList<>();

            session.setAttribute(
                    "usuariosTemporales",
                    usuariosTemporales
            );
        }

        return usuariosTemporales;
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

        List<String> usuariosTemporales = obtenerUsuariosTemporales(session);

        List<Usuario> personasAgregadas = new ArrayList<>();
        for (String emailTemp : usuariosTemporales) {
            usuarioRepository.findByEmailIgnoreCase(emailTemp).ifPresent(personasAgregadas::add);
        }
        model.addAttribute("personasAgregadas", personasAgregadas);
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

        String emailLimpio = email == null ? "" : email.trim();

        // Bug corregido antes: no dejar agregar el propio correo. El
        // creador ya queda como MIEMBRO automáticamente al crear el
        // grupo. equalsIgnoreCase para que "Johan@Gmail.com" y
        // "johan@gmail.com" se traten como el mismo correo.
        if (emailLimpio.equalsIgnoreCase(usuarioActual.getEmail())) {
            return "redirect:/grupo/crear?error=propio";
        }

        // CAMBIO: findByEmailIgnoreCase (igual que en el login). Antes se
        // usaba findByEmail, que dependía de cómo estuviera configurada la
        // base de datos para distinguir mayúsculas de minúsculas.
        Usuario usuario =
                usuarioRepository.findByEmailIgnoreCase(emailLimpio).orElse(null);

        if (usuario == null) {
            return "redirect:/grupo/crear?error=usuario";
        }

        List<String> usuariosTemporales = obtenerUsuariosTemporales(session);

        // CAMBIO: guardamos el correo TAL COMO está registrado, no como lo
        // escribió la persona. Así "ANA@x.com" y "ana@x.com" no entran
        // dos veces a la lista.
        String emailRegistrado = usuario.getEmail();

        boolean yaEstaEnLaLista = usuariosTemporales.stream()
                .anyMatch(e -> e.equalsIgnoreCase(emailRegistrado));

        if (!yaEstaEnLaLista) {

            usuariosTemporales.add(emailRegistrado);
        }

        return "redirect:/grupo/crear";
    }


    // ==========================================
    // QUITAR USUARIO DE LA LISTA TEMPORAL
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

        obtenerUsuariosTemporales(session)
                .removeIf(e -> e.equalsIgnoreCase(email));

        return "redirect:/grupo/crear";
    }


    // ==========================================
    // CREAR GRUPO
    // ==========================================

    @Transactional
    @PostMapping("/grupo/crear")
    public String crearGrupo(
            @RequestParam(required = false) String name,
            HttpSession session) {

        Usuario usuarioActual =
                (Usuario) session.getAttribute("usuario");

        if (usuarioActual == null) {
            return "redirect:/login";
        }

        // CAMBIO: antes se validaba el nombre ya recortado, pero se
        // guardaba el original (con los espacios de los costados). Ahora
        // se guarda el recortado, y también se limita el largo.
        String nombreLimpio = name == null ? "" : name.trim();

        if (nombreLimpio.isEmpty() || nombreLimpio.length() > NOMBRE_MAXIMO) {
            return "redirect:/grupo/crear?error=nombre";
        }

        Grupo grupo = new Grupo();

        grupo.setName(nombreLimpio);
        grupo.setCreador(usuarioActual);
        grupo.setFechaCreacion(LocalDate.now());

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

        for (String email : obtenerUsuariosTemporales(session)) {

            Usuario usuario =
                    usuarioRepository
                            .findByEmailIgnoreCase(email)
                            .orElse(null);

            // El creador ya es MIEMBRO: no se le crea además una
            // invitación pendiente
            if (usuario != null
                    && !usuario.getId().equals(usuarioActual.getId())) {

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

        session.removeAttribute("usuariosTemporales");

        return "redirect:/inicio?grupoCreado=true&nombre=" + java.net.URLEncoder.encode(grupo.getName(), java.nio.charset.StandardCharsets.UTF_8);
    }
}