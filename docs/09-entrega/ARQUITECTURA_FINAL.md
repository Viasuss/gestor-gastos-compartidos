# Arquitectura Final — NEXUS

**Versión:** `v1.0-rc1` (Bloque B6)

---

## 1. Arquitectura General

```mermaid
flowchart LR
    U[Usuario Web / PWA] -->|HTTPS/HTTP + CSRF| SEC[Spring Security]
    TG[Usuario Telegram] -->|Long Polling| TGC[TelegramBotClient / UpdateRouter]
    SEC --> CTRL[Controladores MVC: Usuario, Grupo, Gasto, Liquidacion, Borrador, Integraciones]
    TGC --> SVC
    CTRL --> SVC[Servicios de Dominio: DeudaService, LiquidacionService, BorradorOperacionService]
    CTRL --> IA[ExpenseInterpreterService]
    TGC --> IA
    IA --> RM[ResourceManager: RPM/RPD/TPM/CircuitBreaker]
    RM -->|Habilitado| GROQ[GroqProvider]
    RM -->|Fallback / Sin red| LOCAL[LocalRulesProvider]
    SVC --> REPO[Repositorios Spring Data JPA]
    REPO --> DB[(MySQL 8 / H2 Test)]
```

## 2. Casos de Uso Principales

```mermaid
flowchart TD
    A((Usuario)) --> UC1[Registrarse / Iniciar Sesion / Google]
    A --> UC2[Crear Grupo e Invitar Miembros]
    A --> UC3[Registrar Gasto Manual Multi-pagador]
    A --> UC4[Interpretar Gasto en Lenguaje Natural]
    A --> UC5[Confirmar / Descartar Borrador en Bandeja]
    A --> UC6[Solicitar / Confirmar Liquidacion Bilateral]
    A --> UC7[Vincular Cuenta Telegram NX-...]
    A --> UC8[Instalar PWA NEXUS]
```

## 3. Flujo de Registro de Gasto Manual

```mermaid
sequenceDiagram
    participant U as Usuario
    participant C as GastoController
    participant S as GastoRepository / DeudaService
    participant DB as Base de Datos
    U->>C: POST /grupo/{id}/gasto/nuevo (CSRF + idempotencyKey + pagadores + participantes)
    C->>C: Valida membresia activa, montos > 0, suma pagadores == total, CASE_B
    C->>DB: Verifica idempotencyKey (grupo_id, idempotency_key)
    alt Nuevo gasto
        C->>DB: Persiste Gasto, PagoGasto y GastoParticipante
        C-->>U: Redirect /grupo/{id} con balance actualizado
    else Reintento duplicado
        C-->>U: Retorna estado sin duplicar registro
    end
```

## 4. Flujo de Liquidación Bilateral

```mermaid
sequenceDiagram
    participant P as Pagador
    participant R as Receptor
    participant LS as LiquidacionService
    participant DB as MySQL / JPA
    P->>LS: registrarSolicitud(grupoId, receptorId, monto, metodo, idempotencyKey)
    LS->>DB: findByIdForUpdate(grupoId) [PESSIMISTIC_WRITE]
    LS->>LS: Verifica deudaDisponible = deudaActual - pendientes
    LS->>DB: Guarda Liquidacion (PENDIENTE, version=0)
    R->>LS: confirmar(liquidacionId, receptor)
    LS->>DB: findByIdForUpdate(grupoId) [PESSIMISTIC_WRITE]
    LS->>LS: Valida receptor == autenticado y deudaActual >= monto
    LS->>DB: Actualiza estado = CONFIRMADA (@Version)
```

## 5. Flujo de Interpretación Inteligente y `ResourceManager`

```mermaid
flowchart TD
    IN[Texto natural] --> RM{ResourceManager.tryAcquire}
    RM -->|Cuota OK & Groq ON & Circuit Closed| GP[GroqProvider: JSON estricto]
    GP -->|HTTP 200 JSON valido| VAL[Validacion determinista]
    GP -->|429/401/403/5xx/Timeout| CB[recordFailure -> Circuit Breaker]
    CB --> LR[LocalRulesProvider]
    RM -->|Cuota agotada / Groq OFF / Circuit Open| LR
    LR --> VAL
    VAL --> BOR[Persiste BorradorOperacion PENDIENTE / REQUIERE_ACLARACION]
    BOR --> HUM[Confirmacion humana obligatoria]
```

## 6. Vinculación Telegram y Sincronización entre Canales

```mermaid
sequenceDiagram
    participant W as Web /integraciones
    participant LS as TelegramAccountLinkService
    participant R as TelegramUpdateRouter
    participant B as BorradorOperacionService
    W->>LS: POST /integraciones/telegram/generar-codigo
    LS-->>W: Codigo temporal NX-XXXXXXXX (guarda SHA-256, expira 10m)
    Note over R: Usuario envia /vincular NX-XXXXXXXX en Telegram
    R->>R: TelegramRateLimiter.allowLinkingAttempt(userId)
    R->>LS: vincularPorCodigo(telegramUserId, codigo)
    LS-->>R: CuentaCanal activa
    Note over R: Usuario envia /gasto ... en Telegram o crea borrador en /asistente
    R->>B: crearBorrador(...) -> BorradorOperacion compartido
    W->>B: POST /asistente/borradores/{id}/confirmar -> Gasto en BD
```
