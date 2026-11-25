package com.gestorgastos.gestor_gastos.service.telegram;

import com.gestorgastos.gestor_gastos.entity.CuentaCanal;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.CodigoVinculacionRepository;
import com.gestorgastos.gestor_gastos.repository.CuentaCanalRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.support.ExternalProvidersTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(ExternalProvidersTestConfig.class)
@Transactional
class TelegramRelinkIntegrationTest {

    @Autowired
    private TelegramAccountLinkService linkService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CuentaCanalRepository cuentaCanalRepository;

    @Autowired
    private CodigoVinculacionRepository codigoRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void desvinculacionYRevinculacionConOtroUsuarioLimpiaGrupoActivoYRespetaUnicidad() {
        String pass = passwordEncoder.encode("ClaveSegura123!");
        Usuario u1 = usuarioRepository.save(new Usuario("Personal", "u1.relink@test.local", pass));
        Usuario u2 = usuarioRepository.save(new Usuario("Demo", "u2.relink@test.local", pass));
        Usuario u3 = usuarioRepository.save(new Usuario("Atacante", "u3.relink@test.local", pass));

        long tgId = 888111222L;

        // 1. Vinculacion inicial u1
        String cod1 = linkService.generarCodigoTemporal(u1);
        Optional<CuentaCanal> c1 = linkService.vincularPorCodigo(tgId, cod1);
        assertTrue(c1.isPresent());
        c1.get().setGrupoActivoId(99L);
        cuentaCanalRepository.saveAndFlush(c1.get());

        // Reutilizacion de cod1 rechazada
        assertTrue(linkService.vincularPorCodigo(tgId, cod1).isEmpty());

        // 2. Desvinculacion u1 limpia activa y grupoActivoId
        linkService.desvincularUsuario(u1);
        cuentaCanalRepository.flush();
        assertTrue(linkService.obtenerCuentaActivaPorUsuario(u1.getId()).isEmpty());

        // 3. Revinculacion legitima con u2 usando mismo tgId (antes lanzaba DataIntegrityViolationException)
        String cod2 = linkService.generarCodigoTemporal(u2);
        Optional<CuentaCanal> c2 = linkService.vincularPorCodigo(tgId, cod2);
        cuentaCanalRepository.flush();
        assertTrue(c2.isPresent());
        assertEquals(u2.getId(), c2.get().getUsuario().getId());
        assertTrue(c2.get().isActiva());
        assertNull(c2.get().getGrupoActivoId(), "No debe heredar el grupoActivoId de u1");

        // 4. u3 no puede apropiarse del tgId mientras u2 sigue activo y su codigo no se consume
        String cod3 = linkService.generarCodigoTemporal(u3);
        assertTrue(linkService.vincularPorCodigo(tgId, cod3).isEmpty());
        assertFalse(codigoRepository.findByCodigoHash(TelegramAccountLinkService.sha256(cod3)).orElseThrow().isUsado());
    }
}
