package com.gestorgastos.gestor_gastos.service.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestorgastos.gestor_gastos.entity.*;
import com.gestorgastos.gestor_gastos.repository.*;
import com.gestorgastos.gestor_gastos.service.BorradorOperacionService;
import com.gestorgastos.gestor_gastos.service.DeudaService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
class TelegramIntegrationTest {

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
    @Autowired private CuentaCanalRepository cuentaCanalRepository;
    @Autowired private CodigoVinculacionRepository codigoRepository;
    @Autowired private VinculacionChatGrupoRepository vinculacionChatGrupoRepository;
    @Autowired private TelegramUpdateProcesadoRepository updateProcesadoRepo;
    @Autowired private TelegramAccountLinkService linkService;
    @Autowired private TelegramUpdateRouter updateRouter;
    @Autowired private TelegramPollingService pollingService;
    @Autowired private DeudaService deudaService;
    @Autowired private PasswordEncoder passwordEncoder;

    @MockitoBean
    private TelegramBotClient botClient;

    private final ObjectMapper mapper = new ObjectMapper();
    private Usuario ana;
    private Usuario beto;
    private Grupo grupo;

    @BeforeEach
    void setUp() {
        when(botClient.isConfigured()).thenReturn(false);
        when(botClient.sendMessage(anyLong(), anyString(), any())).thenReturn(true);
        when(botClient.answerCallbackQuery(anyString(), anyString())).thenReturn(true);

        updateProcesadoRepo.deleteAll();
        vinculacionChatGrupoRepository.deleteAll();
        codigoRepository.deleteAll();
        cuentaCanalRepository.deleteAll();
        borradorRepository.deleteAll();
        liquidacionRepository.deleteAll();
        gastoParticipanteRepository.deleteAll();
        pagoGastoRepository.deleteAll();
        gastoRepository.deleteAll();
        miembroGrupoRepository.deleteAll();
        grupoRepository.deleteAll();
        usuarioRepository.deleteAll();

        ana = usuarioRepository.save(new Usuario("Ana", "ana.tg@test.local", passwordEncoder.encode("claveSegura123")));
        beto = usuarioRepository.save(new Usuario("Beto", "beto.tg@test.local", passwordEncoder.encode("claveSegura123")));

        grupo = grupoRepository.save(new Grupo("Hogar TG", LocalDate.now(), ana));
        miembroGrupoRepository.save(new MiembroGrupo(ana, grupo, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(beto, grupo, EstadoMiembro.MIEMBRO));
    }

    @Test
    void pollingNoIniciaSinTokenYVinculacionEsDeUnSoloUsoYExpirable() {
        assertFalse(pollingService.isRunning());

        String codigo = linkService.generarCodigoTemporal(ana);
        assertTrue(codigo.startsWith("NX-"));

        // Codigo incorrecto rechazado
        assertTrue(linkService.vincularPorCodigo(999001L, "NX-INVALIDO").isEmpty());

        // Codigo valido vincula y se marca usado
        Optional<CuentaCanal> vinculada = linkService.vincularPorCodigo(999001L, codigo);
        assertTrue(vinculada.isPresent());
        assertEquals(ana.getId(), vinculada.get().getUsuario().getId());

        // Reutilizacion del mismo codigo por otro telegramUserId es rechazada
        assertTrue(linkService.vincularPorCodigo(999002L, codigo).isEmpty());

        // Codigo expirado rechazado
        String codigoExp = linkService.generarCodigoTemporal(beto);
        CodigoVinculacion cv = codigoRepository.findByCodigoHash(TelegramAccountLinkService.sha256(codigoExp)).orElseThrow();
        cv.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
        codigoRepository.save(cv);
        assertTrue(linkService.vincularPorCodigo(999002L, codigoExp).isEmpty());
    }

    @Test
    void flujoTelegramCreaBorradorVisibleEnWebYConfirmaIdempotenteEntreCanales() throws Exception {
        String codigo = linkService.generarCodigoTemporal(ana);
        linkService.vincularPorCodigo(5001L, codigo).orElseThrow();

        // Seleccionar grupo via /grupo <id>
        updateRouter.procesarUpdate(mapper.readTree("""
                {"update_id":1001,"message":{"chat":{"id":5001,"type":"private"},"from":{"id":5001},"text":"/grupo %d"}}
                """.formatted(grupo.getId())));

        // Enviar gasto en lenguaje natural
        updateRouter.procesarUpdate(mapper.readTree("""
                {"update_id":1002,"message":{"chat":{"id":5001,"type":"private"},"from":{"id":5001},"text":"/gasto Pagué 100 lucas de mercado entre todos"}}
                """));

        // Actualizacion duplicada (mismo update_id) no crea segundo borrador
        updateRouter.procesarUpdate(mapper.readTree("""
                {"update_id":1002,"message":{"chat":{"id":5001,"type":"private"},"from":{"id":5001},"text":"/gasto Pagué 100 lucas de mercado entre todos"}}
                """));

        List<BorradorOperacion> borradores = borradorRepository.findByUsuarioIdOrderByFechaCreacionDesc(ana.getId());
        assertEquals(1, borradores.size());
        BorradorOperacion b = borradores.get(0);
        assertEquals("TELEGRAM", b.getOrigen());
        assertEquals(0, new BigDecimal("100000").compareTo(b.getMontoCop()));
        assertEquals(0, gastoRepository.count());

        // Tercero no vinculado o distinto NO puede confirmar via callback
        updateRouter.procesarUpdate(mapper.readTree("""
                {"update_id":1003,"callback_query":{"id":"cb-evil","from":{"id":999999},"message":{"chat":{"id":999999,"type":"private"}},"data":"CONF:%d"}}
                """.formatted(b.getId())));
        assertEquals(0, gastoRepository.count());

        // Confirmar via callback autorizado crea exactamente 1 gasto
        updateRouter.procesarUpdate(mapper.readTree("""
                {"update_id":1004,"callback_query":{"id":"cb-ok","from":{"id":5001},"message":{"chat":{"id":5001,"type":"private"}},"data":"CONF:%d"}}
                """.formatted(b.getId())));
        assertEquals(1, gastoRepository.count());
        assertEquals(new BigDecimal("50000"), deudaService.calcularBalanceTotal(grupo, ana));

        // Intentar confirmar el mismo borrador desde web es rechazado sin duplicar gasto
        assertThrows(IllegalStateException.class, () -> borradorService.confirmarBorrador(
                b.getId(), ana, "Mercado", new BigDecimal("100000"),
                LocalDate.now(), ana.getId(), List.of(ana.getId(), beto.getId()), "borrador-" + b.getId()));
        assertEquals(1, gastoRepository.count());
    }

    @Test
    void proteccionGruposPublicosYSeguridadWebIntegraciones() throws Exception {
        // En grupo publico, /vincular y /saldo son rechazados por privacidad
        updateRouter.procesarUpdate(mapper.readTree("""
                {"update_id":2001,"message":{"chat":{"id":-100777,"type":"group"},"from":{"id":5001},"text":"/vincular NX-12345678"}}
                """));
        updateRouter.procesarUpdate(mapper.readTree("""
                {"update_id":2002,"message":{"chat":{"id":-100777,"type":"group"},"from":{"id":5001},"text":"/saldo"}}
                """));
        verify(botClient, times(2)).sendMessage(eq(-100777L), contains("chat privado"), isNull());

        // Web /integraciones requiere auth y CSRF
        mockMvc.perform(post("/integraciones/telegram/codigo")).andExpect(status().isForbidden());

        MvcResult login = mockMvc.perform(post("/login").with(csrf())
                        .param("email", "ana.tg@test.local")
                        .param("password", "claveSegura123"))
                .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);

        mockMvc.perform(get("/integraciones").session(session)).andExpect(status().isOk());
        mockMvc.perform(post("/integraciones/telegram/codigo").with(csrf()).session(session))
                .andExpect(status().is3xxRedirection());
    }
}
