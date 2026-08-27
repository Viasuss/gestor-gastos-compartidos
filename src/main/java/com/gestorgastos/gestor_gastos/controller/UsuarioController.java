package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;

    public UsuarioController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/registro")
    public String mostrarRegistro(Model model) {

        model.addAttribute("usuario", new Usuario());

        return "registro";
    }
    @PostMapping("/registro")
    public String registrarUsuario(@ModelAttribute Usuario usuario) {

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
            @RequestParam String password) {

        return usuarioRepository.findByEmail(email)
                .filter(usuario -> usuario.getPassword().equals(password))
                .map(usuario -> "redirect:/inicio")
                .orElse("redirect:/login?error");
    }

}