# Matriz de Validación Funcional — NEXUS (B6)

| Área / Funcionalidad | Estado | Evidencia / Mecanismo de Verificación | Observaciones |
|---|---|---|---|
| Seguridad Spring Security, CSRF, BCrypt, acceso grupos | Verificado | H2 + MockMvc (`SecurityIntegrationTest`) | Cobertura automatizada |
| Núcleo financiero (`DeudaService`, suma cero, multi-pagador) | Verificado | H2 (`DeudaServiceCharacterizationTest`, `B6ValidationSuiteTest`) | `BigDecimal` determinista |
| Liquidaciones bilaterales, idempotencia y bloqueo pesimista | Verificado | H2 (`LiquidacionIntegrationTest`, `B6ValidationSuiteTest`) | Añadido `findByIdForUpdate` en `GrupoRepository` |
| Bandeja Inteligente, `ResourceManager`, circuit breaker | Verificado | H2 + Mocks (`ResourceManagerAndIaTest`, `B6ValidationSuiteTest`) | Lock `quotaLock` añadido |
| Telegram privado (código `NX-...`, rate-limit, comandos) | Verificado (simulado) | H2 + Mocks (`TelegramIntegrationTest`, `B5ExperienceAndPwaTest`) | Prueba real con bot pendiente de sesión del propietario |
| Telegram grupal | Deshabilitado por defecto | `TELEGRAM_GROUPS_ENABLED=false` + pruebas simuladas | Requiere autorización explícita para demo grupal |
| Identidad NEXUS, navegación y PWA (`manifest`, `sw.js`) | Verificado (MockMvc) | H2 + MockMvc (`B5ExperienceAndPwaTest`) | Inspección visual en navegador real pendiente de sesión UI |
| MySQL 8 (`nexus_demo`), Flyway `V1`–`V4` y `ddl-auto=validate` | Preparado | Scripts `V1`–`V4` + `application-demo.properties` + `DemoDataSeeder` | Pendiente de ejecución con credenciales MySQL del propietario |
| Groq API real (3 casos sintéticos) | Pendiente | `application-demo.properties` + `GroqProvider` | Requiere `GROQ_API_KEY` del propietario en entorno demo |
