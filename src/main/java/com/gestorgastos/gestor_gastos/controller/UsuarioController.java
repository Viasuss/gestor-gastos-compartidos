package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.crypto.password.PasswordEncoder;
import jakarta.servlet.http.HttpSession;

@Controller
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
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
    public String mostrarInicio(HttpSession session) {

        if (session.getAttribute("usuario") == null) {
            return "redirect:/login";
        }

        return "inicio";
    }
}