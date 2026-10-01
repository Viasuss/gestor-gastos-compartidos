package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.service.BalanceGeneral;
import com.gestorgastos.gestor_gastos.service.DeudaService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.List;

@Controller
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final MiembroGrupoRepository miembroGrupoRepository;
    // NUEVO: para calcular el balance general de la tarjeta de inicio.html
    private final DeudaService deudaService;

    public UsuarioController(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder,
            MiembroGrupoRepository miembroGrupoRepository,
            DeudaService deudaService) {

        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.deudaService = deudaService;
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {

        model.addAttribute("usuario", new Usuario());

        return "registro";
    }

    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario) {

        String passwordCifrada =
                passwordEncoder.encode(usuario.getPassword());

        usuario.setPassword(passwordCifrada);

        usuarioRepository.save(usuario);

        return "redirect:/login";
    }

    @GetMapping("/login")
    public String mostrarLogin() {

        return "login";
    }

    @PostMapping("/login")
    public String iniciarSesion(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session) {

        return usuarioRepository.findByEmail(email)
                .filter(usuario -> passwordEncoder.matches(
                        password,
                        usuario.getPassword()
                ))
                .map(usuario -> {
                    session.setAttribute("usuario", usuario);
                    return "redirect:/inicio";
                })
                .orElse("redirect:/login?error");
    }
    @GetMapping("/logout")
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
                misGrupos.size() > 2
                        ? misGrupos.subList(0, 2)
                        : misGrupos;

        // NUEVO: para calcular el balance general, DeudaService necesita
        // la lista de objetos Grupo (no de MiembroGrupo). Como misGrupos
        // es List<MiembroGrupo>, sacamos el .getGrupo() de cada uno.
        List<Grupo> grupos = new ArrayList<>();
        for (MiembroGrupo miembro : misGrupos) {
            grupos.add(miembro.getGrupo());
        }

        BalanceGeneral balanceGeneral =
                deudaService.calcularBalanceGeneral(grupos, usuario);

        model.addAttribute("gruposDestacados", gruposDestacados);
        model.addAttribute("balanceGeneral", balanceGeneral);

        return "inicio";
    }
}