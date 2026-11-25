package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.*;
import com.gestorgastos.gestor_gastos.repository.BorradorOperacionRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.service.BorradorOperacionService;
import com.gestorgastos.gestor_gastos.service.GastoService;
import com.gestorgastos.gestor_gastos.service.MoneyParser;
import com.gestorgastos.gestor_gastos.service.ia.GroqProvider;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@Controller
public class BorradorOperacionController {

    private final BorradorOperacionRepository borradorRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final BorradorOperacionService borradorService;
    private final GroqProvider groqProvider;

    public BorradorOperacionController(
            BorradorOperacionRepository borradorRepository,
            MiembroGrupoRepository miembroGrupoRepository,
            BorradorOperacionService borradorService,
            GroqProvider groqProvider) {
        this.borradorRepository = borradorRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.borradorService = borradorService;
        this.groqProvider = groqProvider;
    }

    @GetMapping({"/asistente", "/bandeja"})
    public String verAsistente(HttpSession session, Model model) {
        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        List<MiembroGrupo> misGrupos = miembroGrupoRepository.findByUsuarioIdAndEstado(
                usuario.getId(), EstadoMiembro.MIEMBRO);

        List<BorradorOperacion> borradores = borradorRepository.findByUsuarioIdOrderByFechaCreacionDesc(usuario.getId());

        Map<Long, List<MiembroGrupo>> miembrosPorGrupo = new HashMap<>();
        for (MiembroGrupo mg : misGrupos) {
            Long gId = mg.getGrupo().getId();
            miembrosPorGrupo.put(gId, miembroGrupoRepository.findByGrupoIdAndEstado(gId, EstadoMiembro.MIEMBRO));
        }

        model.addAttribute("misGrupos", misGrupos);
        model.addAttribute("borradores", borradores);
        model.addAttribute("miembrosPorGrupo", miembrosPorGrupo);
        model.addAttribute("groqDisponible", groqProvider.isAvailable());
        model.addAttribute("usuarioActualId", usuario.getId());
        model.addAttribute("idempotencyKey", UUID.randomUUID().toString());

        return "bandeja";
    }

    @PostMapping("/asistente/interpretar")
    public String interpretarInstruccion(
            @RequestParam Long grupoId,
            @RequestParam String texto,
            @RequestParam(required = false) String idempotencyKey,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        if (texto == null || texto.isBlank()) {
            redirectAttributes.addFlashAttribute("errorBandeja", "Escribe una instrucción financiera para interpretar.");
            return "redirect:/asistente";
        }

        try {
            borradorService.crearDesdeTexto(usuario, grupoId, texto, idempotencyKey);
            return "redirect:/asistente?interpretado=true";
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorBandeja", ex.getMessage());
            return "redirect:/asistente";
        }
    }

    @PostMapping("/bandeja/{id}/confirmar")
    public String confirmarBorrador(
            @PathVariable Long id,
            @RequestParam String concepto,
            @RequestParam String montoCop,
            @RequestParam String fecha,
            @RequestParam Long pagadorId,
            @RequestParam(required = false) List<Long> participantesIds,
            @RequestParam(required = false) String confirmIdempotencyKey,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Optional<BigDecimal> montoOpt = MoneyParser.parseCopAcceptingZeroDecimals(montoCop);
        if (montoOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorBandeja", "Monto inválido. Ingresa un valor entero en COP (ej: 60000).");
            return "redirect:/asistente";
        }
        BigDecimal monto = montoOpt.get();
        LocalDate fechaParsed;
        try {
            fechaParsed = LocalDate.parse(fecha.trim());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorBandeja", "Fecha inválida.");
            return "redirect:/asistente";
        }

        try {
            Gasto gasto = borradorService.confirmarBorrador(
                    id, usuario, concepto, monto, fechaParsed, pagadorId, participantesIds, confirmIdempotencyKey);
            return "redirect:/grupo/" + gasto.getGrupo().getId() + "?gastoCreado=true";
        } catch (GastoService.GastoValidationException ex) {
            redirectAttributes.addFlashAttribute("errorBandeja", ex.getMessage());
            return "redirect:/asistente";
        } catch (IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorBandeja", ex.getMessage());
            return "redirect:/asistente";
        }
    }

    @PostMapping("/bandeja/{id}/descartar")
    public String descartarBorrador(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        try {
            borradorService.descartarBorrador(id, usuario);
            return "redirect:/asistente?descartado=true";
        } catch (IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorBandeja", ex.getMessage());
            return "redirect:/asistente";
        }
    }
}
