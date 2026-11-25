package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.CuentaCanal;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.service.telegram.TelegramAccountLinkService;
import com.gestorgastos.gestor_gastos.service.telegram.TelegramBotClient;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class IntegracionesController {

    @Value("${nexus.telegram.bot-username:}")
    private String botUsername;

    @Value("${nexus.telegram.groups-enabled:true}")
    private boolean groupsEnabled;

    private final TelegramAccountLinkService linkService;
    private final TelegramBotClient botClient;

    public IntegracionesController(
            TelegramAccountLinkService linkService,
            TelegramBotClient botClient) {
        this.linkService = linkService;
        this.botClient = botClient;
    }

    @GetMapping("/integraciones")
    public String mostrarIntegraciones(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        CuentaCanal cuentaTelegram = linkService.obtenerCuentaActivaPorUsuario(usuario.getId()).orElse(null);

        model.addAttribute("telegramHabilitado", botClient.isConfigured());
        model.addAttribute("groupsEnabled", groupsEnabled);
        model.addAttribute("botUsername", botUsername);
        model.addAttribute("cuentaTelegram", cuentaTelegram);

        return "integraciones";
    }

    @PostMapping("/integraciones/telegram/codigo")
    public String generarCodigoTelegram(HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        String codigo = linkService.generarCodigoTemporal(usuario);
        redirectAttributes.addFlashAttribute("codigoTemporal", codigo);
        if (botUsername != null && !botUsername.isBlank()) {
            String cleanUser = botUsername.startsWith("@") ? botUsername.substring(1) : botUsername;
            redirectAttributes.addFlashAttribute("deepLink", "https://t.me/" + cleanUser + "?start=" + codigo);
        }

        return "redirect:/integraciones";
    }

    @PostMapping("/integraciones/telegram/desvincular")
    public String desvincularTelegram(HttpSession session, RedirectAttributes redirectAttributes) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        linkService.desvincularUsuario(usuario);
        redirectAttributes.addFlashAttribute("mensajeIntegracion", "Telegram desvinculado correctamente.");
        return "redirect:/integraciones";
    }
}
