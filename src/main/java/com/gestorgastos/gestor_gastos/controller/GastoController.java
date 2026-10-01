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
    // NUEVO (Sprint 6, ampliación): para guardar los pagos de un gasto
    private final PagoGastoRepository pagoGastoRepository;

    public GastoController(
            GastoRepository gastoRepository,
            GastoParticipanteRepository gastoParticipanteRepository,
            GrupoRepository grupoRepository,
            MiembroGrupoRepository miembroGrupoRepository,
            PagoGastoRepository pagoGastoRepository) {

        this.gastoRepository = gastoRepository;
        this.gastoParticipanteRepository = gastoParticipanteRepository;
        this.grupoRepository = grupoRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.pagoGastoRepository = pagoGastoRepository;
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

        return "agregar-gasto";
    }


    // ==========================================
    // GUARDAR EL GASTO
    // ==========================================

    @PostMapping("/grupo/{id}/gasto")
    public String guardarGasto(
            @PathVariable Long id,
            @RequestParam String descripcion,
            // CAMBIO: antes era "BigDecimal monto" directo. Como ahora
            // el formulario usa formato colombiano (punto de miles,
            // coma decimal: "50.000,50"), Spring NO puede convertir
            // ese texto a BigDecimal automáticamente (esperaría algo
            // tipo "50000.50"). Por eso recibimos texto plano y lo
            // convertimos nosotros mismos con convertirAFloat(...).
            @RequestParam String monto,
            @RequestParam LocalDate fecha,
            // CAMBIO (Sprint 6, ampliación): antes era "Long pagadorId"
            // (un solo pagador). Ahora son DOS listas PARALELAS: la
            // posición 0 de pagadoresId corresponde a la posición 0
            // de montosPagados, y así sucesivamente. Por ejemplo:
            //   pagadoresId = [5, 8]
            //   montosPagados = ["25.000", "25.000"]
            // significa: el usuario 5 puso 25.000 y el usuario 8 puso
            // otros 25.000.
            @RequestParam List<Long> pagadoresId,
            @RequestParam List<String> montosPagados,
            @RequestParam List<Long> participantes,
            HttpSession session) {

        // Convertimos el monto total de texto colombiano a BigDecimal
        // real, antes de usarlo en cualquier cálculo
        BigDecimal montoTotal = convertirAMontoReal(monto);

        Usuario usuario = (Usuario) session.getAttribute("usuario");

        if (usuario == null) {
            return "redirect:/login";
        }

        Optional<MiembroGrupo> relacion =
                miembroGrupoRepository.findByUsuarioIdAndGrupoId(
                        usuario.getId(),
                        id
                );

        if (relacion.isEmpty() || relacion.get().getEstado() != EstadoMiembro.MIEMBRO) {

            return "redirect:/mis-grupos";
        }

        Grupo grupo = grupoRepository.findById(id).orElse(null);

        if (grupo == null) {
            return "redirect:/mis-grupos";
        }

        // VALIDACIÓN NUEVA: las dos listas deben venir del mismo tamaño.
        // Si no, algo salió mal en el formulario (un pagador sin monto,
        // o viceversa), y es más seguro rechazar que adivinar.
        if (pagadoresId.size() != montosPagados.size()) {
            return "redirect:/grupo/" + id + "/gasto/nuevo?error=pagos";
        }

        // VALIDACIÓN NUEVA: la suma de lo que pusieron los pagadores
        // debe ser exactamente igual al monto total del gasto.
        // Si Johan dice que el mercado costó $50.000 pero los pagos
        // solo suman $40.000, hay un error de digitación en alguna
        // parte, y preferimos avisar antes de guardar datos inconsistentes.
        // Cada texto de montosPagados también se convierte del formato
        // colombiano antes de sumarlo.
        BigDecimal sumaPagos = BigDecimal.ZERO;
        for (String montoPagoTexto : montosPagados) {
            sumaPagos = sumaPagos.add(convertirAMontoReal(montoPagoTexto));
        }

        if (sumaPagos.compareTo(montoTotal) != 0) {
            return "redirect:/grupo/" + id + "/gasto/nuevo?error=suma";
        }

        // Crear y guardar el Gasto (ya sin el campo "usuario")
        Gasto gasto = new Gasto();

        gasto.setDescripcion(descripcion);
        gasto.setMonto(montoTotal);
        gasto.setFecha(fecha);
        gasto.setGrupo(grupo);

        gastoRepository.save(gasto);

        // NUEVO: guardar un PagoGasto por cada pagador, recorriendo
        // las dos listas EN PARALELO con la misma posición (índice "i")
        for (int i = 0; i < pagadoresId.size(); i++) {

            Long pagadorId = pagadoresId.get(i);
            BigDecimal montoPagado = convertirAMontoReal(montosPagados.get(i));

            Optional<MiembroGrupo> relacionPagador =
                    miembroGrupoRepository.findByUsuarioIdAndGrupoId(
                            pagadorId,
                            id
                    );

            // Solo se acepta como pagador a alguien que de verdad
            // sea MIEMBRO del grupo (misma validación de seguridad
            // que ya usábamos antes con el pagador único)
            if (relacionPagador.isPresent() &&
                    relacionPagador.get().getEstado() == EstadoMiembro.MIEMBRO) {

                Usuario pagador = relacionPagador.get().getUsuario();

                PagoGasto pago = new PagoGasto();

                pago.setGasto(gasto);
                pago.setUsuario(pagador);
                pago.setMonto(montoPagado);

                pagoGastoRepository.save(pago);
            }
        }

        // Guardar los participantes (igual que antes, sin cambios)
        for (Long participanteId : participantes) {

            Optional<MiembroGrupo> relacionParticipante =
                    miembroGrupoRepository.findByUsuarioIdAndGrupoId(
                            participanteId,
                            id
                    );

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


    // ==========================================
    // CONVERTIR TEXTO FORMATEADO A MONTO REAL
    // ==========================================

    // El formulario envía el monto tal como lo ve el usuario, estilo
    // Nequi: por ejemplo "$50.000" (con símbolo de pesos y puntos de
    // miles, puestos por el JavaScript mientras escribía). Spring no
    // puede convertir ese texto directo a BigDecimal, así que aquí
    // quitamos TODO lo que no sea un dígito (el "$", los puntos, o
    // cualquier espacio) y armamos el BigDecimal a partir de los
    // números puros que queden.
    //
    // Como el proyecto solo maneja pesos enteros (sin centavos, igual
    // que al transferir plata en Nequi), no hace falta manejar comas
    // ni decimales aquí: "$50.000" se limpia a "50000", y de ahí sale
    // directo el BigDecimal.
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