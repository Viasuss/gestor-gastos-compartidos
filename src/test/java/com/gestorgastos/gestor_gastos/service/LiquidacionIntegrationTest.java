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
class LiquidacionIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private GrupoRepository grupoRepository;
    @Autowired private MiembroGrupoRepository miembroGrupoRepository;
    @Autowired private GastoRepository gastoRepository;
    @Autowired private PagoGastoRepository pagoGastoRepository;
    @Autowired private GastoParticipanteRepository gastoParticipanteRepository;
    @Autowired private LiquidacionRepository liquidacionRepository;
    @Autowired private LiquidacionService liquidacionService;
    @Autowired private DeudaService deudaService;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario ana;
    private Usuario beto;
    private Usuario ajeno;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        liquidacionRepository.deleteAll();
        gastoParticipanteRepository.deleteAll();
        pagoGastoRepository.deleteAll();
        gastoRepository.deleteAll();
        miembroGrupoRepository.deleteAll();
        grupoRepository.deleteAll();
        usuarioRepository.deleteAll();

        ana = usuarioRepository.save(new Usuario("Ana", "ana@test.local", passwordEncoder.encode("claveSegura123")));
        beto = usuarioRepository.save(new Usuario("Beto", "beto@test.local", passwordEncoder.encode("claveSegura123")));
        ajeno = usuarioRepository.save(new Usuario("Ajeno", "ajeno@test.local", passwordEncoder.encode("claveSegura123")));

        grupo = grupoRepository.save(new Grupo("Hogar B2", LocalDate.now(), beto));
        miembroGrupoRepository.save(new MiembroGrupo(ana, grupo, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(beto, grupo, EstadoMiembro.MIEMBRO));

        // Beto paga $150.000 entre Ana y Beto -> Ana debe $75.000 a Beto
        Gasto gasto = gastoRepository.save(new Gasto("Arriendo", new BigDecimal("150000"), LocalDate.now(), grupo));
        pagoGastoRepository.save(new PagoGasto(gasto, beto, new BigDecimal("150000")));
        gastoParticipanteRepository.save(new GastoParticipante(ana, grupo, gasto));
        gastoParticipanteRepository.save(new GastoParticipante(beto, grupo, gasto));
    }

    @Test
    void flujoCompletoPagoParcialConfirmacionBilateralEIdempotencia() {
        assertEquals(new BigDecimal("-75000"), deudaService.calcularBalanceTotal(grupo, ana));

        String key = UUID.randomUUID().toString();
        Liquidacion pendiente = liquidacionService.registrarSolicitud(
                grupo.getId(), ana, beto.getId(), new BigDecimal("50000"), MetodoPago.NEQUI, "Abono", key);

        // Idempotencia de registro
        Liquidacion repetida = liquidacionService.registrarSolicitud(
                grupo.getId(), ana, beto.getId(), new BigDecimal("50000"), MetodoPago.NEQUI, "Abono", key);
        assertEquals(pendiente.getId(), repetida.getId());

        // Pendiente NO afecta el balance
        assertEquals(new BigDecimal("-75000"), deudaService.calcularBalanceTotal(grupo, ana));

        // Solo el receptor puede confirmar
        assertThrows(SecurityException.class, () -> liquidacionService.confirmar(pendiente.getId(), ana));

        // Confirmacion por Beto actualiza balance a -25.000 / +25.000
        Liquidacion confirmada = liquidacionService.confirmar(pendiente.getId(), beto);
        assertEquals(EstadoLiquidacion.CONFIRMADA, confirmada.getEstado());
        assertEquals(new BigDecimal("-25000"), deudaService.calcularBalanceTotal(grupo, ana));
        assertEquals(new BigDecimal("25000"), deudaService.calcularBalanceTotal(grupo, beto));

        // Segunda confirmacion es idempotente y no descuenta nuevamente
        liquidacionService.confirmar(pendiente.getId(), beto);
        assertEquals(new BigDecimal("-25000"), deudaService.calcularBalanceTotal(grupo, ana));
    }

    @Test
    void validacionesFinancierasReservaPendientesYTransiciones() {
        // Monto 0, negativo, superior a deuda, pagador==receptor, usuario ajeno
        assertThrows(IllegalArgumentException.class, () ->
                liquidacionService.registrarSolicitud(grupo.getId(), ana, beto.getId(), BigDecimal.ZERO, MetodoPago.EFECTIVO, null, "k1"));
        assertThrows(IllegalArgumentException.class, () ->
                liquidacionService.registrarSolicitud(grupo.getId(), ana, beto.getId(), new BigDecimal("-100"), MetodoPago.EFECTIVO, null, "k2"));
        assertThrows(IllegalArgumentException.class, () ->
                liquidacionService.registrarSolicitud(grupo.getId(), ana, beto.getId(), new BigDecimal("80000"), MetodoPago.EFECTIVO, null, "k3"));
        assertThrows(IllegalArgumentException.class, () ->
                liquidacionService.registrarSolicitud(grupo.getId(), ana, ana.getId(), new BigDecimal("1000"), MetodoPago.EFECTIVO, null, "k4"));
        assertThrows(IllegalArgumentException.class, () ->
                liquidacionService.registrarSolicitud(grupo.getId(), ajeno, beto.getId(), new BigDecimal("1000"), MetodoPago.EFECTIVO, null, "k5"));

        // Reserva de pendientes: 50.000 pendiente + otro 30.000 pendiente (> 75.000) debe fallar
        Liquidacion p1 = liquidacionService.registrarSolicitud(
                grupo.getId(), ana, beto.getId(), new BigDecimal("50000"), MetodoPago.NEQUI, null, "k6");
        assertThrows(IllegalArgumentException.class, () ->
                liquidacionService.registrarSolicitud(grupo.getId(), ana, beto.getId(), new BigDecimal("30000"), MetodoPago.NEQUI, null, "k7"));

        // Cancelacion por pagador libera saldo y no altera balance
        liquidacionService.cancelar(p1.getId(), ana);
        assertEquals(new BigDecimal("-75000"), deudaService.calcularBalanceTotal(grupo, ana));
        assertThrows(IllegalStateException.class, () -> liquidacionService.confirmar(p1.getId(), beto));

        // Rechazo por receptor no altera balance
        Liquidacion p2 = liquidacionService.registrarSolicitud(
                grupo.getId(), ana, beto.getId(), new BigDecimal("25000"), MetodoPago.DAVIPLATA, null, "k8");
        liquidacionService.rechazar(p2.getId(), beto);
        assertEquals(new BigDecimal("-75000"), deudaService.calcularBalanceTotal(grupo, ana));
        assertThrows(IllegalStateException.class, () -> liquidacionService.cancelar(p2.getId(), ana));
    }

    @Test
    void seguridadCsrfYRenderizadoLiquidar() throws Exception {
        mockMvc.perform(post("/grupo/" + grupo.getId() + "/liquidar")
                        .param("receptorId", beto.getId().toString())
                        .param("monto", "10000"))
                .andExpect(status().isForbidden());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "ana@test.local")
                        .param("password", "claveSegura123"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/grupo/" + grupo.getId() + "/liquidar").session(session))
                .andExpect(status().isOk());
    }
}
