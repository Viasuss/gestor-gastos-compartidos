package com.gestorgastos.gestor_gastos.config;

import com.gestorgastos.gestor_gastos.entity.BorradorOperacion;
import com.gestorgastos.gestor_gastos.entity.EstadoBorrador;
import com.gestorgastos.gestor_gastos.entity.EstadoMiembro;
import com.gestorgastos.gestor_gastos.entity.Gasto;
import com.gestorgastos.gestor_gastos.entity.GastoParticipante;
import com.gestorgastos.gestor_gastos.entity.Grupo;
import com.gestorgastos.gestor_gastos.entity.Liquidacion;
import com.gestorgastos.gestor_gastos.entity.MetodoPago;
import com.gestorgastos.gestor_gastos.entity.MiembroGrupo;
import com.gestorgastos.gestor_gastos.entity.PagoGasto;
import com.gestorgastos.gestor_gastos.entity.Usuario;
import com.gestorgastos.gestor_gastos.repository.BorradorOperacionRepository;
import com.gestorgastos.gestor_gastos.repository.GastoParticipanteRepository;
import com.gestorgastos.gestor_gastos.repository.GastoRepository;
import com.gestorgastos.gestor_gastos.repository.GrupoRepository;
import com.gestorgastos.gestor_gastos.repository.MiembroGrupoRepository;
import com.gestorgastos.gestor_gastos.repository.PagoGastoRepository;
import com.gestorgastos.gestor_gastos.repository.UsuarioRepository;
import com.gestorgastos.gestor_gastos.service.LiquidacionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Carga opcional e idempotente de datos sinteticos para demostracion (B6).
 * Solo se ejecuta bajo perfil "demo" con nexus.demo.seed-enabled=true y URL dedicada nexus_demo.
 */
@Component
@Profile("demo")
@ConditionalOnProperty(name = "nexus.demo.seed-enabled", havingValue = "true")
public class DemoDataSeeder implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final GrupoRepository grupoRepository;
    private final MiembroGrupoRepository miembroGrupoRepository;
    private final GastoRepository gastoRepository;
    private final PagoGastoRepository pagoGastoRepository;
    private final GastoParticipanteRepository gastoParticipanteRepository;
    private final BorradorOperacionRepository borradorOperacionRepository;
    private final LiquidacionService liquidacionService;
    private final PasswordEncoder passwordEncoder;

    private static final List<String> AUTHORIZED_DEMO_SCHEMAS = List.of("nexus_demo", "nexus_demo_v2");

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    @Value("${nexus.demo.users-password:}")
    private String demoUsersPassword;

    public DemoDataSeeder(
            UsuarioRepository usuarioRepository,
            GrupoRepository grupoRepository,
            MiembroGrupoRepository miembroGrupoRepository,
            GastoRepository gastoRepository,
            PagoGastoRepository pagoGastoRepository,
            GastoParticipanteRepository gastoParticipanteRepository,
            BorradorOperacionRepository borradorOperacionRepository,
            LiquidacionService liquidacionService,
            PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.grupoRepository = grupoRepository;
        this.miembroGrupoRepository = miembroGrupoRepository;
        this.gastoRepository = gastoRepository;
        this.pagoGastoRepository = pagoGastoRepository;
        this.gastoParticipanteRepository = gastoParticipanteRepository;
        this.borradorOperacionRepository = borradorOperacionRepository;
        this.liquidacionService = liquidacionService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!isAuthorizedDemoDatasource(datasourceUrl)) {
            throw new IllegalStateException(
                    "DemoDataSeeder bloqueado: el esquema no pertenece a la lista autorizada " + AUTHORIZED_DEMO_SCHEMAS);
        }

        if (usuarioRepository.findByEmailIgnoreCase("demo.a@nexus.local").isPresent()) {
            return;
        }

        String rawPassword = (demoUsersPassword != null && !demoUsersPassword.isBlank())
                ? demoUsersPassword
                : UUID.randomUUID().toString();
        String demoPass = passwordEncoder.encode(rawPassword);
        Usuario demoA = usuarioRepository.save(new Usuario("Usuario Demo A", "demo.a@nexus.local", demoPass));
        Usuario demoB = usuarioRepository.save(new Usuario("Usuario Demo B", "demo.b@nexus.local", demoPass));
        Usuario demoC = usuarioRepository.save(new Usuario("Usuario Demo C", "demo.c@nexus.local", demoPass));

        LocalDate hoy = LocalDate.now();

        // Grupo 1: Apartamento NEXUS Demo
        Grupo apto = grupoRepository.save(new Grupo("Apartamento NEXUS Demo", hoy, demoA));
        miembroGrupoRepository.save(new MiembroGrupo(demoA, apto, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(demoB, apto, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(demoC, apto, EstadoMiembro.MIEMBRO));

        Gasto internet = new Gasto("Internet fibra hogar", new BigDecimal("120000.00"), hoy, apto);
        internet.setIdempotencyKey("demo-seed-internet-1");
        internet = gastoRepository.save(internet);
        pagoGastoRepository.save(new PagoGasto(internet, demoA, new BigDecimal("120000.00")));
        gastoParticipanteRepository.save(new GastoParticipante(demoA, apto, internet));
        gastoParticipanteRepository.save(new GastoParticipante(demoB, apto, internet));
        gastoParticipanteRepository.save(new GastoParticipante(demoC, apto, internet));

        Gasto mercado = new Gasto("Mercado semanal", new BigDecimal("90000.00"), hoy, apto);
        mercado.setIdempotencyKey("demo-seed-mercado-1");
        mercado = gastoRepository.save(mercado);
        pagoGastoRepository.save(new PagoGasto(mercado, demoA, new BigDecimal("60000.00")));
        pagoGastoRepository.save(new PagoGasto(mercado, demoB, new BigDecimal("30000.00")));
        gastoParticipanteRepository.save(new GastoParticipante(demoA, apto, mercado));
        gastoParticipanteRepository.save(new GastoParticipante(demoB, apto, mercado));
        gastoParticipanteRepository.save(new GastoParticipante(demoC, apto, mercado));

        // Liquidacion confirmada parcial de B hacia A + pendiente de C hacia A
        Liquidacion liqConfirmada = liquidacionService.registrarSolicitud(
                apto.getId(), demoB, demoA.getId(), new BigDecimal("20000.00"),
                MetodoPago.NEQUI, "Abono parcial demo", "demo-seed-liq-1");
        liquidacionService.confirmar(liqConfirmada.getId(), demoA);

        liquidacionService.registrarSolicitud(
                apto.getId(), demoC, demoA.getId(), new BigDecimal("30000.00"),
                MetodoPago.TRANSFERENCIA_BANCARIA, "Transferencia pendiente demo", "demo-seed-liq-2");

        // Grupo 2: Viaje de Amigos (con invitacion pendiente para demoC)
        Grupo viaje = grupoRepository.save(new Grupo("Viaje de Amigos", hoy, demoB));
        miembroGrupoRepository.save(new MiembroGrupo(demoB, viaje, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(demoA, viaje, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(demoC, viaje, EstadoMiembro.PENDIENTE));

        // Grupo 3: Universidad
        Grupo uni = grupoRepository.save(new Grupo("Universidad", hoy, demoA));
        miembroGrupoRepository.save(new MiembroGrupo(demoA, uni, EstadoMiembro.MIEMBRO));
        miembroGrupoRepository.save(new MiembroGrupo(demoC, uni, EstadoMiembro.MIEMBRO));

        // Borradores sinteticos (origen WEB, reglas locales, nunca simulando Groq/Telegram reales)
        LocalDateTime ahora = LocalDateTime.now();
        BorradorOperacion b1 = new BorradorOperacion();
        b1.setUsuario(demoA);
        b1.setGrupo(apto);
        b1.setTipoOperacion("GASTO");
        b1.setConcepto("Aseo apartamento");
        b1.setMontoCop(new BigDecimal("45000.00"));
        b1.setCategoriaSugerida("HOGAR");
        b1.setFechaSugerida(hoy);
        b1.setMensajeOriginal("Pague 45000 de aseo del apartamento");
        b1.setDatosEstructurados("{\"origen\":\"DEMO_SEED\"}");
        b1.setEstado(EstadoBorrador.PENDIENTE);
        b1.setOrigen("WEB");
        b1.setProveedorUtilizado("LOCAL_RULES");
        b1.setFechaCreacion(ahora);
        b1.setFechaActualizacion(ahora);
        b1.setFechaExpiracion(ahora.plusDays(7));
        b1.setIdempotencyKey("demo-seed-borrador-1");
        borradorOperacionRepository.save(b1);
    }

    private boolean isAuthorizedDemoDatasource(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String normalized = url.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("jdbc:h2:mem:")) {
            return true;
        }
        if (!normalized.startsWith("jdbc:mysql://")) {
            return false;
        }
        int slashIdx = normalized.indexOf('/', "jdbc:mysql://".length());
        if (slashIdx < 0 || slashIdx + 1 >= normalized.length()) {
            return false;
        }
        String afterSlash = normalized.substring(slashIdx + 1);
        int queryIdx = afterSlash.indexOf('?');
        String schema = (queryIdx >= 0) ? afterSlash.substring(0, queryIdx) : afterSlash;
        return AUTHORIZED_DEMO_SCHEMAS.contains(schema);
    }
}
