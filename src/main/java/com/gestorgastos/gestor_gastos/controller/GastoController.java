package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.entity.GastoParticipante;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.PagoGasto;
import com.gestorgastos.gestor_gastos.entity.Usuario;

import com.gestorgastos.gestor_gastos.repository.GastoParticipanteRepository;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.PagoGastoRepository;

import com.gestorgastos.gestor_gastos.service.GastoService;
import com.gestorgastos.gestor_gastos.service.MoneyParser;

import jakarta.servlet.http.HttpSession;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Controller
public class GastoController {

    // Límites de validación (el servidor NO confía en lo que valida el
    // formulario: cualquiera puede editar el HTML desde el navegador)
    private static final BigDecimal MONTO_MAXIMO = new BigDecimal("999999999999");
    private static final int DESCRIPCION_MAXIMA = 100;

    private final GrupoRepository grupoRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final GastoService gastoService;

    public GastoController(
            GrupoRepository grupoRepository,
            MiembroGrupoRepository miembroGrupoRepository,
            GastoService gastoService) {

        this.grupoRepository = grupoRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.gastoService = gastoService;
    }


    // ==========================================
    // MOSTRAR FORMULARIO PARA AGREGAR GASTO
    // ==========================================

    @GetMapping("/grupo/{id}/gasto/nuevo")
    public String mostrarFormularioGasto(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        Optional<MiembroGrupo> relacion =
                miembroGrupoRepository.findByUsuarioIdAndGrupoId(
                        usuario.getId(),
                        id
                );

        if (relacion.isEmpty() ||
                relacion.get().getEstado() != EstadoMiembro.MIEMBRO) {

            return "redirect:/mis-grupos";
        }

        Grupo grupo = grupoRepository.findById(id).orElse(null);

        if (grupo == null) {
            return "redirect:/mis-grupos";
        }

        List<MiembroGrupo> miembros =
                miembroGrupoRepository.findByGrupoIdAndEstado(
                        id,
                        EstadoMiembro.MIEMBRO
                );

        model.addAttribute("grupo", grupo);
        model.addAttribute("miembros", miembros);
        model.addAttribute("idempotencyKey", java.util.UUID.randomUUID().toString());

        return "agregar-gasto";
    }


    // ==========================================
    // GUARDAR EL GASTO
    // ==========================================

    // CAMBIO (revisión de calidad): ahora TODO se valida ANTES de guardar
    // cualquier cosa. Antes pasaban dos cosas malas:
    //
    //  1. Si un pagador no era miembro del grupo, se saltaba en silencio,
    //     pero la validación de la suma ya había contado su monto: el gasto
    //     quedaba guardado con pagos que NO sumaban el total.
    //  2. Si ningún participante era válido, el gasto se guardaba sin
    //     participantes y el cálculo de deudas lo ignoraba sin avisar.
    //
    // Además, antes los @RequestParam eran obligatorios: si no se marcaba
    // ningún pagador, el navegador no enviaba esos campos y el usuario veía
    // una página de error 400. Ahora son opcionales y se responde con un
    // mensaje claro (redirect con ?error=...).
    //
    // Como los errores son "return redirect" (no excepciones), @Transactional
    // no los revertiría: por eso la regla es validar primero, guardar después.
    @PostMapping("/grupo/{id}/gasto")
    public String guardarGasto(
            @PathVariable Long id,
            @RequestParam(required = false) String descripcion,
            // El monto llega como texto estilo Nequi ("$50.000"): lo
            // convertimos nosotros con MoneyParser.parseCop(...)
            @RequestParam(required = false) String monto,
            @RequestParam(required = false) String fecha,
            // Dos listas PARALELAS: la posición 0 de pagadoresId corresponde
            // a la posición 0 de montosPagados, y así sucesivamente.
            @RequestParam(required = false) List<Long> pagadoresId,
            @RequestParam(required = false) List<String> montosPagados,
            @RequestParam(required = false) List<Long> participantes,
            @RequestParam(required = false) String idempotencyKey,
            RedirectAttributes redirectAttributes,
            HttpSession session) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");
        if (usuario == null) {
            return "redirect:/login";
        }

        String errorBase = "redirect:/grupo/" + id + "/gasto/nuevo?error=";
        redirectAttributes.addFlashAttribute("prevDescripcion", descripcion);
        redirectAttributes.addFlashAttribute("prevMonto", monto);
        redirectAttributes.addFlashAttribute("prevFecha", fecha);

        BigDecimal montoTotal = MoneyParser.parseCop(monto).orElse(BigDecimal.ZERO);
        LocalDate fechaGasto;
        try {
            fechaGasto = LocalDate.parse(fecha == null ? "" : fecha.trim());
        } catch (DateTimeParseException e) {
            return errorBase + "fecha";
        }

        List<BigDecimal> montosConvertidos = new ArrayList<>();
        if (montosPagados != null) {
            for (String mp : montosPagados) {
                montosConvertidos.add(MoneyParser.parseCop(mp).orElse(BigDecimal.ZERO));
            }
        }

        try {
            gastoService.registrarGasto(
                    id, usuario, descripcion, montoTotal, fechaGasto,
                    pagadoresId, montosConvertidos, participantes, idempotencyKey
            );
            return "redirect:/grupo/" + id;
        } catch (GastoService.GastoValidationException ex) {
            if ("acceso".equals(ex.getErrorCode())) {
                return "redirect:/mis-grupos";
            }
            return errorBase + ex.getErrorCode();
        }
    }


    // ==========================================
    // CONVERTIR TEXTO FORMATEADO A MONTO REAL
    // ==========================================

    // El formulario envía el monto tal como lo ve el usuario, estilo
    // Nequi: por ejemplo "$50.000" (con símbolo de pesos y puntos de
    // miles, puestos por el JavaScript mientras escribía). Aquí quitamos
    // TODO lo que no sea un dígito y armamos el BigDecimal con lo que
    // quede. El proyecto solo maneja pesos enteros (sin centavos).
    private BigDecimal convertirAMontoReal(String textoFormateado) {

        if (textoFormateado == null || textoFormateado.isBlank()) {
            return BigDecimal.ZERO;
        }

        String soloDigitos = textoFormateado.replaceAll("[^0-9]", "");

        if (soloDigitos.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return new BigDecimal(soloDigitos);
    }
}