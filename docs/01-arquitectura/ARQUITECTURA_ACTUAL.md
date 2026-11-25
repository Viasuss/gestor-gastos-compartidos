# Arquitectura Actual — Partly (Línea Base `main` @ `761bc67`)

> **Fuente:** Hechos comprobados mediante auditorías F0.2.A y F0.2.B sobre el código fuente real.

---

## 1. Stack técnico real
- **Backend:** Java 21, Spring Boot `4.1.1` (`pom.xml`), Maven Wrapper.
- **Persistencia:** Spring Data JPA / Hibernate (`ddl-auto=update`), MySQL Community (`mysql-connector-j`), sin Flyway/Liquibase.
- **Seguridad:** `spring-security-crypto` (`BCryptPasswordEncoder`) y autenticación manual vía `HttpSession`. **Sin `spring-boot-starter-security` ni protección CSRF.**
- **Autenticación externa:** Google Identity Services (verificación síncrona contra `oauth2.googleapis.com/tokeninfo` en `GoogleAuthService`).
- **Frontend:** Thymeleaf SSR (`8` plantillas en `src/main/resources/templates/`), `1` archivo CSS (`partly.css`, 1.859 líneas), JS inline embebido (sin archivos `.js` externos ni fragmentos `th:fragment`).

---

## 2. Capas actuales del sistema

```text
[Navegador / Thymeleaf SSR]
        │  HTTP POST/GET (sin CSRF, HttpSession)
        ▼
[Controllers (5)]  ◄── Concentran validaciones, reglas de negocio, sesión y acceso a Repositorios
   ├── UsuarioController
   ├── GrupoController
   ├── MiembroGrupoController
   ├── InvitacionController
   └── GastoController
        │
        ├──► [Services (2)] : DeudaService, GoogleAuthService
        └──► [Repositories (6)] : JpaRepository por entidad
                  │
                  ▼
             [Entities (6) + Enum (1)] ──► [MySQL gestor_gastos (ddl-auto=update)]
```

---

## 3. Limitaciones estructurales comprobadas
1. **Controladores gordos:** Reglas de negocio, validaciones y control de sesión mezclados en los 5 controladores.
2. **Consultas N+1:** `DeudaService.calcularDeudas()` consulta pagos y participantes gasto por gasto; `MiembroGrupoController.verGrupo()` recalcula deudas dos veces.
3. **Ausencia de entorno de pruebas:** Solo existe `GestorGastosApplicationTests` con `@SpringBootTest` dependiente de MySQL local (bloqueado para ejecución segura).
4. **Deuda técnica frontend:** JS y `<head>` duplicados, estilos inline extensivos, XSS DOM en `crear-grupo.html`, `</div>` extra en `inicio.html`, brechas de accesibilidad y ausencia de capacidades PWA.
