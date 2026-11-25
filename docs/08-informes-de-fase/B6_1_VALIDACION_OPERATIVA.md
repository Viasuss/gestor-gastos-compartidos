# Informe de Fase — B6.1: Configuración Operativa Local, MySQL 8 Real y Validación Financiera

**Rama:** `feature/acelerado-b6-1-validacion-operativa`  
**Base RC1:** `release/nexus-v1.0-rc1` @ `07e6ce824b17ac04a751946d7012e557cce9e122`  
**Esquema validado:** `nexus_demo` (`nexus_demo_user` en MySQL 8.0.46)  
**Presupuesto externo:** `$0 COP`  
**Fecha:** 2026-11-25

---

## 1. Evidencias de Arranque, Flyway e Hibernate (Confirmado por el propietario)
- Configuración independiente `NEXUS - DEMO` en IntelliJ IDEA con perfil `demo`.
- Esquema `nexus_demo` (`utf8mb4`) y usuario `nexus_demo_user` con privilegios limitados a `nexus_demo` (`gestor_gastos` intacta).
- Migraciones `V1`–`V4` aplicadas exitosamente, `flyway_schema_history` verificada, `ddl-auto=validate` exitoso y segundo arranque sin reaplicación.
- Verificación HTTP directa: `LOGIN=200`, `MANIFEST=200`, `SW=200` en `http://localhost:8080`.

## 2. Endurecimiento de `DemoDataSeeder` (Verificado directamente + confirmado)
- Reforzado con lista positiva `AUTHORIZED_DEMO_SCHEMAS = ["nexus_demo", "nexus_demo_v2"]` (bloquea cualquier otro esquema incluyendo `gestor_gastos`) y generación aleatoria cuando `NEXUS_DEMO_USERS_PASSWORD` no está definido.
- Primer y segundo arranque en `demo` verificados por el propietario sin duplicación de registros.

## 3. Validación Financiera Real y Concurrencia sobre MySQL 8 (Verificado directamente vía HTTP)
Sobre grupo aislado `Grupo B61 Real` (`grupoId=8`) con 3 usuarios sintéticos (`Demo B61 A`, `Demo B61 B`, `Demo B61 C`):
- **Caso A ($120.000 Internet por A) + reenvío idempotente:** `A: +$80.000`, `B: -$40.000`, `C: -$40.000` (suma `$0`).
- **Caso B ($90.000 Mercado, A=$60.000, B=$30.000):** `A: +$110.000`, `B: -$40.000`, `C: -$70.000` (suma `$0`).
- **Caso C (liquidación parcial $20.000 B->A + rechazo por tercero C + confirmación A):** antes de confirmar `A: +$110.000`; tercero `C` rechazado (`?error=liquidacion`); tras confirmar `A`: `A: +$90.000`, `B: -$20.000`.
- **Defecto de concurrencia detectado y corregido:** En MySQL 8 `InnoDB` (`REPEATABLE READ`), las lecturas previas en la misma transacción fijaban el snapshot antes del `SELECT ... FOR UPDATE`, permitiendo que dos solicitudes paralelas con distinta `idempotencyKey` pasaran simultáneamente.
- **Corrección aplicada:** `@Transactional(isolation = Isolation.READ_COMMITTED)` + `grupoRepository.findByIdForUpdate(...)` (`PESSIMISTIC_WRITE`) en `LiquidacionService.registrarSolicitud` y `confirmar`.
- **Revalidación en vivo tras reinicio:** Ante 2 solicitudes paralelas de `$20.000` con distinta `idempotencyKey`, exactamente 1 creó la liquidación (`?liquidacion=pendiente`) y la otra fue rechazada (`?error=validacion`). Tras confirmar `A`, **Caso D final:** `A: +$70.000`, `B: A PAZ Y SALVO ($0)`, `C: -$70.000`.

## 4. Veredicto B6.1
- **APROBADA.** Se autoriza continuar con **B6.2 — Validación operativa real de Groq y Telegram**.
