# Guía de Demostración — NEXUS

## 1. Preparación Previa (Entorno Aislado `nexus_demo`)
1. Verificar que MySQL 8 tiene el esquema `nexus_demo` y usuario `nexus_demo_user`.
2. Exportar variables en terminal o IntelliJ (`SPRING_PROFILES_ACTIVE=demo`, `NEXUS_DEMO_DB_PASSWORD`, `NEXUS_DEMO_SEED_ENABLED=true` si se desea carga inicial).
3. Iniciar con `.\mvnw.cmd spring-boot:run`.
4. Abrir `http://localhost:8080/login`.

## 2. Flujo Recomendado
1. Iniciar sesión con `demo.a@nexus.local` / `NexusDemo2026!` (o registrar cuenta nueva sintética).
2. Mostrar `/inicio` con métricas reales (Borradores IA, Liquidaciones, Invitaciones, Canal Telegram).
3. Entrar a **Apartamento NEXUS Demo** y registrar un gasto manual con múltiples pagadores.
4. Abrir **Asistente NEXUS** (`/asistente`), ingresar *"Pagué 45 mil pesos de aseo para los tres"* y confirmar el borrador.
5. Si Telegram y Groq están habilitados, mostrar `/integraciones`, generar código `NX-...`, vincular en Telegram y enviar `/gasto` -> confirmar en web.
6. Mostrar una liquidación bilateral pendiente y confirmarla como receptor.
7. Mostrar la instalación PWA en navegador y `offline.html`.

## 3. Alternativa Sin Internet (Plan B)
Si no hay conexión a Internet durante la sustentación:
- Mantener `GROQ_ENABLED=false` y `TELEGRAM_ENABLED=false`.
- `LocalRulesProvider` interpreta el gasto localmente en `/asistente` sin dependencias externas.
- *Nota de honestidad técnica:* Explicar explícitamente que en modo offline actúa `LocalRulesProvider` y no la API externa de Groq ni Telegram.
