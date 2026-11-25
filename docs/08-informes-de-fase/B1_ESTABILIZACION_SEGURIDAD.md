# B1 — INFORME TÉCNICO DE ESTABILIZACIÓN, SEGURIDAD Y CONSISTENCIA FINANCIERA

**Proyecto:** Partly (`gestor-gastos`)  
**Rama:** `feature/acelerado-b1-seguridad` (desde `integracion/f1-1-local` @ `f69d82487cf468058e7f2431ec3b523b418cea14`)  
**Fecha:** 2026-11-24

---

## 1. Checkpoints completados
- **CP1 (Seguridad, CSRF, Identidad y Autorización):** `SecurityConfig` (`Spring Security` + CSRF por defecto + `changeSessionId()` + `POST /logout`), `LoginAttemptService` (rate-limit 5 intentos / 5 min), mitigación de *Account Pre-hijacking* (`googleSub` + `authProvider` + `emailVerified` en `Usuario` y `GoogleAuthService` con timeouts 3s/5s), `GrupoAccessService`, ruta `GET /`, filtro `EstadoMiembro.MIEMBRO` en `/grupo/{id}` (`HB-003`) y `googleVinculado` en `/inicio` (`HB-007`).
- **CP2 (Validación monetaria, `CASE_B` e Idempotencia):** `MoneyParser.parseCop` (rechaza negativos, decimales ambiguos, letras y separadores inválidos), `idempotencyKey` en `Gasto` con índice único `(grupo_id, idempotency_key)` + `RedirectAttributes`, y validación `CASE_B` en `agregar-gasto.html` (bloqueo de `submit` y eliminación del mensaje verde cuando un pagador marcado tiene `$0`).
- **CP3 (Persistencia, Flyway e Invariantes financieros):** `spring-boot-starter-flyway` + `flyway-mysql`, script `V1__baseline_schema.sql`, constraints `@Table(uniqueConstraints)` en `MiembroGrupo` y `GastoParticipante`, `DECIMAL(19,2)` en `Gasto.monto` y `PagoGasto.monto`, y corrección de `HA-014` en `DeudaService` (ajuste de delta de redondeo para garantizar suma cero).
- **CP4 (Regresión completa):** `27` pruebas ejecutadas (`27` aprobadas, `0` fallos, `0` errores, `0` omisiones) en `01:09 min` (`BUILD SUCCESS`).

## 2. Estado de hallazgos
- **Resueltos:** `HA-001`, `HB-001`, `HA-002`, `HA-003`, `HA-004`, `HA-005`, `HA-006`, `HA-011`, `HA-014`, `HB-003`, `HB-007`, `HB-008`, `CASE_B`.
- **Parcialmente resueltos:** `HA-007` (anotaciones JPA y `V1__baseline_schema.sql` listos; falta validación contra MySQL 8 desechable), `HA-012` (constraints base añadidos; auditoría `createdAt`/`@Version` diferida).
- **Pendientes:** `HA-009` (N+1), `HA-010` (sesgo residuo por ID), `HA-013` (DTOs/servicios completos), `HB-002`, `HB-005`, `HB-006`, `HB-009`, `HB-010` (en rama pendiente de B).

## 3. Veredicto técnico
**BLOQUE B1 COMPLETADO Y VERIFICADO (27/27 pruebas en entorno aislado H2).** Listo para avanzar al **Bloque B2 (Evolución Financiera)** previa autorización.
