package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.entity.PagoGasto;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.repository.PagoGastoRepository;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.service.DeudaConPersona;
import com.gestorgastos.gestor_gastos.service.DeudaService;


import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class MiembroGrupoController {

    private final MiembroGrupoRepository miembroGrupoRepository;
    private final GrupoRepository grupoRepository;
    private final GastoRepository gastoRepository;
    private final DeudaService deudaService;
    // NUEVO (Sprint 6, ampliación): para traer los pagadores de cada gasto
    private final PagoGastoRepository pagoGastoRepository;

    public MiembroGrupoController(
            MiembroGrupoRepository miembroGrupoRepository,
            GrupoRepository grupoRepository,
            GastoRepository gastoRepository,
            DeudaService deudaService,
            PagoGastoRepository pagoGastoRepository) {

        this.miembroGrupoRepository = miembroGrupoRepository;
        this.grupoRepository = grupoRepository;
        this.gastoRepository = gastoRepository;
        this.deudaService = deudaService;
        this.pagoGastoRepository = pagoGastoRepository;
    }

    @GetMapping("/mis-grupos")
    public String mostrarMisGrupos(HttpSession session, Model model) {

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        List<MiembroGrupo> miembros =
                miembroGrupoRepository.findByUsuarioId(usuario.getId());

        // NUEVO: balance real por grupo, para mostrarlo en mis-grupos.html
        // (como en el Figma: "-$75.000" / "+$210.000" junto a cada grupo).
        // Armamos un mapa "id del grupo -> balance", porque solo tiene
        // sentido calcular la deuda de los grupos donde el usuario ya
        // es MIEMBRO (no de invitaciones pendientes o rechazadas, que
        // también vienen en "miembros").
        Map<Long, BigDecimal> balancePorGrupo = new HashMap<>();

        for (MiembroGrupo miembro : miembros) {

            if (miembro.getEstado() == EstadoMiembro.MIEMBRO) {

                BigDecimal balance = deudaService.calcularBalanceTotal(
                        miembro.getGrupo(),
                        usuario
                );

                balancePorGrupo.put(miembro.getGrupo().getId(), balance);
            }
        }

        model.addAttribute("balancePorGrupo", balancePorGrupo);

        model.addAttribute("miembros", miembros);

        return "mis-grupos";
    }
    @GetMapping("/grupo/{id}")
    public String verGrupo(
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

        Grupo grupo =
                grupoRepository.findById(id).orElse(null);

        if (grupo == null) {
            return "redirect:/mis-grupos";
        }

        List<MiembroGrupo> miembros =
                miembroGrupoRepository.findByGrupoId(id);

        List<Gasto> gastos =
                gastoRepository.findByGrupoOrderByFechaDesc(grupo);

        // NUEVO (Sprint 6, ampliación): como un Gasto ya no tiene un solo
        // "usuario" (pagador), armamos aquí un mapa "id del gasto -> lista
        // de sus pagos", para que grupo.html pueda mostrar, en el historial,
        // quién o quiénes pagaron cada gasto y cuánto puso cada uno.
        Map<Long, List<PagoGasto>> pagosPorGasto = new HashMap<>();

        for (Gasto gasto : gastos) {
            List<PagoGasto> pagos =
                    pagoGastoRepository.findByGastoId(gasto.getId());

            pagosPorGasto.put(gasto.getId(), pagos);
        }

        List<DeudaConPersona> deudas =
                deudaService.calcularDeudas(grupo, usuario);

        BigDecimal balanceTotal =
                deudaService.calcularBalanceTotal(grupo, usuario);

        model.addAttribute("grupo", grupo);
        model.addAttribute("miembros", miembros);
        model.addAttribute("gastos", gastos);
        model.addAttribute("pagosPorGasto", pagosPorGasto);
        model.addAttribute("deudas", deudas);
        model.addAttribute("balanceTotal", balanceTotal);

        return "grupo";
    }
}