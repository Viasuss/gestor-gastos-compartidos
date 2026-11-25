package com.gestorgastos.gestor_gastos.b6;

import com.gestorgastos.gestor_gastos.config.DemoDataSeeder;
import com.gestorgastos.gestor_gastos.entity.EstadoLiquidacion;
import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.entity.GastoParticipante;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.Liquidacion;
import com.gestorgastos.gestor_gastos.entity.MetodoPago;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.PagoGasto;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.BorradorOperacionRepository;
import com.gestorgastos.gestor_gastos.repository.GastoParticipanteRepository;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.PagoGastoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.service.DeudaService;
import com.gestorgastos.gestor_gastos.service.LiquidacionService;
import com.gestorgastos.gestor_gastos.service.ia.ResourceManager;
import com.gestorgastos.gestor_gastos.support.ExternalProvidersTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
@Transactional
class B6ValidationSuiteTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private MiembroGrupoRepository miembroGrupoRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private PagoGastoRepository pagoGastoRepository;

    @Autowired
    private GastoParticipanteRepository gastoParticipanteRepository;

    @Autowired
    private BorradorOperacionRepository borradorOperacionRepository;

    @Autowired
    private DeudaService deudaService;

    @Autowired
    private LiquidacionService liquidacionService;

    @Autowired
    private ResourceManager resourceManager;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void recorridoFinancieroCompletoSumaCeroEIndependenciaEntreGrupos() {
        String pass = passwordEncoder.encode("ClaveDemo2026!");
        Usuario a = usuarioRepository.save(new Usuario("Demo A", "b6.a@test.local", pass));
        Usuario b = usuarioRepository.save(new Usuario("Demo B", "b6.b@test.local", pass));
        Usuario c = usuarioRepository.save(new Usuario("Demo C", "b6.c@test.local", pass));

        Grupo apto = grupoRepository.save(new Grupo("Apartamento NEXUS Demo", LocalDate.now(), a));
        miembroGrupoRepository.save(new MiembroGrupo(a, apto, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(b, apto, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(c, apto, EstadoMiembro.MIEMBRO));

        // Gasto 1: A paga 120.000, participan A, B y C
        Gasto g1 = gastoRepository.save(new Gasto("Internet", new BigDecimal("120000.00"), LocalDate.now(), apto));
        pagoGastoRepository.save(new PagoGasto(g1, a, new BigDecimal("120000.00")));
        gastoParticipanteRepository.save(new GastoParticipante(a, apto, g1));
        gastoParticipanteRepository.save(new GastoParticipante(b, apto, g1));
        gastoParticipanteRepository.save(new GastoParticipante(c, apto, g1));

        Map<Long, BigDecimal> bal1A = deudaService.calcularBalancesPorGrupo(List.of(apto), a);
        Map<Long, BigDecimal> bal1B = deudaService.calcularBalancesPorGrupo(List.of(apto), b);
        Map<Long, BigDecimal> bal1C = deudaService.calcularBalancesPorGrupo(List.of(apto), c);

        assertEquals(0, new BigDecimal("80000.00").compareTo(bal1A.get(apto.getId())));
        assertEquals(0, new BigDecimal("-40000.00").compareTo(bal1B.get(apto.getId())));
        assertEquals(0, new BigDecimal("-40000.00").compareTo(bal1C.get(apto.getId())));
        assertEquals(0, bal1A.get(apto.getId()).add(bal1B.get(apto.getId())).add(bal1C.get(apto.getId())).signum());

        // Gasto 2: Mercado 90.000, A paga 60.000 y B paga 30.000, participan A, B y C
        Gasto g2 = gastoRepository.save(new Gasto("Mercado", new BigDecimal("90000.00"), LocalDate.now(), apto));
        pagoGastoRepository.save(new PagoGasto(g2, a, new BigDecimal("60000.00")));
        pagoGastoRepository.save(new PagoGasto(g2, b, new BigDecimal("30000.00")));
        gastoParticipanteRepository.save(new GastoParticipante(a, apto, g2));
        gastoParticipanteRepository.save(new GastoParticipante(b, apto, g2));
        gastoParticipanteRepository.save(new GastoParticipante(c, apto, g2));

        Map<Long, BigDecimal> bal2A = deudaService.calcularBalancesPorGrupo(List.of(apto), a);
        Map<Long, BigDecimal> bal2B = deudaService.calcularBalancesPorGrupo(List.of(apto), b);
        Map<Long, BigDecimal> bal2C = deudaService.calcularBalancesPorGrupo(List.of(apto), c);

        assertEquals(0, new BigDecimal("110000.00").compareTo(bal2A.get(apto.getId())));
        assertEquals(0, new BigDecimal("-40000.00").compareTo(bal2B.get(apto.getId())));
        assertEquals(0, new BigDecimal("-70000.00").compareTo(bal2C.get(apto.getId())));
        assertEquals(0, bal2A.get(apto.getId()).add(bal2B.get(apto.getId())).add(bal2C.get(apto.getId())).signum());

        // Independencia entre grupos
        Grupo otro = grupoRepository.save(new Grupo("Viaje B6", LocalDate.now(), b));
        miembroGrupoRepository.save(new MiembroGrupo(b, otro, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(a, otro, EstadoMiembro.MIEMBRO));
        assertEquals(0, BigDecimal.ZERO.compareTo(deudaService.calcularBalancesPorGrupo(List.of(otro), a).get(otro.getId())));
    }

    @Test
    void liquidacionesRechazanSobreLiquidacionTercerosYPermitenIdempotencia() {
        String pass = passwordEncoder.encode("ClaveDemo2026!");
        Usuario a = usuarioRepository.save(new Usuario("A", "liq.a@test.local", pass));
        Usuario b = usuarioRepository.save(new Usuario("B", "liq.b@test.local", pass));
        Usuario c = usuarioRepository.save(new Usuario("C", "liq.c@test.local", pass));

        Grupo g = grupoRepository.save(new Grupo("Grupo Liq B6", LocalDate.now(), a));
        miembroGrupoRepository.save(new MiembroGrupo(a, g, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(b, g, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(c, g, EstadoMiembro.MIEMBRO));

        Gasto gasto = gastoRepository.save(new Gasto("Cena", new BigDecimal("60000.00"), LocalDate.now(), g));
        pagoGastoRepository.save(new PagoGasto(gasto, a, new BigDecimal("60000.00")));
        gastoParticipanteRepository.save(new GastoParticipante(a, g, gasto));
        gastoParticipanteRepository.save(new GastoParticipante(b, g, gasto));
        gastoParticipanteRepository.save(new GastoParticipante(c, g, gasto));

        // B debe 20.000 a A
        Liquidacion l1 = liquidacionService.registrarSolicitud(
                g.getId(), b, a.getId(), new BigDecimal("15000.00"),
                MetodoPago.NEQUI, "Abono", "key-liq-1");
        assertEquals(EstadoLiquidacion.PENDIENTE, l1.getEstado());

        // Idempotencia con misma clave
        Liquidacion l1Dup = liquidacionService.registrarSolicitud(
                g.getId(), b, a.getId(), new BigDecimal("15000.00"),
                MetodoPago.NEQUI, "Abono", "key-liq-1");
        assertEquals(l1.getId(), l1Dup.getId());

        // Otra solicitud que supera el disponible restante (5.000) debe fallar
        assertThrows(IllegalArgumentException.class, () -> liquidacionService.registrarSolicitud(
                g.getId(), b, a.getId(), new BigDecimal("10000.00"),
                MetodoPago.EFECTIVO, "Exceso", "key-liq-2"));

        // Tercero C no puede confirmar liquidacion de B hacia A
        assertThrows(SecurityException.class, () -> liquidacionService.confirmar(l1.getId(), c));

        // A confirma
        Liquidacion confirmada = liquidacionService.confirmar(l1.getId(), a);
        assertEquals(EstadoLiquidacion.CONFIRMADA, confirmada.getEstado());
    }

    @Test
    void resourceManagerCircuitBreakerYConcurrenciaBajoCuota() throws InterruptedException {
        // Simular 429 abre circuit breaker de inmediato
        resourceManager.recordFailure("GROQ", 429);
        assertTrue(resourceManager.isCircuitOpen("GROQ"));

        // Simular 401/403
        resourceManager.recordFailure("GROQ_TEST_AUTH", 401);
        assertTrue(resourceManager.isCircuitOpen("GROQ_TEST_AUTH"));
        resourceManager.recordFailure("GROQ_TEST_FORBIDDEN", 403);
        assertTrue(resourceManager.isCircuitOpen("GROQ_TEST_FORBIDDEN"));

        // Simular 5xx consecutivo hasta umbral (3)
        resourceManager.recordFailure("GROQ_TEST_5XX", 500);
        assertFalse(resourceManager.isCircuitOpen("GROQ_TEST_5XX"));
        resourceManager.recordFailure("GROQ_TEST_5XX", 502);
        assertFalse(resourceManager.isCircuitOpen("GROQ_TEST_5XX"));
        resourceManager.recordFailure("GROQ_TEST_5XX", 503);
        assertTrue(resourceManager.isCircuitOpen("GROQ_TEST_5XX"));

        // Verificar que con groqEnabled=false nunca adquiere cuota real
        ExecutorService pool = Executors.newFixedThreadPool(4);
        CountDownLatch latch = new CountDownLatch(4);
        AtomicInteger acquired = new AtomicInteger(0);
        for (int i = 0; i < 4; i++) {
            pool.submit(() -> {
                try {
                    if (resourceManager.tryAcquire("GROQ", 1L, 600L)) {
                        acquired.incrementAndGet();
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        pool.shutdown();
        assertEquals(0, acquired.get());
    }

    @Test
    void demoDataSeederBloqueaEsquemaHabitualYEsIdempotente() {
        DemoDataSeeder seeder = new DemoDataSeeder(
                usuarioRepository,
                grupoRepository,
                miembroGrupoRepository,
                gastoRepository,
                pagoGastoRepository,
                gastoParticipanteRepository,
                borradorOperacionRepository,
                liquidacionService,
                passwordEncoder
        );

        ReflectionTestUtils.setField(seeder, "datasourceUrl", "jdbc:mysql://localhost:3306/gestor_gastos");
        assertThrows(IllegalStateException.class, () -> seeder.run());

        ReflectionTestUtils.setField(seeder, "datasourceUrl", "jdbc:mysql://localhost:3306/nexus_demo");
        seeder.run();
        long usuariosTrasPrimero = usuarioRepository.count();
        long gruposTrasPrimero = grupoRepository.count();
        assertTrue(usuarioRepository.findByEmailIgnoreCase("demo.a@nexus.local").isPresent());

        // Segunda ejecucion idempotente
        seeder.run();
        assertEquals(usuariosTrasPrimero, usuarioRepository.count());
        assertEquals(gruposTrasPrimero, grupoRepository.count());
    }
}
