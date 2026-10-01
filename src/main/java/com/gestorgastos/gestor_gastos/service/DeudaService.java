package com.gestorgastos.gestor_gastos.service;

import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.entity.GastoParticipante;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.PagoGasto;
import com.gestorgastos.gestor_gastos.entity.Usuario;

import com.gestorgastos.gestor_gastos.repository.GastoParticipanteRepository;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.repository.PagoGastoRepository;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// @Service: capa de lógica de negocio, entre el controlador y los repositorios.
//
// CAMBIO IMPORTANTE (Sprint 6, ampliación): antes este servicio asumía
// "un gasto = un solo pagador que puso el 100%". Ahora un gasto puede
// tener VARIOS pagadores, cada uno con su propio monto (tabla PagoGasto).
//
// La nueva fórmula central, para cada persona en cada gasto, es:
//
//     saldo_en_este_gasto = lo_que_puso - lo_que_le_tocaba_pagar
//
// Donde "lo que le tocaba pagar" es monto_total / cantidad_de_participantes.
// Si el saldo es positivo, puso de más -> le deben esa diferencia.
// Si el saldo es negativo, puso de menos (o nada) -> debe esa diferencia.
//
// Como puede haber varias personas con saldo positivo y varias con saldo
// negativo en un mismo gasto (no un único "pagador" contra todos), hay
// que repartir esas deudas entre ellas. Usamos un reparto proporcional
// simple: cada "deudor" le paga a cada "acreedor" en proporción a cuánto
// necesita cada acreedor, hasta saldar todo.
@Service
public class DeudaService {

    private final GastoRepository gastoRepository;
    private final GastoParticipanteRepository gastoParticipanteRepository;
    private final PagoGastoRepository pagoGastoRepository;

    public DeudaService(
            GastoRepository gastoRepository,
            GastoParticipanteRepository gastoParticipanteRepository,
            PagoGastoRepository pagoGastoRepository) {

        this.gastoRepository = gastoRepository;
        this.gastoParticipanteRepository = gastoParticipanteRepository;
        this.pagoGastoRepository = pagoGastoRepository;
    }

    public List<DeudaConPersona> calcularDeudas(Grupo grupo, Usuario usuario) {

        Map<Long, DeudaConPersona> resumenPorPersona = new LinkedHashMap<>();

        List<Gasto> gastos = gastoRepository.findByGrupoOrderByFechaDesc(grupo);

        for (Gasto gasto : gastos) {

            List<GastoParticipante> participantes =
                    gastoParticipanteRepository.findByGastoId(gasto.getId());

            if (participantes.isEmpty()) {
                continue;
            }

            List<PagoGasto> pagos =
                    pagoGastoRepository.findByGastoId(gasto.getId());

            if (pagos.isEmpty()) {
                continue;
            }

            // 1. Cuánto le toca pagar a cada participante (parte igual)
            BigDecimal cantidadParticipantes =
                    new BigDecimal(participantes.size());

            BigDecimal partePorPersona = gasto.getMonto()
                    .divide(cantidadParticipantes, 2, RoundingMode.HALF_UP);

            // 2. Armamos el saldo de CADA persona involucrada en este gasto:
            //    saldo = lo que puso - lo que le tocaba pagar.
            //    Usamos un mapa (id de usuario -> saldo) solo para este
            //    gasto en particular, distinto del mapa general de arriba.
            Map<Long, Usuario> personasDelGasto = new LinkedHashMap<>();
            Map<Long, BigDecimal> saldosDelGasto = new LinkedHashMap<>();

            // Empezamos a todos los participantes en "-parte" (les toca pagar)
            for (GastoParticipante p : participantes) {
                Usuario u = p.getUsuario();
                personasDelGasto.put(u.getId(), u);
                saldosDelGasto.put(u.getId(), partePorPersona.negate());
            }

            // A cada pagador le sumamos lo que puso (puede no ser
            // participante del gasto, aunque lo normal es que sí lo sea)
            for (PagoGasto pago : pagos) {
                Usuario u = pago.getUsuario();
                personasDelGasto.put(u.getId(), u);

                BigDecimal saldoActual =
                        saldosDelGasto.getOrDefault(u.getId(), BigDecimal.ZERO);

                saldosDelGasto.put(u.getId(), saldoActual.add(pago.getMonto()));
            }

            // 3. Separamos quién quedó con saldo positivo (le deben,
            //    "acreedores" de este gasto) y quién con saldo negativo
            //    (debe, "deudores" de este gasto)
            List<Long> acreedores = new ArrayList<>();
            List<Long> deudores = new ArrayList<>();

            for (Map.Entry<Long, BigDecimal> entry : saldosDelGasto.entrySet()) {

                int comparacion = entry.getValue().compareTo(BigDecimal.ZERO);

                if (comparacion > 0) {
                    acreedores.add(entry.getKey());
                } else if (comparacion < 0) {
                    deudores.add(entry.getKey());
                }
                // si es exactamente 0, esa persona quedó a paz y salvo
                // en este gasto puntual, no participa del reparto
            }

            // 4. Solo nos interesa este gasto si nuestro "usuario" fue
            //    acreedor o deudor en él. Si quedó en 0 (o no participó
            //    ni pagó), este gasto no le genera ningún movimiento.
            boolean usuarioInvolucrado =
                    acreedores.contains(usuario.getId()) ||
                            deudores.contains(usuario.getId());

            if (!usuarioInvolucrado) {
                continue;
            }

            // 5. Repartimos las deudas: cada deudor le paga a cada acreedor,
            //    en proporción a cuánto necesita cobrar cada acreedor.
            //    Usamos copias mutables de los saldos para ir "gastando"
            //    lo que cada quien debe/necesita cobrar a medida que se
            //    reparte.
            Map<Long, BigDecimal> pendientePorCobrar = new LinkedHashMap<>();
            for (Long idAcreedor : acreedores) {
                pendientePorCobrar.put(idAcreedor, saldosDelGasto.get(idAcreedor));
            }

            for (Long idDeudor : deudores) {

                BigDecimal deudaRestante = saldosDelGasto.get(idDeudor).abs();
                BigDecimal totalPorCobrar = sumar(pendientePorCobrar.values());

                for (Long idAcreedor : acreedores) {

                    if (deudaRestante.compareTo(BigDecimal.ZERO) == 0) {
                        break;
                    }

                    BigDecimal necesitaEsteAcreedor =
                            pendientePorCobrar.get(idAcreedor);

                    if (necesitaEsteAcreedor.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    // Proporción: cuánto de esta deuda le corresponde
                    // a este acreedor específico, según lo que necesita
                    // cobrar frente al total que falta por cobrar
                    BigDecimal proporcion = necesitaEsteAcreedor
                            .divide(totalPorCobrar, 10, RoundingMode.HALF_UP);

                    BigDecimal montoAsignado = deudaRestante
                            .multiply(proporcion)
                            .setScale(2, RoundingMode.HALF_UP);

                    if (montoAsignado.compareTo(BigDecimal.ZERO) <= 0) {
                        continue;
                    }

                    // Solo nos importa registrar el movimiento si involucra
                    // a nuestro "usuario" (como deudor o como acreedor)
                    if (idDeudor == usuario.getId()) {
                        // Nuestro usuario le debe a este acreedor
                        Usuario acreedor = personasDelGasto.get(idAcreedor);
                        acumular(resumenPorPersona, acreedor, montoAsignado.negate());
                    } else if (idAcreedor == usuario.getId()) {
                        // Este deudor le debe a nuestro usuario
                        Usuario deudor = personasDelGasto.get(idDeudor);
                        acumular(resumenPorPersona, deudor, montoAsignado);
                    }

                    pendientePorCobrar.put(
                            idAcreedor,
                            necesitaEsteAcreedor.subtract(montoAsignado)
                    );
                }
            }
        }

        return new ArrayList<>(resumenPorPersona.values());
    }

    private BigDecimal sumar(Iterable<BigDecimal> valores) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal v : valores) {
            total = total.add(v);
        }
        return total;
    }

    private void acumular(
            Map<Long, DeudaConPersona> resumen,
            Usuario otraPersona,
            BigDecimal monto) {

        DeudaConPersona deuda = resumen.get(otraPersona.getId());

        if (deuda == null) {
            deuda = new DeudaConPersona(otraPersona, BigDecimal.ZERO);
            resumen.put(otraPersona.getId(), deuda);
        }

        deuda.sumarMonto(monto);
    }

    public BigDecimal calcularBalanceTotal(Grupo grupo, Usuario usuario) {

        List<DeudaConPersona> deudas = calcularDeudas(grupo, usuario);

        BigDecimal total = BigDecimal.ZERO;

        for (DeudaConPersona deuda : deudas) {
            total = total.add(deuda.getMonto());
        }

        return total;
    }


    // ==========================================
    // BALANCE GENERAL (NUEVO): across TODOS los grupos del usuario
    // ==========================================

    // Para la tarjeta "BALANCE TOTAL" de inicio.html (el Figma: "Te deben
    // $210.000" / "Debes $75.000"). A propósito, esto NO "netea" las
    // deudas entre grupos distintos: si en el Grupo A le debes $20.000
    // a Wilmer, y en el Grupo B Wilmer te debe $50.000, el resultado es
    // "Te deben $50.000" y "Debes $20.000" por separado — no un neto de
    // "$30.000 a tu favor". Esto es consistente con que cada grupo ya
    // se calcula y se muestra de forma independiente en grupo.html; el
    // balance general es, simplemente, la suma de esos totales por grupo.
    public BalanceGeneral calcularBalanceGeneral(List<Grupo> grupos, Usuario usuario) {

        BigDecimal totalLeDeben = BigDecimal.ZERO;
        BigDecimal totalDebe = BigDecimal.ZERO;

        for (Grupo grupo : grupos) {

            BigDecimal balanceDelGrupo = calcularBalanceTotal(grupo, usuario);

            // Positivo: en este grupo, en total, le deben a él.
            // Negativo: en este grupo, en total, él debe.
            if (balanceDelGrupo.compareTo(BigDecimal.ZERO) > 0) {
                totalLeDeben = totalLeDeben.add(balanceDelGrupo);
            } else if (balanceDelGrupo.compareTo(BigDecimal.ZERO) < 0) {
                totalDebe = totalDebe.add(balanceDelGrupo.abs());
            }
        }

        return new BalanceGeneral(totalLeDeben, totalDebe);
    }
}