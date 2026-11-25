# Arquitectura Objetivo — Partly (Monolito Modular)

> **Estado:** Propuesta arquitectónica consolidada (F0.2.C), sujeta a evolución incremental por fases (`F1`–`F15`).

---

## 1. Principio rector
Partly evolucionará como un **monolito modular** en Java 21 + Spring Boot + MySQL + Thymeleaf (con API REST `/api/v1` para PWA y canales externos), manteniendo el **núcleo financiero determinista** desacoplado de la IA y de los proveedores externos.

---

## 2. Módulos objetivo

```text
com.gestorgastos.gestor_gastos
├── shared/           # Excepciones, ProblemDetail, política BigDecimal, Idempotencia, Auditoría
├── security/         # SecurityFilterChain, CSRF, Auth, GrupoAccessEvaluator
├── identidad/        # Usuario, registro/login, Google OAuth, vinculación segura de cuentas
├── grupos/           # Grupo, MiembroGrupo, Invitaciones, enlaces/QR, invitados temporales
├── financiero/       # Gasto, PagoGasto, GastoParticipante, DeudaService, Liquidaciones
├── bandeja/          # Bandeja Inteligente (borradores, OCR/voz/texto, confirmación humana)
├── ia/               # Puertos LLM/ASR/OCR y motor de interpretación (solo propuestas)
├── recursos/         # ResourceManager (cuotas, colas, concurrencia, circuit breakers, fallback)
├── automatizaciones/ # Spring Scheduler (gastos recurrentes, vencimientos, recordatorios, cierres)
├── analitica/        # Presupuestos, metas compartidas, anomalías, exportación PDF/Excel
├── conectividad/     # Telegram Bot API, Web Push (VAPID), utilidades share WhatsApp
└── api/v1/           # Controladores @RestController JSON para PWA y clientes
```

---

## 3. Reglas de dependencia entre módulos
1. `financiero/` no puede depender de `ia/`, `bandeja/`, `conectividad/` ni `recursos/`.
2. `ia/` y `conectividad/` nunca escriben directamente en `financiero/`; toda sugerencia pasa por `bandeja/` y requiere validación determinista + confirmación humana.
3. Toda invocación a proveedores externos (Groq Free, Ollama, Whisper, Telegram, Brevo) pasa por `recursos/` (`ResourceManager`) con presupuesto `$0 COP` y degradación automática a operación manual.
4. `api/v1/` y los controladores Thymeleaf consumen los mismos servicios de dominio.
