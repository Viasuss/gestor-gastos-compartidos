package com.gestorgastos.gestor_gastos.service.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gestorgastos.gestor_gastos.entity.*;
import com.gestorgastos.gestor_gastos.repository.*;
import com.gestorgastos.gestor_gastos.service.BorradorOperacionService;
import com.gestorgastos.gestor_gastos.support.ExternalProvidersTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
@Transactional
class MultichannelIdempotencyTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private MiembroGrupoRepository miembroGrupoRepository;

    @Autowired
    private BorradorOperacionService borradorService;

    @Autowired
    private BorradorOperacionRepository borradorRepository;

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private TelegramAccountLinkService linkService;

    @Autowired
    private TelegramUpdateRouter updateRouter;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void confirmacionWebYReintentoTelegramMantienenIdempotenciaYAutorizacion() throws Exception {
        String pass = passwordEncoder.encode("ClaveSegura123!");
        Usuario a = usuarioRepository.save(new Usuario("Multicanal A", "mc.a@test.local", pass));
        Usuario b = usuarioRepository.save(new Usuario("Multicanal B", "mc.b@test.local", pass));
        Usuario ajeno = usuarioRepository.save(new Usuario("Ajeno", "mc.ajeno@test.local", pass));

        Grupo g = grupoRepository.save(new Grupo("Grupo Multicanal", LocalDate.now(), a));
        miembroGrupoRepository.save(new MiembroGrupo(a, g, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(b, g, EstadoMiembro.MIEMBRO));

        String cod = linkService.generarCodigoTemporal(a);
        linkService.vincularPorCodigo(555666777L, cod);

        BorradorOperacion borrador = borradorService.crearDesdeTexto(
                a, g.getId(), "Pague 60000 pesos de prueba multicanal para todos", "mc-key-1");
        long gastosAntes = gastoRepository.count();

        // Usuario ajeno no puede confirmar
        assertThrows(SecurityException.class, () -> borradorService.confirmarBorrador(
                borrador.getId(), ajeno, "Prueba multicanal", new BigDecimal("60000.00"),
                LocalDate.now(), a.getId(), List.of(a.getId(), b.getId()), "conf-ajeno"));

        // Confirmacion web unica
        Gasto gasto = borradorService.confirmarBorrador(
                borrador.getId(), a, "Prueba multicanal NEXUS", new BigDecimal("60000.00"),
                LocalDate.now(), a.getId(), List.of(a.getId(), b.getId()), "conf-web-1");
        assertNotNull(gasto.getId());
        assertEquals(gastosAntes + 1, gastoRepository.count());
        assertEquals(EstadoBorrador.CONFIRMADO, borradorRepository.findById(borrador.getId()).orElseThrow().getEstado());

        // Reintento por callback desde Telegram sobre el borrador ya confirmado no duplica el gasto
        JsonNode cb = new ObjectMapper().readTree(
                "{\"update_id\":9001,\"callback_query\":{\"id\":\"cb1\",\"from\":{\"id\":555666777},\"message\":{\"chat\":{\"id\":555666777}},\"data\":\"CONF:" + borrador.getId() + "\"}}");
        updateRouter.procesarUpdate(cb);
        assertEquals(gastosAntes + 1, gastoRepository.count());
    }
}
