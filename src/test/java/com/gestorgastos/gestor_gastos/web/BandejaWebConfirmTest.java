package com.gestorgastos.gestor_gastos.web;

import com.gestorgastos.gestor_gastos.entity.*;
import com.gestorgastos.gestor_gastos.repository.*;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
@Transactional
class BandejaWebConfirmTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private MiembroGrupoRepository miembroGrupoRepository;

    @Autowired
    private BorradorOperacionRepository borradorRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void confirmaBorradorTelegramRequiereAclaracionDesdeFormularioWebEIdempotencia() throws Exception {
        String pass = passwordEncoder.encode("ClaveSegura123!");
        Usuario a = usuarioRepository.save(new Usuario("Demo A", "bw.a@test.local", pass));
        Usuario b = usuarioRepository.save(new Usuario("Demo B", "bw.b@test.local", pass));
        Usuario c = usuarioRepository.save(new Usuario("Demo C", "bw.c@test.local", pass));

        Grupo g = grupoRepository.save(new Grupo("Apartamento Demo", LocalDate.now(), a));
        miembroGrupoRepository.save(new MiembroGrupo(a, g, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(b, g, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(c, g, EstadoMiembro.MIEMBRO));

        LocalDateTime now = LocalDateTime.now();
        BorradorOperacion borrador = new BorradorOperacion();
        borrador.setUsuario(a);
        borrador.setGrupo(g);
        borrador.setTipoOperacion("REGISTRO_GASTO");
        borrador.setConcepto("Prueba multicanal");
        borrador.setMontoCop(new BigDecimal("60000.00"));
        borrador.setFechaSugerida(LocalDate.of(2026, 11, 25));
        borrador.setEstado(EstadoBorrador.REQUIERE_ACLARACION);
        borrador.setOrigen("TELEGRAM");
        borrador.setProveedorUtilizado("LOCAL_RULES");
        borrador.setFechaCreacion(now);
        borrador.setFechaActualizacion(now);
        borrador.setFechaExpiracion(now.plusDays(7));
        borrador.setIdempotencyKey("tg-m01-test");
        borrador = borradorRepository.save(borrador);

        MvcResult loginRes = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "bw.a@test.local")
                        .param("password", "ClaveSegura123!"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginRes.getRequest().getSession(false);

        // Verifica que /asistente renderiza 60000 sin .00 y etiquetas legibles
        mockMvc.perform(get("/asistente").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"60000\"")))
                .andExpect(content().string(containsString("Requiere aclaraci")))
                .andExpect(content().string(containsString("Interpretaci")))
                .andExpect(content().string(containsString("Telegram")));

        long gastosAntes = gastoRepository.count();

        // Confirmacion web
        mockMvc.perform(post("/bandeja/" + borrador.getId() + "/confirmar").with(csrf()).session(session)
                        .param("concepto", "Prueba multicanal NEXUS")
                        .param("montoCop", "60000")
                        .param("fecha", "2026-11-25")
                        .param("pagadorId", a.getId().toString())
                        .param("participantesIds", a.getId().toString(), b.getId().toString(), c.getId().toString())
                        .param("confirmIdempotencyKey", "confirm-" + borrador.getId()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/grupo/" + g.getId() + "?gastoCreado=true"));

        assertEquals(gastosAntes + 1, gastoRepository.count());
        assertEquals(EstadoBorrador.CONFIRMADO, borradorRepository.findById(borrador.getId()).orElseThrow().getEstado());

        // Segundo intento idempotente no duplica
        mockMvc.perform(post("/bandeja/" + borrador.getId() + "/confirmar").with(csrf()).session(session)
                        .param("concepto", "Prueba multicanal NEXUS")
                        .param("montoCop", "60000")
                        .param("fecha", "2026-11-25")
                        .param("pagadorId", a.getId().toString())
                        .param("participantesIds", a.getId().toString(), b.getId().toString(), c.getId().toString())
                        .param("confirmIdempotencyKey", "confirm-" + borrador.getId()))
                .andExpect(status().is3xxRedirection());

        assertEquals(gastosAntes + 1, gastoRepository.count());
    }
}
