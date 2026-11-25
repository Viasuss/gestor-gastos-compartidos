package com.gestorgastos.gestor_gastos.service;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.GastoParticipanteRepository;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.LiquidacionRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.PagoGastoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.support.ExternalProvidersTestConfig;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
class MoneyParserAndIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

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
    private LiquidacionRepository liquidacionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void moneyParserValidaFormatosCopYRechazaEntradasAmbiguasONegativas() {
        assertEquals(new BigDecimal("50000"), MoneyParser.parseCop("50000").orElseThrow());
        assertEquals(new BigDecimal("50000"), MoneyParser.parseCop("50.000").orElseThrow());
        assertEquals(new BigDecimal("1500000"), MoneyParser.parseCop("1.500.000").orElseThrow());
        assertEquals(new BigDecimal("50000"), MoneyParser.parseCop("$50.000").orElseThrow());
        assertEquals(BigDecimal.ZERO, MoneyParser.parseCop("0").orElseThrow());

        assertTrue(MoneyParser.parseCop("-5000").isEmpty());
        assertTrue(MoneyParser.parseCop("").isEmpty());
        assertTrue(MoneyParser.parseCop(null).isEmpty());
        assertTrue(MoneyParser.parseCop("50abc").isEmpty());
        assertTrue(MoneyParser.parseCop("50.00").isEmpty());
        assertTrue(MoneyParser.parseCop("50.000,50").isEmpty());
        assertTrue(MoneyParser.parseCop("50..000").isEmpty());
    }

    @Test
    void gastoControllerRechazaCaseBYGarantizaIdempotencia() throws Exception {
        liquidacionRepository.deleteAll();
        gastoParticipanteRepository.deleteAll();
        pagoGastoRepository.deleteAll();
        gastoRepository.deleteAll();
        miembroGrupoRepository.deleteAll();
        grupoRepository.deleteAll();
        usuarioRepository.deleteAll();

        Usuario u1 = usuarioRepository.save(new Usuario("U1", "u1@test.local", passwordEncoder.encode("claveSegura123")));
        Usuario u2 = usuarioRepository.save(new Usuario("U2", "u2@test.local", passwordEncoder.encode("claveSegura123")));
        Grupo g = grupoRepository.save(new Grupo("G1", LocalDate.now(), u1));
        miembroGrupoRepository.save(new MiembroGrupo(u1, g, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(u2, g, EstadoMiembro.MIEMBRO));

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "u1@test.local")
                        .param("password", "claveSegura123"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        // CASE_B: pagador u1 con monto vacio y u2 con 50.000 -> rechazado sin persistir
        mockMvc.perform(post("/grupo/" + g.getId() + "/gasto").with(csrf()).session(session)
                        .param("descripcion", "Mercado")
                        .param("monto", "50.000")
                        .param("fecha", LocalDate.now().toString())
                        .param("pagadoresId", u1.getId().toString(), u2.getId().toString())
                        .param("montosPagados", "", "50.000")
                        .param("participantes", u1.getId().toString(), u2.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grupo/" + g.getId() + "/gasto/nuevo?error=pagomonto"));
        assertEquals(0, gastoRepository.count());

        // Reenvio idempotente: dos POST con misma idempotencyKey crean 1 solo gasto
        String key = UUID.randomUUID().toString();
        mockMvc.perform(post("/grupo/" + g.getId() + "/gasto").with(csrf()).session(session)
                        .param("descripcion", "Mercado")
                        .param("monto", "50.000")
                        .param("fecha", LocalDate.now().toString())
                        .param("pagadoresId", u1.getId().toString())
                        .param("montosPagados", "50.000")
                        .param("participantes", u1.getId().toString(), u2.getId().toString())
                        .param("idempotencyKey", key))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grupo/" + g.getId()));

        mockMvc.perform(post("/grupo/" + g.getId() + "/gasto").with(csrf()).session(session)
                        .param("descripcion", "Mercado")
                        .param("monto", "50.000")
                        .param("fecha", LocalDate.now().toString())
                        .param("pagadoresId", u1.getId().toString())
                        .param("montosPagados", "50.000")
                        .param("participantes", u1.getId().toString(), u2.getId().toString())
                        .param("idempotencyKey", key))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grupo/" + g.getId()));

        assertEquals(1, gastoRepository.count());
    }
}
