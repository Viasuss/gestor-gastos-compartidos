# Guía de Ejecución de Pruebas — Partly

**Aplica a:** Desarrollador A (Backend/Dominio) y Desarrollador B (Frontend/@WebMvcTest/UI)  
**Última actualización:** Fase `F1.1.A`

---

## 1. Requisitos previos
- **Java 21** (verificado con OpenJDK 21 LTS).
- **Maven Wrapper** incluido en el repositorio (`.\mvnw.cmd` en Windows, `./mvnw` en Linux/macOS).
- **Sin Docker requerido** en esta etapa (las pruebas de integración usan H2 en memoria).
- **Presupuesto:** `$0 COP` (sin servicios externos ni credenciales reales).

## 2. Prohibición estricta sobre la base de datos habitual
> [!CAUTION]
> **PROHIBIDO ejecutar pruebas contra `jdbc:mysql://localhost:3306/gestor_gastos` o cualquier instancia MySQL real.**
> El entorno de pruebas incluye una guardia temprana (`TestIsolationEnvironmentPostProcessor`) que bloquea el arranque si el perfil `test` no está activo, si la URL no cumple la lista permitida `jdbc:h2:mem:...`, si `ddl-auto` es `update` o si el usuario es `root`.

## 3. Comandos de ejecución

```powershell
# Windows (PowerShell / CMD)
.\mvnw.cmd test

# Ejecutar una clase de prueba especifica
.\mvnw.cmd test -Dtest=DeudaServiceCharacterizationTest
```

```bash
# Linux / macOS
./mvnw test
```

## 4. Arquitectura y configuración aislada
1. **Nivel 1 — Pruebas unitarias puras (`@ExtendWith(MockitoExtension.class)`):** Sin contexto Spring ni BD; rápidas y deterministas (ej. `DeudaServiceCharacterizationTest`).
2. **Nivel 2 — Pruebas MVC aisladas (`@WebMvcTest`):** Para controladores y plantillas Thymeleaf (Desarrollador B), simulando servicios/repositorios con `@MockitoBean`.
3. **Nivel 3 — Pruebas de contexto Spring (`@SpringBootTest` + `@ActiveProfiles("test")`):**
   - `src/test/resources/application.properties` fuerza `spring.profiles.active=test`.
   - `src/test/resources/application-test.properties` configura `jdbc:h2:mem:partly_test_db;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE`, usuario sintético `partly_test`, `ddl-auto=create-drop`.
   - `TestIsolationEnvironmentPostProcessor` (`META-INF/spring.factories`) valida el entorno **antes** de crear `DataSource` o inicializar Hibernate (C2).
   - `TestDatabaseUrlValidator` aplica lista permitida estricta (C1).

## 5. Uso de mocks para proveedores externos (C3 + C4)
Toda prueba `@SpringBootTest` debe importar `ExternalProvidersTestConfig` (`@Import(ExternalProvidersTestConfig.class)`), que sustituye `GoogleAuthService` por un mock `@Primary` de Mockito, evitando llamadas HTTP salientes. Complementariamente, `application-test.properties` fija `google.client.id=YOUR_GOOGLE_CLIENT_ID_TEST_DISABLED` para activar el cortocircuito interno de `GoogleAuthService`.

## 6. Política de aislamiento futuro (C7)
`TestIsolationEnvironmentPostProcessor` se ejecuta durante `ApplicationEnvironmentPreparedEvent`, **antes** de que Spring cree el `DataSource` o inicialice Hibernate. Sin embargo, mecanismos que se aplican *después* de ese evento (`@DynamicPropertySource`, `@TestPropertySource`, `@Bean DataSource` personalizados o inicializadores SQL en tiempo de contexto) podrían sobreescribir propiedades de conexión.
Por política del proyecto:
- **Prohibido** utilizar `@DynamicPropertySource`, `@TestPropertySource`, `@Bean DataSource` alternativos o scripts `INIT`/`RUNSCRIPT` para apuntar a bases externas, en disco o TCP.
- Solo se autoriza el usuario sintético exacto `partly_test`, el perfil `test` y URLs H2 en memoria validadas por `TestDatabaseUrlValidator` (conjunto cerrado sin parámetros duplicados ni contradictorios).

## 7. Advertencias operativas conocidas (F11A-AUD-004)
Durante la ejecución con Java 21 y Spring Boot 4.1.1 se observan dos advertencias no bloqueantes:
1. Carga dinámica del agente ByteBuddy por Mockito (`byte-buddy-agent`).
2. `spring.jpa.open-in-view` habilitado por defecto en Spring Boot.
No deben silenciarse modificando opciones JVM ni `maven-surefire-plugin` en esta fase; se atenderán en fases posteriores.

## 8. Creación de nuevos tests y tratamiento de fallos
- Usar siempre datos sintéticos y deterministas.
- Si una prueba falla, **no deshabilitarla ni ocultar el fallo**: diagnosticar si es regresión propia o defecto preexistente (documentar con evidencia y hallazgo `HA-`/`HB-`).

## 9. Limitaciones conocidas de H2 (`MODE=MySQL`)
H2 en modo MySQL **no garantiza** paridad al 100% con MySQL 8 en: dialectos SQL avanzados, restricciones específicas, índices físicos, bloqueos (`FOR UPDATE`), concurrencia real, migraciones Flyway/Liquibase ni comportamiento transaccional específico. Estas validaciones quedan pendientes para una instancia MySQL desechable (Testcontainers/Docker) en fases posteriores.
