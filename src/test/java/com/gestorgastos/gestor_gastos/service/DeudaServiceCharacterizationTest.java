package com.gestorgastos.gestor_gastos.service;

import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.entity.GastoParticipante;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.PagoGasto;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.GastoParticipanteRepository;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.repository.PagoGastoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de caracterizacion del comportamiento vigente de DeudaService.
 *
 * Nota metodologica (F1.1.A):
 * - Estas pruebas documentan el comportamiento ACTUAL del codigo productivo sin modificarlo.
 * - Cuando una prueba evidencia un defecto conocido (ej. HA-010, HA-014), se indica
 *   explicitamente en el @DisplayName y comentario para no consolidarlo como requisito permanente.
 */
@ExtendWith(MockitoExtension.class)
class DeudaServiceCharacterizationTest {

    @Mock
    private GastoRepository gastoRepository;

    @Mock
    private GastoParticipanteRepository gastoParticipanteRepository;

    @Mock
    private PagoGastoRepository pagoGastoRepository;

    @InjectMocks
    private DeudaService deudaService;

    private Usuario ana;
    private Usuario beto;
    private Usuario carla;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        ana = crearUsuario(1L, "Ana", "ana@test.local");
        beto = crearUsuario(2L, "Beto", "beto@test.local");
        carla = crearUsuario(3L, "Carla", "carla@test.local");

        grupo = new Grupo("Hogar Test", LocalDate.of(2026, 1, 1), ana);
        grupo.setId(10L);
    }

    @Test
    @DisplayName("1. Division igualitaria exacta con un pagador participante")
    void divisionIgualitariaExactaUnPagador() {
        Gasto gasto = crearGasto(100L, "Mercado", new BigDecimal("90000"));
        mockGasto(gasto, List.of(ana, beto, carla), List.of(new PagoGasto(gasto, ana, new BigDecimal("90000"))));

        List<DeudaConPersona> deudasAna = deudaService.calcularDeudas(grupo, ana);
        assertEquals(2, deudasAna.size());
        assertEquals(new BigDecimal("30000"), buscarMontoCon(deudasAna, beto));
        assertEquals(new BigDecimal("30000"), buscarMontoCon(deudasAna, carla));
        assertEquals(new BigDecimal("60000"), deudaService.calcularBalanceTotal(grupo, ana));

        List<DeudaConPersona> deudasBeto = deudaService.calcularDeudas(grupo, beto);
        assertEquals(1, deudasBeto.size());
        assertEquals(new BigDecimal("-30000"), buscarMontoCon(deudasBeto, ana));
        assertEquals(new BigDecimal("-30000"), deudaService.calcularBalanceTotal(grupo, beto));

        verificarInvarianteSumaCero(List.of(ana, beto, carla));
    }

    @Test
    @DisplayName("2. Division con residuos: comportamiento vigente asigna +1 peso a menor usuario.id (evidencia HA-010)")
    void divisionConResiduosAsignaSobranteAMenorId() {
        // $100.000 / 3 = $33.333 base, sobra $1 -> Ana (id=1) asume $33.334; Beto (id=2) y Carla (id=3) $33.333
        Gasto gasto = crearGasto(101L, "Cena", new BigDecimal("100000"));
        mockGasto(gasto, List.of(carla, beto, ana), List.of(new PagoGasto(gasto, carla, new BigDecimal("100000"))));

        List<DeudaConPersona> deudasCarla = deudaService.calcularDeudas(grupo, carla);
        assertEquals(new BigDecimal("33334"), buscarMontoCon(deudasCarla, ana));
        assertEquals(new BigDecimal("33333"), buscarMontoCon(deudasCarla, beto));

        verificarInvarianteSumaCero(List.of(ana, beto, carla));
    }

    @Test
    @DisplayName("3. Pagador no participante del gasto recibe el total dividido entre participantes")
    void pagadorNoParticipante() {
        Gasto gasto = crearGasto(102L, "Transporte amigos", new BigDecimal("40000"));
        mockGasto(gasto, List.of(beto, carla), List.of(new PagoGasto(gasto, ana, new BigDecimal("40000"))));

        assertEquals(new BigDecimal("40000"), deudaService.calcularBalanceTotal(grupo, ana));
        assertEquals(new BigDecimal("-20000"), deudaService.calcularBalanceTotal(grupo, beto));
        assertEquals(new BigDecimal("-20000"), deudaService.calcularBalanceTotal(grupo, carla));

        verificarInvarianteSumaCero(List.of(ana, beto, carla));
    }

    @Test
    @DisplayName("4. Multiples pagadores (participantes y no participantes) con reparto proporcional")
    void multiplesPagadoresRepartoProporcional() {
        // Gasto $60.000 entre Ana, Beto, Carla ($20.000 c/u).
        // Ana pone $50.000 (saldo +30.000), Beto pone $10.000 (saldo -10.000), Carla pone $0 (saldo -20.000).
        Gasto gasto = crearGasto(103L, "Servicios", new BigDecimal("60000"));
        mockGasto(
                gasto,
                List.of(ana, beto, carla),
                List.of(
                        new PagoGasto(gasto, ana, new BigDecimal("50000")),
                        new PagoGasto(gasto, beto, new BigDecimal("10000"))
                )
        );

        assertEquals(new BigDecimal("30000"), deudaService.calcularBalanceTotal(grupo, ana));
        assertEquals(new BigDecimal("-10000"), deudaService.calcularBalanceTotal(grupo, beto));
        assertEquals(new BigDecimal("-20000"), deudaService.calcularBalanceTotal(grupo, carla));

        verificarInvarianteSumaCero(List.of(ana, beto, carla));
    }

    @Test
    @DisplayName("5. Cancelacion de deudas entre dos gastos opuestos")
    void cancelacionDeudasEntreGastos() {
        Gasto g1 = crearGasto(104L, "Almuerzo", new BigDecimal("50000"));
        Gasto g2 = crearGasto(105L, "Cafe", new BigDecimal("50000"));

        when(gastoRepository.findByGrupoOrderByFechaDesc(grupo)).thenReturn(List.of(g1, g2));
        when(gastoParticipanteRepository.findByGastoId(104L)).thenReturn(participantesDe(g1, ana, beto));
        when(pagoGastoRepository.findByGastoId(104L)).thenReturn(List.of(new PagoGasto(g1, ana, new BigDecimal("50000"))));

        when(gastoParticipanteRepository.findByGastoId(105L)).thenReturn(participantesDe(g2, ana, beto));
        when(pagoGastoRepository.findByGastoId(105L)).thenReturn(List.of(new PagoGasto(g2, beto, new BigDecimal("50000"))));

        List<DeudaConPersona> deudasAna = deudaService.calcularDeudas(grupo, ana);
        assertTrue(deudasAna.isEmpty());
        assertEquals(BigDecimal.ZERO, deudaService.calcularBalanceTotal(grupo, ana));
        assertEquals(BigDecimal.ZERO, deudaService.calcularBalanceTotal(grupo, beto));
    }

    @Test
    @DisplayName("6. Grupo sin gastos retorna lista vacia y balance cero")
    void grupoSinGastos() {
        when(gastoRepository.findByGrupoOrderByFechaDesc(grupo)).thenReturn(List.of());

        assertTrue(deudaService.calcularDeudas(grupo, ana).isEmpty());
        assertEquals(BigDecimal.ZERO, deudaService.calcularBalanceTotal(grupo, ana));
    }

    @Test
    @DisplayName("7. Casos limite monetarios: $1 entre 3 personas y montos grandes (999999999999)")
    void casosLimiteMonetarios() {
        Gasto gMin = crearGasto(106L, "Minimo", BigDecimal.ONE);
        mockGasto(gMin, List.of(ana, beto, carla), List.of(new PagoGasto(gMin, carla, BigDecimal.ONE)));

        // Ana (id=1) asume el unico peso -> debe $1 a Carla; Beto queda en $0
        assertEquals(new BigDecimal("-1"), deudaService.calcularBalanceTotal(grupo, ana));
        assertEquals(BigDecimal.ZERO, deudaService.calcularBalanceTotal(grupo, beto));
        assertEquals(BigDecimal.ONE, deudaService.calcularBalanceTotal(grupo, carla));
        verificarInvarianteSumaCero(List.of(ana, beto, carla));

        Gasto gMax = crearGasto(107L, "Tope", new BigDecimal("999999999999"));
        mockGasto(gMax, List.of(ana, beto, carla), List.of(new PagoGasto(gMax, ana, new BigDecimal("999999999999"))));
        assertEquals(new BigDecimal("666666666666"), deudaService.calcularBalanceTotal(grupo, ana));
        verificarInvarianteSumaCero(List.of(ana, beto, carla));
    }

    @Test
    @DisplayName("8. Datos inconsistentes: gasto sin participantes o sin pagos es ignorado")
    void gastoSinParticipantesOSinPagosEsIgnorado() {
        Gasto sinParticipantes = crearGasto(108L, "Huerfano 1", new BigDecimal("20000"));
        Gasto sinPagos = crearGasto(109L, "Huerfano 2", new BigDecimal("20000"));

        when(gastoRepository.findByGrupoOrderByFechaDesc(grupo)).thenReturn(List.of(sinParticipantes, sinPagos));
        when(gastoParticipanteRepository.findByGastoId(108L)).thenReturn(List.of());
        when(gastoParticipanteRepository.findByGastoId(109L)).thenReturn(participantesDe(sinPagos, ana, beto));
        when(pagoGastoRepository.findByGastoId(109L)).thenReturn(List.of());

        assertTrue(deudaService.calcularDeudas(grupo, ana).isEmpty());
        assertEquals(BigDecimal.ZERO, deudaService.calcularBalanceTotal(grupo, ana));
    }

    @Test
    @DisplayName("9. Caracterizacion de defecto HA-014: decimales en BD generan saldo acreedor perdido por HALF_UP independiente")
    void caracterizaSaldoPerdidoConDecimalesEnBd() {
        // Gasto = 21.00 entre Ana (11) y Beto (10).
        // Pagos = 10.50 (Ana -> 11) y 10.50 (Beto -> 11): suma pagos = 22 != 21.
        // Saldo de Ana = 0, Saldo de Beto = +1 (acreedor), pero deudores = [] -> ese +1 desaparece.
        Gasto gasto = crearGasto(110L, "Decimales BD", new BigDecimal("21.00"));
        mockGasto(
                gasto,
                List.of(ana, beto),
                List.of(
                        new PagoGasto(gasto, ana, new BigDecimal("10.50")),
                        new PagoGasto(gasto, beto, new BigDecimal("10.50"))
                )
        );

        // Documenta el comportamiento vigente (HA-014): Beto deberia tener +1 segun redondeo,
        // pero al no existir deudor en el gasto su credito se pierde silenciosamente y resulta 0.
        assertEquals(BigDecimal.ZERO, deudaService.calcularBalanceTotal(grupo, beto));
    }

    @Test
    @DisplayName("10. Balance general con varios grupos: separa te deben y debes sin compensar entre grupos")
    void balanceGeneralConVariosGruposNoCompensaEntreGrupos() {
        Grupo grupoB = new Grupo("Viaje", LocalDate.of(2026, 2, 1), ana);
        grupoB.setId(20L);
        Grupo grupoC = new Grupo("Sin gastos", LocalDate.of(2026, 3, 1), ana);
        grupoC.setId(30L);

        // Grupo A (grupo): Ana paga $40.000 entre Ana y Beto -> Ana tiene +20.000
        Gasto gA = crearGasto(201L, "Mercado A", new BigDecimal("40000"));
        when(gastoRepository.findByGrupoOrderByFechaDesc(grupo)).thenReturn(List.of(gA));
        when(gastoParticipanteRepository.findByGastoId(201L)).thenReturn(participantesDe(gA, ana, beto));
        when(pagoGastoRepository.findByGastoId(201L)).thenReturn(List.of(new PagoGasto(gA, ana, new BigDecimal("40000"))));

        // Grupo B (grupoB): Beto paga $100.000 entre Ana y Beto -> Ana tiene -50.000
        Gasto gB = new Gasto("Hotel B", new BigDecimal("100000"), LocalDate.of(2026, 2, 2), grupoB);
        gB.setId(202L);
        when(gastoRepository.findByGrupoOrderByFechaDesc(grupoB)).thenReturn(List.of(gB));
        when(gastoParticipanteRepository.findByGastoId(202L)).thenReturn(List.of(
                new GastoParticipante(ana, grupoB, gB),
                new GastoParticipante(beto, grupoB, gB)
        ));
        when(pagoGastoRepository.findByGastoId(202L)).thenReturn(List.of(new PagoGasto(gB, beto, new BigDecimal("100000"))));

        // Grupo C: sin gastos -> saldo 0
        when(gastoRepository.findByGrupoOrderByFechaDesc(grupoC)).thenReturn(List.of());

        List<Grupo> grupos = List.of(grupo, grupoB, grupoC);
        Map<Long, BigDecimal> porGrupo = deudaService.calcularBalancesPorGrupo(grupos, ana);

        assertEquals(3, porGrupo.size());
        assertEquals(new BigDecimal("20000"), porGrupo.get(10L));
        assertEquals(new BigDecimal("-50000"), porGrupo.get(20L));
        assertEquals(BigDecimal.ZERO, porGrupo.get(30L));

        // Independencia entre grupos
        assertEquals(deudaService.calcularBalanceTotal(grupo, ana), porGrupo.get(10L));
        assertEquals(deudaService.calcularBalanceTotal(grupoB, ana), porGrupo.get(20L));

        // No compensa entre grupos: te deben 20.000 y debes 50.000 por separado
        BalanceGeneral general = deudaService.calcularBalanceGeneral(grupos, ana);
        assertEquals(new BigDecimal("20000"), general.getTotalLeDeben());
        assertEquals(new BigDecimal("50000"), general.getTotalDebe());

        // Orden distinto produce exactamente el mismo BalanceGeneral y mismos valores por grupo
        List<Grupo> ordenInvertido = List.of(grupoC, grupoB, grupo);
        BalanceGeneral generalInvertido = deudaService.calcularBalanceGeneral(ordenInvertido, ana);
        assertEquals(general.getTotalLeDeben(), generalInvertido.getTotalLeDeben());
        assertEquals(general.getTotalDebe(), generalInvertido.getTotalDebe());
    }

    @Test
    @DisplayName("11. Usuario sin grupos retorna mapa vacio y balance general en cero")
    void usuarioSinGruposRetornaBalanceGeneralEnCero() {
        Map<Long, BigDecimal> porGrupo = deudaService.calcularBalancesPorGrupo(List.of(), ana);
        assertTrue(porGrupo.isEmpty());

        BalanceGeneral general = deudaService.calcularBalanceGeneral(List.of(), ana);
        assertEquals(BigDecimal.ZERO, general.getTotalLeDeben());
        assertEquals(BigDecimal.ZERO, general.getTotalDebe());
    }

    @Test
    @DisplayName("12. resumirBalances suma positivos y negativos por separado ignorando ceros")
    void resumirBalancesConservaSaldosAgregados() {
        BalanceGeneral resumen = deudaService.resumirBalances(List.of(
                new BigDecimal("15000"),
                BigDecimal.ZERO,
                new BigDecimal("-8000"),
                new BigDecimal("5000"),
                new BigDecimal("-12000")
        ));

        assertEquals(new BigDecimal("20000"), resumen.getTotalLeDeben());
        assertEquals(new BigDecimal("20000"), resumen.getTotalDebe());
    }

    private void verificarInvarianteSumaCero(List<Usuario> usuarios) {
        BigDecimal suma = BigDecimal.ZERO;
        for (Usuario u : usuarios) {
            suma = suma.add(deudaService.calcularBalanceTotal(grupo, u));
        }
        assertEquals(0, suma.compareTo(BigDecimal.ZERO),
                "La suma de balances del grupo debe ser cero, pero fue: " + suma);
    }

    private Usuario crearUsuario(Long id, String nombre, String email) {
        Usuario u = new Usuario(nombre, email, "hash");
        u.setId(id);
        return u;
    }

    private Gasto crearGasto(Long id, String desc, BigDecimal monto) {
        Gasto g = new Gasto(desc, monto, LocalDate.of(2026, 2, 1), grupo);
        g.setId(id);
        return g;
    }

    private List<GastoParticipante> participantesDe(Gasto gasto, Usuario... usuarios) {
        return java.util.Arrays.stream(usuarios)
                .map(u -> new GastoParticipante(u, grupo, gasto))
                .toList();
    }

    private void mockGasto(Gasto gasto, List<Usuario> participantes, List<PagoGasto> pagos) {
        when(gastoRepository.findByGrupoOrderByFechaDesc(grupo)).thenReturn(List.of(gasto));
        when(gastoParticipanteRepository.findByGastoId(gasto.getId()))
                .thenReturn(participantes.stream().map(u -> new GastoParticipante(u, grupo, gasto)).toList());
        when(pagoGastoRepository.findByGastoId(gasto.getId())).thenReturn(pagos);
    }

    private BigDecimal buscarMontoCon(List<DeudaConPersona> deudas, Usuario persona) {
        return deudas.stream()
                .filter(d -> d.getOtraPersona().getId().equals(persona.getId()))
                .map(DeudaConPersona::getMonto)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se encontro deuda con " + persona.getName()));
    }
}
