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
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// @Service: capa de lógica de negocio, entre el controlador y los repositorios.
//
// Un gasto puede tener VARIOS pagadores, cada uno con su propio monto
// (tabla PagoGasto). La fórmula central, para cada persona en cada gasto:
//
//     saldo_en_este_gasto = lo_que_puso - lo_que_le_tocaba_pagar
//
// Saldo positivo: puso de más -> le deben esa diferencia.
// Saldo negativo: puso de menos (o nada) -> debe esa diferencia.
//
// CAMBIO (revisión de calidad): la app trabaja con PESOS ENTEROS (sin
// centavos), así que TODO el cálculo se hace en pesos enteros:
//
//  - Antes la parte de cada uno era monto / participantes con 2 decimales.
//    Con $100.000 entre 3 daba $33.333,33 para cada uno: la suma de las
//    partes ($99.999,99) ya no coincidía con el monto, y aparecían deudas
//    fantasma de $0,33 incluso cuando todos habían pagado su parte exacta.
//  - Ahora la parte es el cociente entero, y los pesos que sobran se
//    reparten de a uno entre los primeros participantes (ordenados por id,
//    para que el resultado siempre sea el mismo). $100.000 entre 3 queda
//    $33.334 + $33.333 + $33.333 = $100.000 exactos.
//  - Como los saldos suman exactamente cero, el balance de todos los
//    integrantes también suma cero: lo que unos deben es lo que otros cobran.
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

            // Copia mutable, porque la vamos a ordenar
            List<GastoParticipante> participantes =
                    new ArrayList<>(gastoParticipanteRepository.findByGastoId(gasto.getId()));

            if (participantes.isEmpty()) {
                continue;
            }

            List<PagoGasto> pagos =
                    pagoGastoRepository.findByGastoId(gasto.getId());

            if (pagos.isEmpty()) {
                continue;
            }

            // Orden fijo por id de usuario: así el peso que sobra al dividir
            // siempre cae en las mismas personas, cada vez que se calcula.
            participantes.sort(Comparator.comparing(p -> p.getUsuario().getId()));

            // 1. Cuánto le toca pagar a cada participante, en pesos enteros
            BigDecimal cantidadParticipantes =
                    BigDecimal.valueOf(participantes.size());

            BigDecimal montoEntero =
                    gasto.getMonto().setScale(0, RoundingMode.HALF_UP);

            BigDecimal parteBase =
                    montoEntero.divide(cantidadParticipantes, 0, RoundingMode.DOWN);

            // Pesos que sobran al dividir (siempre menos que participantes)
            int pesosSobrantes = montoEntero
                    .subtract(parteBase.multiply(cantidadParticipantes))
                    .intValue();

            // 2. Saldo de CADA persona involucrada en este gasto
            Map<Long, Usuario> personasDelGasto = new LinkedHashMap<>();
            Map<Long, BigDecimal> saldosDelGasto = new LinkedHashMap<>();

            int posicion = 0;
            for (GastoParticipante p : participantes) {
                Usuario u = p.getUsuario();

                BigDecimal parte = posicion < pesosSobrantes
                        ? parteBase.add(BigDecimal.ONE)
                        : parteBase;
                posicion++;

                personasDelGasto.put(u.getId(), u);
                saldosDelGasto.put(u.getId(), parte.negate());
            }

            // A cada pagador le sumamos lo que puso (puede no ser
            // participante del gasto, aunque lo normal es que sí lo sea)
            BigDecimal totalPagosRedondeados = BigDecimal.ZERO;
            Long primerPagadorId = null;
            for (PagoGasto pago : pagos) {
                Usuario u = pago.getUsuario();
                personasDelGasto.put(u.getId(), u);
                if (primerPagadorId == null) {
                    primerPagadorId = u.getId();
                }

                BigDecimal montoPago =
                        pago.getMonto().setScale(0, RoundingMode.HALF_UP);
                totalPagosRedondeados = totalPagosRedondeados.add(montoPago);

                saldosDelGasto.merge(u.getId(), montoPago, BigDecimal::add);
            }

            // Garantia de invariante suma cero (HA-014): si el redondeo independiente
            // de pagos historicos con decimales difiere de montoEntero, ajustamos el delta.
            BigDecimal deltaRedondeo = montoEntero.subtract(totalPagosRedondeados);
            if (deltaRedondeo.signum() != 0 && primerPagadorId != null) {
                saldosDelGasto.merge(primerPagadorId, deltaRedondeo, BigDecimal::add);
            }

            // 3. Acreedores (saldo positivo) y deudores (saldo negativo).
            //    Saldo exactamente 0 = a paz y salvo en este gasto.
            List<Long> acreedores = new ArrayList<>();
            List<Long> deudores = new ArrayList<>();

            for (Map.Entry<Long, BigDecimal> entry : saldosDelGasto.entrySet()) {

                int comparacion = entry.getValue().signum();

                if (comparacion > 0) {
                    acreedores.add(entry.getKey());
                } else if (comparacion < 0) {
                    deudores.add(entry.getKey());
                }
            }

            // 4. Solo nos interesa este gasto si nuestro "usuario" fue
            //    acreedor o deudor en él.
            boolean usuarioInvolucrado =
                    acreedores.contains(usuario.getId()) ||
                            deudores.contains(usuario.getId());

            if (!usuarioInvolucrado) {
                continue;
            }

            // 5. Repartimos las deudas: cada deudor le paga a cada acreedor
            //    en proporción a cuánto necesita cobrar cada uno.
            Map<Long, BigDecimal> pendientePorCobrar = new LinkedHashMap<>();
            for (Long idAcreedor : acreedores) {
                pendientePorCobrar.put(idAcreedor, saldosDelGasto.get(idAcreedor));
            }

            for (Long idDeudor : deudores) {

                BigDecimal deudaRestante = saldosDelGasto.get(idDeudor).abs();
                BigDecimal totalPorCobrar = sumar(pendientePorCobrar.values());

                if (totalPorCobrar.signum() <= 0) {
                    break;
                }

                // 5a. Reparto proporcional, redondeado HACIA ABAJO a pesos
                //     enteros (multiplicamos primero y dividimos después,
                //     para no perder precisión). Nunca se le asigna a un
                //     acreedor más de lo que le falta por cobrar.
                Map<Long, BigDecimal> asignado = new LinkedHashMap<>();
                BigDecimal asignadoTotal = BigDecimal.ZERO;

                for (Long idAcreedor : acreedores) {

                    BigDecimal necesita = pendientePorCobrar.get(idAcreedor);

                    BigDecimal parte = deudaRestante
                            .multiply(necesita)
                            .divide(totalPorCobrar, 0, RoundingMode.DOWN)
                            .min(necesita);

                    asignado.put(idAcreedor, parte);
                    asignadoTotal = asignadoTotal.add(parte);
                }

                // 5b. Los pesos que sobraron por el redondeo hacia abajo se
                //     reparten de a uno, para que esta persona pague
                //     exactamente lo que debe.
                BigDecimal porAsignar = deudaRestante.subtract(asignadoTotal);

                for (Long idAcreedor : acreedores) {

                    if (porAsignar.signum() <= 0) {
                        break;
                    }

                    BigDecimal libre = pendientePorCobrar.get(idAcreedor)
                            .subtract(asignado.get(idAcreedor));

                    if (libre.signum() > 0) {
                        asignado.put(idAcreedor, asignado.get(idAcreedor).add(BigDecimal.ONE));
                        porAsignar = porAsignar.subtract(BigDecimal.ONE);
                    }
                }

                // 5c. Registramos el movimiento solo si involucra a nuestro
                //     "usuario" (como deudor o como acreedor)
                for (Long idAcreedor : acreedores) {

                    BigDecimal montoAsignado = asignado.get(idAcreedor);

                    if (montoAsignado.signum() <= 0) {
                        continue;
                    }

                    if (idDeudor.equals(usuario.getId())) {
                        // Nuestro usuario le debe a este acreedor
                        acumular(resumenPorPersona,
                                personasDelGasto.get(idAcreedor),
                                montoAsignado.negate());
                    } else if (idAcreedor.equals(usuario.getId())) {
                        // Este deudor le debe a nuestro usuario
                        acumular(resumenPorPersona,
                                personasDelGasto.get(idDeudor),
                                montoAsignado);
                    }

                    pendientePorCobrar.put(
                            idAcreedor,
                            pendientePorCobrar.get(idAcreedor).subtract(montoAsignado)
                    );
                }
            }
        }

        // Defensa extra: si dos gastos se cancelan entre sí (o queda algún
        // resto menor a $1), esa persona no aparece como "te debe $0".
        List<DeudaConPersona> resultado = new ArrayList<>(resumenPorPersona.values());
        resultado.removeIf(d -> d.getMonto().abs().compareTo(BigDecimal.ONE) < 0);

        return resultado;
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
    // BALANCE GENERAL: across TODOS los grupos del usuario
    // ==========================================

    // Para la tarjeta "BALANCE TOTAL" de inicio.html. A propósito, esto NO
    // "netea" las deudas entre grupos distintos: si en el Grupo A le debes
    // $20.000 a Wilmer, y en el Grupo B Wilmer te debe $50.000, el
    // resultado es "Te deben $50.000" y "Debes $20.000" por separado.

    // NUEVO: calcula el balance de cada grupo UNA sola vez. Antes,
    // inicio calculaba dos veces el balance de los grupos destacados (una
    // para el general y otra para la tarjeta de cada grupo), y cada cálculo
    // hace varias consultas a la base de datos. Ahora el controlador pide
    // este mapa (id de grupo -> balance) y reutiliza el resultado.
    public Map<Long, BigDecimal> calcularBalancesPorGrupo(List<Grupo> grupos, Usuario usuario) {

        Map<Long, BigDecimal> balances = new LinkedHashMap<>();

        for (Grupo grupo : grupos) {
            balances.put(grupo.getId(), calcularBalanceTotal(grupo, usuario));
        }

        return balances;
    }

    // NUEVO: convierte esos balances en el "te deben / debes" general
    public BalanceGeneral resumirBalances(Collection<BigDecimal> balancesPorGrupo) {

        BigDecimal totalLeDeben = BigDecimal.ZERO;
        BigDecimal totalDebe = BigDecimal.ZERO;

        for (BigDecimal balanceDelGrupo : balancesPorGrupo) {

            // Positivo: en este grupo, en total, le deben a él.
            // Negativo: en este grupo, en total, él debe.
            if (balanceDelGrupo.signum() > 0) {
                totalLeDeben = totalLeDeben.add(balanceDelGrupo);
            } else if (balanceDelGrupo.signum() < 0) {
                totalDebe = totalDebe.add(balanceDelGrupo.abs());
            }
        }

        return new BalanceGeneral(totalLeDeben, totalDebe);
    }

    // Se mantiene por compatibilidad: hace lo mismo que antes, pero
    // apoyándose en los dos métodos de arriba.
    public BalanceGeneral calcularBalanceGeneral(List<Grupo> grupos, Usuario usuario) {

        return resumirBalances(calcularBalancesPorGrupo(grupos, usuario).values());
    }
}