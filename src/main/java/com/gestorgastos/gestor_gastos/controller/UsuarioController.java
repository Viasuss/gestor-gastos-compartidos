package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.service.BalanceGeneral;
import com.gestorgastos.gestor_gastos.service.DeudaService;
import com.gestorgastos.gestor_gastos.service.GoogleAuthService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final DeudaService deudaService;
    private final GoogleAuthService googleAuthService;

    public UsuarioController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            MiembroGrupoRepository miembroGrupoRepository,
            DeudaService deudaService,
            GoogleAuthService googleAuthService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.deudaService = deudaService;
        this.googleAuthService = googleAuthService;
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {

        model.addAttribute("usuario", new Usuario());
        model.addAttribute("googleClientId", googleAuthService.getGoogleClientId());

        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario) {

        if (usuario.getName() == null || usuario.getName().isBlank() ||
                usuario.getEmail() == null || usuario.getEmail().isBlank() ||
                usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            return "redirect:/registro?error=campos";
        }

        String emailNormalizado = usuario.getEmail().trim().toLowerCase();
        if (usuarioRepository.findByEmailIgnoreCase(emailNormalizado).isPresent()) {
            return "redirect:/registro?error=existe";
        }

        usuario.setName(usuario.getName().trim());
        usuario.setEmail(emailNormalizado);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));

        usuarioRepository.save(usuario);

        return "redirect:/login?registrado";
    }

    @GetMapping("/login")
    public String mostrarLogin(Model model) {

        model.addAttribute("googleClientId", googleAuthService.getGoogleClientId());

        return "login";
    }

    @PostMapping("/login")
    public String iniciarSesion(
            @RequestParam String email,
            @RequestParam String password,
            HttpServletRequest request,
            HttpSession session) {

        String emailNormalizado = email != null ? email.trim() : "";

        return usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .filter(usuario -> passwordEncoder.matches(
                        password,
                        usuario.getPassword()
                ))
                .map(usuario -> {
                    abrirSesion(request, session, usuario);
                    return "redirect:/inicio";
                })
                .orElse("redirect:/login?error");
    }

    // NUEVO: lo que pasa al iniciar sesión (con correo y contraseña, o con
    // Google) vive aquí, en un solo lugar, en vez de estar copiado en los
    // dos métodos. Y hace dos cosas de seguridad:
    //
    //  1. Cambia el id de la sesión. Así nadie puede dejar "preparada" una
    //     sesión (session fixation) y aprovecharla cuando la víctima inicie
    //     sesión con ese mismo id.
    //  2. Guarda en la sesión una copia del usuario SIN la contraseña
    //     cifrada. Antes se guardaba el Usuario completo, hash incluido, y
    //     no hay ningún motivo para que ese dato viaje en la sesión: las
    //     pantallas solo necesitan el id, el nombre y el correo.
    private void abrirSesion(
            HttpServletRequest request,
            HttpSession session,
            Usuario usuario) {

        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        Usuario usuarioEnSesion = new Usuario();
        usuarioEnSesion.setId(usuario.getId());
        usuarioEnSesion.setName(usuario.getName());
        usuarioEnSesion.setEmail(usuario.getEmail());

        session.setAttribute("usuario", usuarioEnSesion);

        int pendientes = miembroGrupoRepository.findByUsuarioIdAndEstado(
                usuario.getId(),
                EstadoMiembro.PENDIENTE
        ).size();
        session.setAttribute("invitacionesPendientes", pendientes);
    }

    // ==========================================
    // AUTENTICACIÓN GOOGLE SIGN-IN (OPCIÓN 1)
    // ==========================================
    @PostMapping("/auth/google")
    public String autenticarConGoogle(
            @RequestParam(name = "credential", required = false) String credential,
            HttpServletRequest request,
            HttpSession session) {

        if (credential == null || credential.isBlank()) {
            return "redirect:/login?error=google";
        }

        return googleAuthService.verificarToken(credential)
                .map(googleUser -> {
                    String email = googleUser.email();
                    String name = googleUser.name();

                    // Buscar usuario existente o registrar uno nuevo automáticamente
                    Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                            .orElseGet(() -> {
                                Usuario nuevo = new Usuario();
                                nuevo.setName(name);
                                nuevo.setEmail(email);
                                nuevo.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                                return usuarioRepository.save(nuevo);
                            });

                    abrirSesion(request, session, usuario);

                    return "redirect:/inicio";
                })
                .orElse("redirect:/login?error=google");
    }

    // CAMBIO: antes era GET. Con GET, cualquier enlace o imagen escondida en
    // otra página (<img src=".../logout">) podía cerrarle la sesión a
    // alguien sin que lo pidiera. Cerrar sesión cambia el estado, así que
    // tiene que ser POST (en inicio.html ahora es un formulario con botón).
    @PostMapping("/logout")
    public String cerrarSesion(HttpSession session) {

        session.invalidate();

        return "redirect:/login";
    }

    @GetMapping("/inicio")
    public String mostrarInicio(HttpSession session, Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        List<MiembroGrupo> misGrupos =
                miembroGrupoRepository.findByUsuarioIdAndEstado(
                        usuario.getId(),
                        EstadoMiembro.MIEMBRO
                );

        List<MiembroGrupo> gruposDestacados =
                misGrupos.size() > 4
                        ? misGrupos.subList(0, 4)
                        : misGrupos;

        List<Grupo> grupos = new ArrayList<>();
        for (MiembroGrupo miembro : misGrupos) {
            grupos.add(miembro.getGrupo());
        }

        // CAMBIO: el balance de cada grupo se calcula UNA sola vez y se
        // reutiliza. Antes, los grupos destacados se calculaban dos veces
        // (una para el balance general y otra para su tarjeta), y cada
        // cálculo hace varias consultas a la base de datos.
        Map<Long, BigDecimal> balancePorGrupo =
                deudaService.calcularBalancesPorGrupo(grupos, usuario);

        BalanceGeneral balanceGeneral =
                deudaService.resumirBalances(balancePorGrupo.values());

        Map<Long, Integer> miembrosPorGrupo = new HashMap<>();

        for (MiembroGrupo miembro : gruposDestacados) {

            int cantidadMiembros = miembroGrupoRepository.findByGrupoIdAndEstado(
                    miembro.getGrupo().getId(),
                    EstadoMiembro.MIEMBRO
            ).size();
            miembrosPorGrupo.put(miembro.getGrupo().getId(), cantidadMiembros);
        }

        int invitacionesPendientes = miembroGrupoRepository.findByUsuarioIdAndEstado(
                usuario.getId(),
                EstadoMiembro.PENDIENTE
        ).size();

        session.setAttribute("invitacionesPendientes", invitacionesPendientes);
        model.addAttribute("gruposDestacados", gruposDestacados);
        model.addAttribute("balanceGeneral", balanceGeneral);
        model.addAttribute("balancePorGrupo", balancePorGrupo);
        model.addAttribute("miembrosPorGrupo", miembrosPorGrupo);
        model.addAttribute("invitacionesPendientes", invitacionesPendientes);
        model.addAttribute("googleClientId", googleAuthService.getGoogleClientId());

        return "inicio";
    }
}