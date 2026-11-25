package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.EstadoBorrador;
import com.gestorgastos.gestor_gastos.entity.EstadoLiquidacion;
import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.BorradorOperacionRepository;
import com.gestorgastos.gestor_gastos.repository.LiquidacionRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.security.LoginAttemptService;
import com.gestorgastos.gestor_gastos.service.BalanceGeneral;
import com.gestorgastos.gestor_gastos.service.DeudaService;
import com.gestorgastos.gestor_gastos.service.GoogleAuthService;
import com.gestorgastos.gestor_gastos.service.telegram.TelegramAccountLinkService;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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
    private final LoginAttemptService loginAttemptService;
    private final BorradorOperacionRepository borradorOperacionRepository;
    private final LiquidacionRepository liquidacionRepository;
    private final TelegramAccountLinkService telegramAccountLinkService;

    public UsuarioController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            MiembroGrupoRepository miembroGrupoRepository,
            DeudaService deudaService,
            GoogleAuthService googleAuthService,
            LoginAttemptService loginAttemptService,
            BorradorOperacionRepository borradorOperacionRepository,
            LiquidacionRepository liquidacionRepository,
            TelegramAccountLinkService telegramAccountLinkService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.deudaService = deudaService;
        this.googleAuthService = googleAuthService;
        this.loginAttemptService = loginAttemptService;
        this.borradorOperacionRepository = borradorOperacionRepository;
        this.liquidacionRepository = liquidacionRepository;
        this.telegramAccountLinkService = telegramAccountLinkService;
    }

    @GetMapping("/")
    public String redirigirRaiz(HttpSession session) {
        return session.getAttribute("usuario") != null ? "redirect:/inicio" : "redirect:/login";
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
                usuario.getPassword() == null || usuario.getPassword().isBlank() ||
                usuario.getPassword().length() < 8) {
            return "redirect:/registro?error=campos";
        }

        String emailNormalizado = usuario.getEmail().trim().toLowerCase();
        if (usuarioRepository.findByEmailIgnoreCase(emailNormalizado).isPresent()) {
            return "redirect:/registro?error=existe";
        }

        usuario.setName(usuario.getName().trim());
        usuario.setEmail(emailNormalizado);
        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setAuthProvider("LOCAL");
        usuario.setEmailVerified(false);

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

        String emailNormalizado = email != null ? email.trim().toLowerCase() : "";

        if (loginAttemptService.isBlocked(emailNormalizado)) {
            return "redirect:/login?error";
        }

        return usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                .filter(usuario -> passwordEncoder.matches(
                        password,
                        usuario.getPassword()
                ))
                .map(usuario -> {
                    loginAttemptService.recordSuccess(emailNormalizado);
                    abrirSesion(request, session, usuario);
                    return "redirect:/inicio";
                })
                .orElseGet(() -> {
                    loginAttemptService.recordFailure(emailNormalizado);
                    return "redirect:/login?error";
                });
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
        usuarioEnSesion.setGoogleSub(usuario.getGoogleSub());
        usuarioEnSesion.setAuthProvider(usuario.getAuthProvider());
        usuarioEnSesion.setEmailVerified(usuario.isEmailVerified());

        session.setAttribute("usuario", usuarioEnSesion);

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                usuarioEnSesion,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

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
                    String sub = googleUser.sub();
                    String email = googleUser.email();
                    String name = googleUser.name();

                    // 1. Buscar por identificador estable de Google (sub)
                    Usuario usuario = usuarioRepository.findByGoogleSub(sub).orElse(null);

                    if (usuario == null) {
                        // 2. Mitigacion de Account Pre-hijacking (HA-002):
                        // Nunca vincular automaticamente una cuenta LOCAL no verificada por simple coincidencia de correo.
                        Usuario existentePorEmail = usuarioRepository.findByEmailIgnoreCase(email).orElse(null);
                        if (existentePorEmail != null) {
                            return "redirect:/login?error=google";
                        }

                        Usuario nuevo = new Usuario();
                        nuevo.setName(name);
                        nuevo.setEmail(email);
                        nuevo.setGoogleSub(sub);
                        nuevo.setAuthProvider("GOOGLE");
                        nuevo.setEmailVerified(true);
                        nuevo.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                        usuario = usuarioRepository.save(nuevo);
                    }

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

        long borradoresPendientesCount = borradorOperacionRepository
                .findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId())
                .stream()
                .filter(b -> b.getEstado() == EstadoBorrador.PENDIENTE
                        || b.getEstado() == EstadoBorrador.REQUIERE_ACLARACION)
                .count();

        int liquidacionesPendientesCount = 0;
        for (Grupo grupo : grupos) {
            liquidacionesPendientesCount += liquidacionRepository
                    .findByGrupoIdAndEstado(grupo.getId(), EstadoLiquidacion.PENDIENTE)
                    .size();
        }

        boolean telegramVinculado = telegramAccountLinkService
                .obtenerCuentaActivaPorUsuario(usuario.getId())
                .isPresent();

        session.setAttribute("invitacionesPendientes", invitacionesPendientes);
        model.addAttribute("gruposDestacados", gruposDestacados);
        model.addAttribute("totalGruposActivos", misGrupos.size());
        model.addAttribute("balanceGeneral", balanceGeneral);
        model.addAttribute("balancePorGrupo", balancePorGrupo);
        model.addAttribute("miembrosPorGrupo", miembrosPorGrupo);
        model.addAttribute("invitacionesPendientes", invitacionesPendientes);
        model.addAttribute("borradoresPendientesCount", borradoresPendientesCount);
        model.addAttribute("liquidacionesPendientesCount", liquidacionesPendientesCount);
        model.addAttribute("telegramVinculado", telegramVinculado);
        model.addAttribute("googleClientId", googleAuthService.getGoogleClientId());
        model.addAttribute("googleVinculado", usuario.getGoogleSub() != null);

        return "inicio";
    }
}