package com.gestorgastos.gestor_gastos.controller;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.entity.GastoParticipante;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;

import com.gestorgastos.gestor_gastos.repository.GastoParticipanteRepository;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;

import jakarta.servlet.http.HttpSession;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class GastoController {

    private final GastoRepository gastoRepository;
    private final GastoParticipanteRepository gastoParticipanteRepository;
    private final GrupoRepository grupoRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;

    public GastoController(
            GastoRepository gastoRepository,
            GastoParticipanteRepository gastoParticipanteRepository,
            GrupoRepository grupoRepository,
            MiembroGrupoRepository miembroGrupoRepository) {

        this.gastoRepository = gastoRepository;
        this.gastoParticipanteRepository = gastoParticipanteRepository;
        this.grupoRepository = grupoRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
    }


    // ==========================================
    // MOSTRAR FORMULARIO PARA AGREGAR GASTO
    // ==========================================

    @GetMapping("/grupo/{id}/gasto/nuevo")
    public String mostrarFormularioGasto(
            @PathVariable Long id,
            HttpSession session,
            Model model) {

        // 1. Verificar sesión
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        // 2. Verificar que el usuario en sesión sea MIEMBRO de este grupo
        //    (mismo patrón que usa MiembroGrupoController.verGrupo)
        Optional<MiembroGrupo> relacion =
                miembroGrupoRepository.findByUsuarioIdAndGrupoId(
                        usuario.getId(),
                        id
                );

        if (relacion.isEmpty() ||
                relacion.get().getEstado() != EstadoMiembro.MIEMBRO) {

            return "redirect:/mis-grupos";
        }

        // 3. Buscar el grupo
        Grupo grupo = grupoRepository.findById(id).orElse(null);

        if (grupo == null) {
            return "redirect:/mis-grupos";
        }

        // 4. Buscar solo los miembros que ya aceptaron (estado MIEMBRO)
        List<MiembroGrupo> miembros =
                miembroGrupoRepository.findByGrupoIdAndEstado(
                        id,
                        EstadoMiembro.MIEMBRO
                );

        // 5. Mandar todo a la vista
        model.addAttribute("grupo", grupo);
        model.addAttribute("miembros", miembros);

        return "agregar-gasto";
    }


    // ==========================================
    // GUARDAR EL GASTO
    // ==========================================

    @PostMapping("/grupo/{id}/gasto")
    public String guardarGasto(
            @PathVariable Long id,
            @RequestParam String descripcion,
            @RequestParam BigDecimal monto,
            @RequestParam LocalDate fecha,
            @RequestParam Long pagadorId,
            @RequestParam List<Long> participantes,
            HttpSession session) {

        // 1. Verificar sesión
        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        // 2. Verificar que el usuario en sesión sea MIEMBRO de este grupo
        Optional<MiembroGrupo> relacion =
                miembroGrupoRepository.findByUsuarioIdAndGrupoId(
                        usuario.getId(),
                        id
                );

        if (relacion.isEmpty() || relacion.get().getEstado() != EstadoMiembro.MIEMBRO) {

            return "redirect:/mis-grupos";
        }

        // 3. Buscar el grupo
        Grupo grupo = grupoRepository.findById(id).orElse(null);

        if (grupo == null) {
            return "redirect:/mis-grupos";
        }

        // 4. Buscar quién pagó (debe ser MIEMBRO del grupo)
        Optional<MiembroGrupo> relacionPagador =
                miembroGrupoRepository.findByUsuarioIdAndGrupoId(
                        pagadorId,
                        id
                );

        if (relacionPagador.isEmpty() || relacionPagador.get().getEstado() != EstadoMiembro.MIEMBRO) {

            return "redirect:/grupo/" + id + "/gasto/nuevo";
        }

        Usuario pagador = relacionPagador.get().getUsuario();

        // 5. Crear y guardar el Gasto (una sola fila)
        Gasto gasto = new Gasto();

        gasto.SetDescripcion(descripcion);
        gasto.SetMonto(monto);
        gasto.SetFecha(fecha);
        gasto.Setusuario(pagador);
        gasto.Setgrupo(grupo);

        gastoRepository.save(gasto);

        // 6. Por cada usuario seleccionado en "Dividir entre",
        //    crear y guardar un GastoParticipante
        for (Long participanteId : participantes) {

            Optional<MiembroGrupo> relacionParticipante =
                    miembroGrupoRepository.findByUsuarioIdAndGrupoId(
                            participanteId,
                            id
                    );

            // Solo agregamos participantes que de verdad son MIEMBRO del grupo
            if (relacionParticipante.isPresent() &&
                    relacionParticipante.get().getEstado() == EstadoMiembro.MIEMBRO) {

                Usuario participante = relacionParticipante.get().getUsuario();

                GastoParticipante gastoParticipante = new GastoParticipante();

                gastoParticipante.setUsuario(participante);
                gastoParticipante.setGrupo(grupo);
                gastoParticipante.setGasto(gasto);

                gastoParticipanteRepository.save(gastoParticipante);
            }
        }

        return "redirect:/grupo/" + id;
    }
}
