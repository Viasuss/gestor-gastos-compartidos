# NEXUS — Asistente Inteligente de Finanzas Compartidas

> **Conectamos personas. Simplificamos cuentas.**

![NEXUS Isotipo](src/main/resources/static/img/nexus-isotipo.svg)

**NEXUS** (anteriormente *Partly*) es una aplicación web progresiva (PWA) desarrollada en **Java 21 + Spring Boot 4 + MySQL 8 + Thymeleaf** para la gestión determinista de gastos compartidos en grupo, liquidaciones bilaterales con confirmación humana, bandeja inteligente asistida por IA (Groq Free + reglas locales) y conectividad opcional vía Telegram Bot API.

---

## 1. Propuesta de Valor y Funcionalidades Reales

- **Núcleo financiero determinista (`BigDecimal`):** Registro de gastos con uno o múltiples pagadores, reparto equitativo entre participantes seleccionados, cálculo exacto de deudas bilaterales y conservación de suma cero.
- **Liquidaciones bilaterales (`B2`):** Solicitud de pago parcial o total, reserva sobre deuda disponible, confirmación por el receptor, rechazo, cancelación por el pagador, bloqueo pesimista a nivel de grupo e idempotencia (`idempotencyKey`).
- **Seguridad robusta (`B1`):** Spring Security con sesiones HTTP-Only/SameSite, protección CSRF, BCrypt, rate-limiting de `/login`, validación estricta de membresía activa (`GrupoAccessService`) y mitigación de *account pre-hijacking* en Google Sign-In.
- **Bandeja Inteligente y `ResourceManager` (`B3`):** Propuesta de gastos en lenguaje natural mediante **Groq Free** o **reglas locales (`LocalRulesProvider`)**, subordinada siempre a confirmación humana antes de impactar el libro financiero, con control de RPM, RPD, TPM, reserva de capacidad, concurrencia y *circuit breaker*.
- **Conectividad Telegram Bot API (`B4`):** Vinculación temporal con código `NX-...` (hash SHA-256 + expiración + rate-limit), comandos `/start`, `/ayuda`, `/vincular`, `/grupos`, `/saldo`, `/deudas`, `/gasto`, `/pendientes` y confirmación sincronizada con la web. Integración grupal deshabilitada por defecto (`TELEGRAM_GROUPS_ENABLED=false`).
- **Identidad NEXUS y PWA segura (`B5`):** Diseño unificado desktop/móvil, métricas operativas reales en `/inicio`, `manifest.webmanifest`, íconos PNG instalables y `sw.js` con *allow-list* estática que nunca cachea datos financieros ni HTML autenticado.

---

## 2. Stack Tecnológico

- **Backend:** Java 21, Spring Boot 4.1.1, Spring Web MVC, Spring Security, Spring Data JPA (Hibernate), Flyway.
- **Base de datos:** MySQL 8 (producción/demo), H2 en memoria (`MODE=MySQL`, perfil `test`).
- **Frontend:** Thymeleaf, HTML5, CSS3 (`partly.css` + `nexus.css`), JavaScript modular, PWA (`manifest.webmanifest`, `sw.js`).
- **Integraciones opcionales (`$0 COP`):** Google Identity Services, Groq Free API, Telegram Bot API.
- **Pruebas:** JUnit 5 (Jupiter), Mockito, MockMvc, Maven Wrapper (`mvnw` / `mvnw.cmd`).

---

## 3. Requisitos e Instalación

- **JDK 21** (`java -version`)
- **MySQL 8.0+** (para ejecución habitual o perfil `demo`)
- **Maven Wrapper** incluido (`./mvnw` en Linux/macOS, `.\mvnw.cmd` en Windows)

### Configuración de Base de Datos y Perfil `demo`

Para demostraciones seguras sin tocar la base habitual `gestor_gastos`, NEXUS incluye el perfil **`demo`** (`application-demo.properties`) con Flyway (`V1`–`V4`) y `ddl-auto=validate`:

```sql
CREATE DATABASE IF NOT EXISTS nexus_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'nexus_demo_user'@'localhost' IDENTIFIED BY '<CONTRASENA_SEGURA>';
GRANT ALL PRIVILEGES ON nexus_demo.* TO 'nexus_demo_user'@'localhost';
FLUSH PRIVILEGES;
```

Variables de entorno recomendadas (nunca almacenar secretos en el repositorio):

| Variable | Descripción | Valor por defecto |
|---|---|---|
| `NEXUS_DEMO_DB_URL` | URL JDBC del esquema demo | `jdbc:mysql://localhost:3306/nexus_demo?...` |
| `NEXUS_DEMO_DB_USERNAME` | Usuario dedicado MySQL | `nexus_demo_user` |
| `NEXUS_DEMO_DB_PASSWORD` | Contraseña del usuario demo | *(requerida)* |
| `NEXUS_DEMO_SEED_ENABLED` | Carga idempotente de datos sintéticos | `false` |
| `GROQ_ENABLED` / `GROQ_API_KEY` | Proveedor Groq Free | `false` / vacío |
| `TELEGRAM_ENABLED` / `TELEGRAM_BOT_TOKEN` | Bot de Telegram | `false` / vacío |
| `TELEGRAM_GROUPS_ENABLED` | Soporte grupal Telegram | `false` |
| `GOOGLE_CLIENT_ID` | Client ID Google Sign-In | Placeholder seguro |

### Ejecución

```powershell
# Perfil demo en Windows PowerShell (sin datos semilla)
$env:SPRING_PROFILES_ACTIVE="demo"
$env:NEXUS_DEMO_DB_PASSWORD="<tu_clave>"
.\mvnw.cmd spring-boot:run
```

### Ejecución de Pruebas Automatizadas (Aisladas en H2)

Las pruebas automatizadas usan el perfil `test` con H2 en memoria y proveedores externos deshabilitados:

```powershell
.\mvnw.cmd -o test
```

*(Si OneDrive bloquea la carpeta `target/` durante `clean`, copiar `pom.xml`, `mvnw`, `mvnw.cmd`, `.mvn/` y `src/` a un directorio temporal fuera de OneDrive y ejecutar `.\mvnw.cmd -o clean test`.)*

---

## 4. Documentación de Entrega

- [Arquitectura Final y Diagramas](docs/09-entrega/ARQUITECTURA_FINAL.md)
- [Modelo de Datos Final](docs/09-entrega/MODELO_DATOS_FINAL.md)
- [Guía de Instalación](docs/09-entrega/GUIA_INSTALACION.md)
- [Manual de Usuario](docs/09-entrega/MANUAL_USUARIO.md)
- [Manual Técnico](docs/09-entrega/MANUAL_TECNICO.md)
- [Guía de Demostración](docs/09-entrega/GUIA_DEMOSTRACION.md)
- [Guion de Sustentación](docs/09-entrega/GUION_SUSTENTACION.md)
- [Matriz de Validación Funcional](docs/09-entrega/MATRIZ_VALIDACION_FUNCIONAL.md)
- [Limitaciones y Trabajo Futuro](docs/09-entrega/LIMITACIONES_Y_TRABAJO_FUTURO.md)
- [Contribuciones del Equipo](docs/09-entrega/CONTRIBUCIONES_EQUIPO.md)
- [PWA: Instalación y Seguridad](docs/07-integraciones/PWA_INSTALACION_Y_SEGURIDAD.md)

---

## 5. Créditos

- **Johan Viasus (Desarrollador A):** Arquitectura backend, Spring Security, núcleo financiero y liquidaciones, Flyway `V1`–`V4`, `ResourceManager`, adaptador Groq Free, conectividad Telegram Bot API, unificación NEXUS/PWA y suite de pruebas automatizadas.
- **Wilmer (Desarrollador B):** Evolución y saneamiento frontend, accesibilidad WCAG en formularios y vistas, corrección XSS DOM en `crear-grupo.html`, validación `CASE_B` en `agregar-gasto.html` y diseño de experiencia base.
