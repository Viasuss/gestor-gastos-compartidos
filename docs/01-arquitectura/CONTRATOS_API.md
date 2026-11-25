# Contratos Técnicos y API — Partly

Este documento distingue estrictamente entre:
1. **Contratos reales actuales (Thymeleaf SSR)** verificados en `main` @ `761bc67`.
2. **Contratos futuros propuestos (`/api/v1/*`)** para trabajo paralelo con mocks — **NO implementados aún**.

---

## 1. Contratos reales actuales (Thymeleaf SSR)

| Módulo | Método | Ruta | Entradas principales | Salida / Redirección |
|---|---|---|---|---|
| **Auth** | `GET` | `/registro` | — | Vista `registro` (`usuario`, `googleClientId`) |
| **Auth** | `POST` | `/registro` | `name`, `email`, `password` | `redirect:/login?registrado` o `?error=campos\|existe` |
| **Auth** | `GET` | `/login` | — | Vista `login` (`googleClientId`) |
| **Auth** | `POST` | `/login` | `email`, `password` | `redirect:/inicio` o `redirect:/login?error` |
| **Auth** | `POST` | `/auth/google` | `credential` | `redirect:/inicio` o `redirect:/login?error=google` |
| **Auth** | `POST` | `/logout` | — | `redirect:/login` |
| **Inicio** | `GET` | `/inicio` | — | Vista `inicio` (`gruposDestacados`, `balanceGeneral`, `balancePorGrupo`, `miembrosPorGrupo`, `invitacionesPendientes`) |
| **Grupos** | `GET` | `/grupo/crear` | — | Vista `crear-grupo` (`personasAgregadas`, `usuariosTemporales`) |
| **Grupos** | `POST` | `/grupo/agregar-usuario` | `email` | `redirect:/grupo/crear` (`?error=propio\|usuario`) |
| **Grupos** | `POST` | `/grupo/quitar-usuario` | `email` | `redirect:/grupo/crear` |
| **Grupos** | `POST` | `/grupo/crear` | `name` (máx 60) | `redirect:/inicio?grupoCreado=true&nombre=...` o `?error=nombre` |
| **Grupos** | `GET` | `/mis-grupos` | — | Vista `mis-grupos` (`miembros`, `balancePorGrupo`, `detallesGrupo`, `gruposActivosCount`) |
| **Grupos** | `GET` | `/grupo/{id}` | `@PathVariable id` | Vista `grupo` (`grupo`, `miembros`, `gastos`, `pagosPorGasto`, `deudas`, `balanceTotal`) |
| **Invitaciones** | `GET` | `/invitaciones` | — | Vista `invitaciones` (`invitaciones`) |
| **Invitaciones** | `POST` | `/invitacion/{id}/aceptar` | `@PathVariable id` | `redirect:/invitaciones` |
| **Invitaciones** | `POST` | `/invitacion/{id}/rechazar` | `@PathVariable id` | `redirect:/invitaciones` |
| **Gastos** | `GET` | `/grupo/{id}/gasto/nuevo` | `@PathVariable id` | Vista `agregar-gasto` (`grupo`, `miembros`) |
| **Gastos** | `POST` | `/grupo/{id}/gasto` | `descripcion`, `monto`, `fecha`, `pagadoresId[]`, `montosPagados[]`, `participantes[]` | `redirect:/grupo/{id}` o `?error=descripcion\|monto\|fecha\|pagadores\|pagos\|pagomonto\|suma\|participantes` |

---

## 2. Ajustes de contrato SSR acordados para F1 (Pendientes de implementación)
1. **CSRF:** Exponer `_csrf` en todos los formularios Thymeleaf y meta tags.
2. **Ruta raíz:** Mapear `GET /` hacia `/inicio` (autenticado) o `/login` (anónimo).
3. **Filtro de miembros en `/grupo/{id}`:** Enviar solo miembros `MIEMBRO` (o separar pendientes/rechazados).
4. **Preservación de formularios:** Usar `RedirectAttributes` ante errores en `/grupo/{id}/gasto` y enviar `${mensaje}` al agregar usuario temporal.
5. **Estado Google en `/inicio`:** Exponer booleano `googleVinculado`.

---

## 3. Contratos JSON `/api/v1` propuestos (NO IMPLEMENTADOS — Para mocks en F2+)

> [!WARNING]
> **Estado: PROPUESTA PRELIMINAR.** Ninguno de estos endpoints existe en el código actual. Su implementación pertenece a F2+.

- **Convenciones:** Prefijo `/api/v1`, montos en `BigDecimal` serializados sin formato visual (`"50000.00"`) + `"moneda": "COP"`, cabecera `Idempotency-Key` en `POST` financieros, errores RFC 7807 (`ProblemDetail`).
- **Endpoints propuestos:**
  - `GET /api/v1/dashboard`
  - `GET /api/v1/grupos`, `GET /api/v1/grupos/{grupoId}`, `GET /api/v1/grupos/{grupoId}/deudas-simplificadas`
  - `POST /api/v1/grupos/{grupoId}/gastos`
  - `POST /api/v1/grupos/{grupoId}/liquidaciones`, `POST /api/v1/liquidaciones/{id}/confirmar`, `POST /api/v1/liquidaciones/{id}/rechazar`
  - `GET /api/v1/bandeja/borradores`, `POST /api/v1/bandeja/borradores/{id}/confirmar`, `POST /api/v1/bandeja/borradores/{id}/rechazar`
  - `POST /api/v1/ia/interpretar-texto`, `POST /api/v1/ia/interpretar-voz`, `POST /api/v1/ocr/procesar`
  - `POST /api/v1/integraciones/telegram/codigo-vinculacion`
  - `GET /api/v1/notificaciones`, `POST /api/v1/push/suscribir`
  - `GET /api/v1/admin/recursos/estado`
