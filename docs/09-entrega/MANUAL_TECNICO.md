# Manual Técnico — NEXUS

## 1. Estructura de Paquetes (`com.gestorgastos.gestor_gastos`)
- `config/`: `SecurityConfig`, `DemoDataSeeder`.
- `controller/`: `UsuarioController`, `GrupoController`, `MiembroGrupoController`, `GastoController`, `LiquidacionController`, `BorradorOperacionController`, `IntegracionesController`.
- `entity/`: `Usuario`, `Grupo`, `MiembroGrupo`, `Gasto`, `PagoGasto`, `GastoParticipante`, `Liquidacion`, `ConsumoRecurso`, `BorradorOperacion`, `CuentaCanal`, `CodigoVinculacion`, `VinculacionChatGrupo`, `TelegramUpdateProcesado`.
- `repository/`: Repositorios Spring Data JPA (incluyendo `GrupoRepository.findByIdForUpdate` con `PESSIMISTIC_WRITE`).
- `security/`: `GrupoAccessService`, `LoginAttemptService`, `MoneyInputParser`.
- `service/`: `DeudaService`, `LiquidacionService`, `BorradorOperacionService`, `GoogleAuthService`, `ia/` (`ResourceManager`, `ExpenseInterpreterService`, `GroqProvider`, `LocalRulesProvider`), `telegram/` (`TelegramBotClient`, `TelegramPollingRunner`, `TelegramUpdateRouter`, `TelegramAccountLinkService`, `TelegramGroupBindingService`, `TelegramRateLimiter`).

## 2. Reglas de Seguridad e Integridad Financiera
- **Dinero:** `BigDecimal` con escala explícita; `DECIMAL(19,2)` en MySQL.
- **Concurrencia:** `@Version` + `idempotencyKey` + bloqueo pesimista `PESSIMISTIC_WRITE` sobre `Grupo` al registrar o confirmar liquidaciones.
- **IA subordinada:** `GroqProvider` y `LocalRulesProvider` solo crean `BorradorOperacion`; únicamente la confirmación explícita por `POST` con CSRF crea un `Gasto`.
- **PWA segura:** `sw.js` aplica `STATIC_ALLOWLIST` estricta y nunca almacena vistas autenticadas ni endpoints financieros.
