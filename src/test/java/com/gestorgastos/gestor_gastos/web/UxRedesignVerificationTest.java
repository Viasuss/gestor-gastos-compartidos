package com.gestorgastos.gestor_gastos.web;

import com.gestorgastos.gestor_gastos.entity.BorradorOperacion;
import com.gestorgastos.gestor_gastos.entity.CuentaCanal;
import com.gestorgastos.gestor_gastos.entity.EstadoBorrador;
import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.BorradorOperacionRepository;
import com.gestorgastos.gestor_gastos.repository.CuentaCanalRepository;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.service.GastoService;
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
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
@Transactional
class UxRedesignVerificationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private MiembroGrupoRepository miembroGrupoRepository;

    @Autowired
    private BorradorOperacionRepository borradorOperacionRepository;

    @Autowired
    private CuentaCanalRepository cuentaCanalRepository;

    @Autowired
    private GastoService gastoService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private MockHttpSession loginAs(String email, String rawPassword) throws Exception {
        MvcResult result = mockMvc.perform(post("/login").with(csrf())
                        .param("email", email)
                        .param("password", rawPassword))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }

    @Test
    void designSystemAssetsYPwaV2SonPublicosYSeguros() throws Exception {
        List<String> publicAssets = List.of(
                "/css/nexus-tokens.css",
                "/css/nexus-shell.css",
                "/css/nexus-components.css",
                "/css/nexus-views.css",
                "/css/nexus.css",
                "/js/nexus-shell.js",
                "/js/pwa-register.js",
                "/offline.html"
        );
        for (String path : publicAssets) {
            mockMvc.perform(get(path)).andExpect(status().isOk());
        }

        mockMvc.perform(get("/sw.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nexus-static-v3")))
                .andExpect(content().string(containsString("/css/nexus-tokens.css")))
                .andExpect(content().string(containsString("/js/nexus-shell.js")))
                .andExpect(content().string(not(containsString("'/inicio'"))))
                .andExpect(content().string(not(containsString("'/bandeja'"))));
    }

    @Test
    void vistasPublicasRenderizanSplitScreenSinDependenciaExternaDeFuentes() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nx-auth-page")))
                .andExpect(content().string(not(containsString("fonts.googleapis.com"))))
                .andExpect(content().string(not(containsString("partly.css"))));

        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("nx-auth-page")))
                .andExpect(content().string(containsString("confirmPassword")))
                .andExpect(content().string(not(containsString("fonts.googleapis.com"))));
    }

    @Test
    void appShellUnificadoEliminaNavegacionDuplicadaYBotonesFalsos() throws Exception {
        Usuario user = usuarioRepository.save(new Usuario(
                "Usuario UX",
                "ux.user@test.local",
                passwordEncoder.encode("claveSegura123")
        ));
        Grupo grupo = grupoRepository.save(new Grupo("Grupo UX Principal", LocalDate.now(), user));
        miembroGrupoRepository.save(new MiembroGrupo(user, grupo, EstadoMiembro.MIEMBRO));

        MockHttpSession session = loginAs("ux.user@test.local", "claveSegura123");

        List<String> authRoutes = List.of(
                "/inicio",
                "/mis-grupos",
                "/grupo/crear",
                "/invitaciones",
                "/integraciones",
                "/asistente",
                "/grupo/" + grupo.getId(),
                "/grupo/" + grupo.getId() + "/gasto/nuevo",
                "/grupo/" + grupo.getId() + "/liquidar"
        );

        for (String route : authRoutes) {
            mockMvc.perform(get(route).session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("id=\"nxSidebar\"")))
                    .andExpect(content().string(containsString("class=\"nx-topbar\"")))
                    .andExpect(content().string(containsString("id=\"panelControlBackdrop\"")))
                    .andExpect(content().string(not(containsString("class=\"desktop-nav\""))))
                    .andExpect(content().string(not(containsString("mostrarRequerimientosGoogle"))))
                    .andExpect(content().string(not(containsString("fonts.googleapis.com"))))
                    .andExpect(content().string(not(containsString("partly.css"))));
        }

        // Verificar que la fecha del formulario de gasto es editable
        mockMvc.perform(get("/grupo/" + grupo.getId() + "/gasto/nuevo").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"fecha\"")))
                .andExpect(content().string(not(containsString("readonly"))));
    }

    @Test
    void inicioMuestraEstadoVacioYTambienConteoRealCuandoHayMasDeCuatroGrupos() throws Exception {
        Usuario soloUser = usuarioRepository.save(new Usuario(
                "Usuario Vacio",
                "ux.vacio@test.local",
                passwordEncoder.encode("claveSegura123")
        ));
        MockHttpSession sessionVacia = loginAs("ux.vacio@test.local", "claveSegura123");

        mockMvc.perform(get("/inicio").session(sessionVacia))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Grupos activos (<span>0</span>)")))
                .andExpect(content().string(containsString("Crear mi primer grupo")));

        // Agregar 5 grupos al usuario
        for (int i = 1; i <= 5; i++) {
            Grupo g = grupoRepository.save(new Grupo("Grupo Num " + i, LocalDate.now(), soloUser));
            miembroGrupoRepository.save(new MiembroGrupo(soloUser, g, EstadoMiembro.MIEMBRO));
        }

        mockMvc.perform(get("/inicio").session(sessionVacia))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Grupos activos (<span>5</span>)")))
                .andExpect(content().string(containsString("Mostrando 4 destacados de 5 grupos activos.")));

        mockMvc.perform(get("/inicio")
                        .param("grupoCreado", "true")
                        .param("nombre", "Viaje UX")
                        .session(sessionVacia))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Grupo «Viaje UX» creado correctamente.")))
                .andExpect(content().string(not(containsString("param.nombre"))));
    }

    @Test
    void asistenteRenderizaMontoEnteroSinSufijoDecimalCeroYPermiteConfirmar() throws Exception {
        Usuario u = usuarioRepository.save(new Usuario(
                "Usuario Bandeja UX",
                "ux.bandeja@test.local",
                passwordEncoder.encode("claveSegura123")
        ));
        Grupo g = grupoRepository.save(new Grupo("Apartamento UX", LocalDate.now(), u));
        miembroGrupoRepository.save(new MiembroGrupo(u, g, EstadoMiembro.MIEMBRO));

        BorradorOperacion b = new BorradorOperacion();
        b.setUsuario(u);
        b.setGrupo(g);
        b.setConcepto("Mercado mensual");
        b.setMontoCop(new BigDecimal("60000.00"));
        b.setFechaSugerida(LocalDate.now());
        b.setEstado(EstadoBorrador.REQUIERE_ACLARACION);
        b.setOrigen("TELEGRAM");
        b.setProveedorUtilizado("LOCAL_RULES");
        b.setFechaCreacion(LocalDateTime.now());
        b.setFechaActualizacion(LocalDateTime.now());
        b.setFechaExpiracion(LocalDateTime.now().plusHours(24));
        borradorOperacionRepository.save(b);

        MockHttpSession session = loginAs("ux.bandeja@test.local", "claveSegura123");

        mockMvc.perform(get("/asistente").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("value=\"60000\"")))
                .andExpect(content().string(containsString("Requiere aclaración")))
                .andExpect(content().string(containsString("Telegram")))
                .andExpect(content().string(containsString("Interpretación local")));
    }

    @Test
    void grupoConGastosDeudasEIntegracionTelegramVinculadaRenderizanCompleto() throws Exception {
        Usuario ana = usuarioRepository.save(new Usuario(
                "Ana Detalle",
                "ana.detalle@test.local",
                passwordEncoder.encode("claveSegura123")
        ));
        Usuario bruno = usuarioRepository.save(new Usuario(
                "Bruno Detalle",
                "bruno.detalle@test.local",
                passwordEncoder.encode("claveSegura123")
        ));
        Grupo grupo = grupoRepository.save(new Grupo("Grupo Con Gastos", LocalDate.now(), ana));
        miembroGrupoRepository.save(new MiembroGrupo(ana, grupo, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(bruno, grupo, EstadoMiembro.MIEMBRO));

        gastoService.registrarGasto(
                grupo.getId(),
                ana,
                "Cena compartida UX",
                new BigDecimal("80000"),
                LocalDate.now(),
                List.of(ana.getId()),
                List.of(new BigDecimal("80000")),
                List.of(ana.getId(), bruno.getId()),
                "ux-test-gasto-1"
        );

        CuentaCanal canal = new CuentaCanal();
        canal.setUsuario(ana);
        canal.setCanal("TELEGRAM");
        canal.setIdentificadorExterno(555123456L);
        canal.setActiva(true);
        canal.setFechaVinculacion(LocalDateTime.now());
        cuentaCanalRepository.save(canal);

        MockHttpSession session = loginAs("ana.detalle@test.local", "claveSegura123");

        mockMvc.perform(get("/grupo/" + grupo.getId()).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cena compartida UX")))
                .andExpect(content().string(containsString("80.000 COP")))
                .andExpect(content().string(containsString("Deudas por persona")))
                .andExpect(content().string(containsString("Bruno Detalle")))
                .andExpect(content().string(containsString("Miembros (<span>2</span>)")));

        mockMvc.perform(get("/integraciones").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("555123456")))
                .andExpect(content().string(containsString("Desvincular Telegram")));
    }
}
