package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.Liquidacion;
import com.gestorgastos.gestor_gastos.entity.MetodoPago;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.LiquidacionRepository;
import com.gestorgastos.gestor_gastos.security.GrupoAccessService;
import com.gestorgastos.gestor_gastos.service.DeudaConPersona;
import com.gestorgastos.gestor_gastos.service.DeudaService;
import com.gestorgastos.gestor_gastos.service.LiquidacionService;
import com.gestorgastos.gestor_gastos.service.MoneyParser;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
public class LiquidacionController {

    private final LiquidacionService liquidacionService;
    private final LiquidacionRepository liquidacionRepository;
    private final GrupoAccessService grupoAccessService;
    private final DeudaService deudaService;

    public LiquidacionController(
            LiquidacionService liquidacionService,
            LiquidacionRepository liquidacionRepository,
            GrupoAccessService grupoAccessService,
            DeudaService deudaService) {
        this.liquidacionService = liquidacionService;
        this.liquidacionRepository = liquidacionRepository;
        this.grupoAccessService = grupoAccessService;
        this.deudaService = deudaService;
    }

    @GetMapping("/grupo/{id}/liquidar")
    public String mostrarFormularioLiquidacion(
            @PathVariable Long id,
            @RequestParam(required = false) Long receptorId,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Grupo grupo = grupoAccessService.findGrupoSiEsMiembroActivo(id, usuario).orElse(null);
        if (grupo == null) {
            return "redirect:/mis-grupos";
        }

        List<DeudaConPersona> deudasPorPagar = deudaService.calcularDeudas(grupo, usuario).stream()
                .filter(DeudaConPersona::leDebo)
                .toList();

        model.addAttribute("grupo", grupo);
        model.addAttribute("deudasPorPagar", deudasPorPagar);
        model.addAttribute("metodosPago", MetodoPago.values());
        model.addAttribute("receptorPreseleccionado", receptorId);
        model.addAttribute("idempotencyKey", UUID.randomUUID().toString());

        return "liquidar";
    }

    @PostMapping("/grupo/{id}/liquidar")
    public String registrarLiquidacion(
            @PathVariable Long id,
            @RequestParam(required = false) Long receptorId,
            @RequestParam(required = false) String monto,
            @RequestParam(required = false) MetodoPago metodoPago,
            @RequestParam(required = false) String nota,
            @RequestParam(required = false) String idempotencyKey,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        redirectAttributes.addFlashAttribute("prevMonto", monto);
        redirectAttributes.addFlashAttribute("prevNota", nota);
        redirectAttributes.addFlashAttribute("prevReceptorId", receptorId);
        redirectAttributes.addFlashAttribute("prevMetodo", metodoPago);

        BigDecimal montoReal = MoneyParser.parseCop(monto).orElse(null);
        if (montoReal == null || montoReal.signum() <= 0) {
            redirectAttributes.addFlashAttribute("errorMensaje", "Ingresa un monto válido en pesos mayor a $0.");
            return "redirect:/grupo/" + id + "/liquidar?error=monto";
        }

        try {
            liquidacionService.registrarSolicitud(
                    id, usuario, receptorId, montoReal, metodoPago, nota, idempotencyKey
            );
            return "redirect:/grupo/" + id + "?liquidacion=pendiente";
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("errorMensaje", ex.getMessage());
            return "redirect:/grupo/" + id + "/liquidar?error=validacion";
        }
    }

    @PostMapping("/liquidacion/{id}/confirmar")
    public String confirmarLiquidacion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Liquidacion liq = liquidacionRepository.findById(id).orElse(null);
        if (liq == null) {
            return "redirect:/mis-grupos";
        }
        Long grupoId = liq.getGrupo().getId();

        try {
            liquidacionService.confirmar(id, usuario);
            return "redirect:/grupo/" + grupoId + "?liquidacion=confirmada";
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorLiquidacion", ex.getMessage());
            return "redirect:/grupo/" + grupoId + "?error=liquidacion";
        }
    }

    @PostMapping("/liquidacion/{id}/rechazar")
    public String rechazarLiquidacion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Liquidacion liq = liquidacionRepository.findById(id).orElse(null);
        if (liq == null) {
            return "redirect:/mis-grupos";
        }
        Long grupoId = liq.getGrupo().getId();

        try {
            liquidacionService.rechazar(id, usuario);
            return "redirect:/grupo/" + grupoId + "?liquidacion=rechazada";
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorLiquidacion", ex.getMessage());
            return "redirect:/grupo/" + grupoId + "?error=liquidacion";
        }
    }

    @PostMapping("/liquidacion/{id}/cancelar")
    public String cancelarLiquidacion(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        Liquidacion liq = liquidacionRepository.findById(id).orElse(null);
        if (liq == null) {
            return "redirect:/mis-grupos";
        }
        Long grupoId = liq.getGrupo().getId();

        try {
            liquidacionService.cancelar(id, usuario);
            return "redirect:/grupo/" + grupoId + "?liquidacion=cancelada";
        } catch (IllegalArgumentException | IllegalStateException | SecurityException ex) {
            redirectAttributes.addFlashAttribute("errorLiquidacion", ex.getMessage());
            return "redirect:/grupo/" + grupoId + "?error=liquidacion";
        }
    }
}
