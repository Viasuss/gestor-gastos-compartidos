package com.gestorgastos.gestor_gastos.security;

import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.service.GoogleAuthService;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
class SecurityIntegrationTest {

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
    private GoogleAuthService googleAuthService;

    private Usuario userA;
    private Usuario userB;
    private Grupo grupoDeB;

    @BeforeEach
    void setUp() {
        miembroGrupoRepository.deleteAll();
        grupoRepository.deleteAll();
        usuarioRepository.deleteAll();

        userA = usuarioRepository.save(new Usuario("Usuario A", "usera@test.local", passwordEncoder.encode("claveSegura123")));
        userB = usuarioRepository.save(new Usuario("Usuario B", "userb@test.local", passwordEncoder.encode("claveSegura123")));

        grupoDeB = grupoRepository.save(new Grupo("Grupo Privado B", LocalDate.now(), userB));
        miembroGrupoRepository.save(new MiembroGrupo(userB, grupoDeB, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(userA, grupoDeB, EstadoMiembro.PENDIENTE));
    }

    @Test
    void rutasPrivadasRedirigenALoginSiEsAnonimo() throws Exception {
        mockMvc.perform(get("/")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/inicio")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/mis-grupos")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/grupo/crear")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
        mockMvc.perform(get("/invitaciones")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login"));
    }

    @Test
    void postSinCsrfEsRechazadoCon403() throws Exception {
        mockMvc.perform(post("/login")
                        .param("email", "usera@test.local")
                        .param("password", "claveSegura123"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/registro")
                        .param("name", "Nuevo")
                        .param("email", "nuevo@test.local")
                        .param("password", "claveSegura123"))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginExitosoYLogoutConCsrf() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "usera@test.local")
                        .param("password", "claveSegura123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(get("/").session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"));

        mockMvc.perform(get("/inicio").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/logout").with(csrf()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void accesoAGrupoAjenoOPendienteEsDenegado() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "usera@test.local")
                        .param("password", "claveSegura123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/grupo/" + grupoDeB.getId()).session(session))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/mis-grupos"));
    }

    @Test
    void mitigaPreHijackingEntreRegistroLocalYGoogleOAuth() throws Exception {
        when(googleAuthService.verificarToken("valid-google-token"))
                .thenReturn(Optional.of(new GoogleAuthService.GoogleUserInfo(
                        "google-sub-victima",
                        "usera@test.local",
                        "Victima",
                        null
                )));

        mockMvc.perform(post("/auth/google").with(csrf())
                        .param("credential", "valid-google-token"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=google"));
    }
}
