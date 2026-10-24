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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final MiembroGrupoRepository miembroGrupoRepository;
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
    public String mostrarLogin() {

        return "login";
    }

    @PostMapping("/login")
    public String iniciarSesion(
            @RequestParam String email,
            @RequestParam String password,
            HttpSession session) {

        String emailNormalizado = email != null ? email.trim() : "";

        return usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
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
                misGrupos.size() > 4
                        ? misGrupos.subList(0, 4)
                        : misGrupos;

        List<Grupo> grupos = new ArrayList<>();
        for (MiembroGrupo miembro : misGrupos) {
            grupos.add(miembro.getGrupo());
        }

        BalanceGeneral balanceGeneral =
                deudaService.calcularBalanceGeneral(grupos, usuario);

        Map<Long, BigDecimal> balancePorGrupo = new HashMap<>();
        Map<Long, Integer> miembrosPorGrupo = new HashMap<>();

        for (MiembroGrupo miembro : gruposDestacados) {
            BigDecimal balance = deudaService.calcularBalanceTotal(
                    miembro.getGrupo(),
                    usuario
            );
            balancePorGrupo.put(miembro.getGrupo().getId(), balance);

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

        return "inicio";
    }
}
