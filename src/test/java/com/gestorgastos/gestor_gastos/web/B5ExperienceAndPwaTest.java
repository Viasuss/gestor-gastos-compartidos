package com.gestorgastos.gestor_gastos.web;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.service.telegram.TelegramRateLimiter;
import com.gestorgastos.gestor_gastos.support.ExternalProvidersTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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
class B5ExperienceAndPwaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private MiembroGrupoRepository miembroGrupoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TelegramRateLimiter telegramRateLimiter;

    @Value("${nexus.telegram.groups-enabled:false}")
    private boolean telegramGroupsEnabled;

    @Test
    void telegramGroupsEstaDesactivadoPorDefecto() {
        assertFalse(telegramGroupsEnabled, "nexus.telegram.groups-enabled debe ser false por defecto");
    }

    @Test
    void telegramRateLimiterBloqueaTrasCincoIntentosFallidosDeVinculacion() {
        long testTelegramUserId = 98765432101L;
        for (int i = 0; i < 5; i++) {
            assertTrue(telegramRateLimiter.allowLinkingAttempt(testTelegramUserId));
            telegramRateLimiter.recordLinkingFailure(testTelegramUserId);
        }
        assertFalse(telegramRateLimiter.allowLinkingAttempt(testTelegramUserId),
                "Debe bloquear temporalmente tras 5 intentos fallidos de vinculación");
    }

    @Test
    void recursosPublicosPwaSonAccesiblesYSeguros() throws Exception {
        mockMvc.perform(get("/manifest.webmanifest"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")))
                .andExpect(content().string(containsString("/img/icon-192.png")))
                .andExpect(content().string(containsString("/img/icon-512.png")));

        mockMvc.perform(get("/sw.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("STATIC_ALLOWLIST")))
                .andExpect(content().string(not(containsString("'/inicio'"))))
                .andExpect(content().string(not(containsString("'/grupo/'"))))
                .andExpect(content().string(not(containsString("'/api/'"))));

        mockMvc.perform(get("/offline.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")));

        mockMvc.perform(get("/img/icon-192.png")).andExpect(status().isOk());
        mockMvc.perform(get("/img/icon-512.png")).andExpect(status().isOk());
        mockMvc.perform(get("/css/nexus.css")).andExpect(status().isOk());
        mockMvc.perform(get("/js/pwa-register.js")).andExpect(status().isOk());
    }

    @Test
    void vistasPublicasRenderizanIdentidadNexusYManifest() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")))
                .andExpect(content().string(containsString("manifest.webmanifest")))
                .andExpect(content().string(containsString("_csrf")));

        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")))
                .andExpect(content().string(containsString("manifest.webmanifest")))
                .andExpect(content().string(containsString("_csrf")));
    }

    @Test
    void vistasAutenticadasRenderizanNavegacionUnificadaYMetricasReales() throws Exception {
        Usuario user = usuarioRepository.save(new Usuario(
                "Usuario B5",
                "usuariob5@test.local",
                passwordEncoder.encode("claveSegura123")
        ));
        Grupo grupo = grupoRepository.save(new Grupo("Viaje B5", LocalDate.now(), user));
        miembroGrupoRepository.save(new MiembroGrupo(user, grupo, EstadoMiembro.MIEMBRO));

        MvcResult loginResult = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "usuariob5@test.local")
                        .param("password", "claveSegura123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/inicio").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")))
                .andExpect(content().string(containsString("Borradores IA")))
                .andExpect(content().string(containsString("Liquidaciones")))
                .andExpect(content().string(containsString("Canal Telegram")))
                .andExpect(content().string(containsString("manifest.webmanifest")));

        mockMvc.perform(get("/mis-grupos").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")));

        mockMvc.perform(get("/asistente").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Asistente NEXUS")));

        mockMvc.perform(get("/integraciones").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")))
                .andExpect(content().string(containsString("_csrf")));

        mockMvc.perform(get("/grupo/" + grupo.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")));

        mockMvc.perform(get("/grupo/" + grupo.getId() + "/gasto/nuevo").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")))
                .andExpect(content().string(containsString("_csrf")));

        mockMvc.perform(get("/grupo/" + grupo.getId() + "/liquidar").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("NEXUS")))
                .andExpect(content().string(containsString("Registrar liquidaci")));
    }
}
