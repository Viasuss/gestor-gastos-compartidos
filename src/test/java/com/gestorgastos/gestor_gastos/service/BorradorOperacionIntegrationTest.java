package com.gestorgastos.gestor_gastos.service;

import com.gestorgastos.gestor_gastos.entity.*;
import com.gestorgastos.gestor_gastos.repository.*;
import com.gestorgastos.gestor_gastos.support.ExternalProvidersTestConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
class BorradorOperacionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private GrupoRepository grupoRepository;
    @Autowired private MiembroGrupoRepository miembroGrupoRepository;
    @Autowired private GastoRepository gastoRepository;
    @Autowired private PagoGastoRepository pagoGastoRepository;
    @Autowired private GastoParticipanteRepository gastoParticipanteRepository;
    @Autowired private LiquidacionRepository liquidacionRepository;
    @Autowired private BorradorOperacionRepository borradorRepository;
    @Autowired private BorradorOperacionService borradorService;
    @Autowired private DeudaService deudaService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario ana;
    private Usuario beto;
    private Usuario ajeno;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        borradorRepository.deleteAll();
        liquidacionRepository.deleteAll();
        gastoParticipanteRepository.deleteAll();
        pagoGastoRepository.deleteAll();
        gastoRepository.deleteAll();
        miembroGrupoRepository.deleteAll();
        grupoRepository.deleteAll();
        usuarioRepository.deleteAll();

        ana = usuarioRepository.save(new Usuario("Ana", "ana.b3@test.local", passwordEncoder.encode("claveSegura123")));
        beto = usuarioRepository.save(new Usuario("Beto", "beto.b3@test.local", passwordEncoder.encode("claveSegura123")));
        ajeno = usuarioRepository.save(new Usuario("Ajeno", "ajeno.b3@test.local", passwordEncoder.encode("claveSegura123")));

        grupo = grupoRepository.save(new Grupo("Apto B3", LocalDate.now(), ana));
        miembroGrupoRepository.save(new MiembroGrupo(ana, grupo, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(beto, grupo, EstadoMiembro.MIEMBRO));
    }

    @Test
    void borradorNoAfectaBalancesYConfirmacionAtomicaCreaUnSoloGasto() {
        BorradorOperacion borrador = borradorService.crearDesdeTexto(
                ana, grupo.getId(), "Pagué 120 lucas de internet entre todos", UUID.randomUUID().toString());

        assertEquals(EstadoBorrador.PENDIENTE, borrador.getEstado());
        assertEquals(new BigDecimal("120000"), borrador.getMontoCop());
        assertEquals(0, gastoRepository.count());
        assertEquals(BigDecimal.ZERO, deudaService.calcularBalanceTotal(grupo, ana));

        // Usuario ajeno no puede confirmar ni descartar el borrador de Ana
        assertThrows(SecurityException.class, () -> borradorService.confirmarBorrador(
                borrador.getId(), ajeno, "Internet", new BigDecimal("120000"),
                LocalDate.now(), ana.getId(), List.of(ana.getId(), beto.getId()), "k-ajeno"));
        assertThrows(SecurityException.class, () -> borradorService.descartarBorrador(borrador.getId(), ajeno));

        // Confirmacion valida crea exactamente 1 gasto y actualiza balances
        Gasto gasto = borradorService.confirmarBorrador(
                borrador.getId(), ana, "Internet apto", new BigDecimal("120000"),
                LocalDate.now(), ana.getId(), List.of(ana.getId(), beto.getId()), "k-ok");

        assertNotNull(gasto.getId());
        assertEquals(1, gastoRepository.count());
        assertEquals(new BigDecimal("60000"), deudaService.calcularBalanceTotal(grupo, ana));
        assertEquals(new BigDecimal("-60000"), deudaService.calcularBalanceTotal(grupo, beto));

        // Confirmacion duplicada rechazada sin crear segundo gasto
        assertThrows(IllegalStateException.class, () -> borradorService.confirmarBorrador(
                borrador.getId(), ana, "Internet apto", new BigDecimal("120000"),
                LocalDate.now(), ana.getId(), List.of(ana.getId(), beto.getId()), "k-dup"));
        assertEquals(1, gastoRepository.count());
    }

    @Test
    void borradorExpiradoYDescartadoNoPuedenConfirmarse() {
        BorradorOperacion b1 = borradorService.crearDesdeTexto(
                ana, grupo.getId(), "Pagué 20 mil de café", "k-exp");
        b1.setFechaExpiracion(LocalDateTime.now().minusMinutes(5));
        borradorRepository.save(b1);

        assertThrows(IllegalStateException.class, () -> borradorService.confirmarBorrador(
                b1.getId(), ana, "Café", new BigDecimal("20000"),
                LocalDate.now(), ana.getId(), List.of(ana.getId(), beto.getId()), "k-c1"));
        assertEquals(0, gastoRepository.count());

        BorradorOperacion b2 = borradorService.crearDesdeTexto(
                ana, grupo.getId(), "Pagué 30 mil de taxi", "k-desc");
        borradorService.descartarBorrador(b2.getId(), ana);
        assertEquals(EstadoBorrador.DESCARTADO, borradorRepository.findById(b2.getId()).orElseThrow().getEstado());
    }

    @Test
    void seguridadCsrfYRenderizadoAsistente() throws Exception {
        mockMvc.perform(post("/asistente/interpretar")
                        .param("grupoId", grupo.getId().toString())
                        .param("texto", "Pagué 10 mil"))
                .andExpect(status().isForbidden());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "ana.b3@test.local")
                        .param("password", "claveSegura123"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/asistente").session(session))
                .andExpect(status().isOk());
    }
}
