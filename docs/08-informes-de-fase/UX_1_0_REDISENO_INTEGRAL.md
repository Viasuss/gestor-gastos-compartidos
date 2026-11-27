# Informe de Fase — UX 1.0: Rediseño Integral Desktop-First y Design System (NEXUS)

**Rama de trabajo:** `feature/ux-1-0-dashboard-redesign` (derivada de `v1.0.0` @ `454f787`)  
**Fecha:** 2026-11-25  
**Presupuesto externo empleado:** `$0 COP` (sin librerías de pago ni fuentes externas de red)

---

## 1. Resumen ejecutivo

Se ejecutó el rediseño integral **UX 1.0** de NEXUS transformando las 11 vistas web de una presentación centrada en columna estrecha (`600px`) hacia una arquitectura **SaaS Fintech desktop-first** con sistema de diseño propio, **App Shell unificado** (`sidebar` lateral de `240px` colapsable a `76px`, `topbar` única con migas de pan y espacio de trabajo hasta `1240px`), compatibilidad móvil responsiva (`drawer` + navegación inferior sin ocultación de contenido) y accesibilidad **WCAG 2.2 AA**.

El núcleo financiero, los proveedores de IA/Telegram, las entidades JPA, migraciones Flyway, `SecurityConfig` y `pom.xml` permanecieron **intactos**. Únicamente se incorporó el atributo de presentación `totalGruposActivos` en `UsuarioController.mostrarInicio(...)`.

---

## 2. Hallazgos resueltos respecto a la línea base (`v1.0.0`)

1. **Navegación duplicada en `/inicio`:** eliminada la doble cabecera (`.desktop-nav` + `.nexus-nav-bar`). Todas las vistas autenticadas comparten el fragmento único `appBody` de `src/main/resources/templates/fragments/nexus-layout.html`.
2. **Contenedor estrecho en escritorio:** reemplazada la restricción previa (`600px`) por un espacio de trabajo amplio (`--nx-content-max: 1240px`) con grillas de 2 y 3 columnas en dashboard, grupos, detalle de grupo y flujos financieros.
3. **Botón simulado de Google Contacts (`mostrarRequerimientosGoogle` / `alert`):** eliminado del modal de cuenta; `#panelControlBackdrop` ahora presenta estado real de sesión, método de acceso (`googleVinculado`), accesos directos reales y cierre de sesión con CSRF.
4. **Campo `#fecha` de solo lectura en `agregar-gasto.html`:** eliminado `readonly`; la fecha es editable como `<input type="date">`.
5. **Dependencia externa de tipografía (`fonts.googleapis.com`):** eliminada del `<head>` común en favor de una pila tipográfica nativa offline-safe (`--nx-font-sans`).
6. **Service Worker:** actualizado en `src/main/resources/static/sw.js` a `nexus-static-v3` con lista explícita (`STATIC_ALLOWLIST`) para los recursos públicos del Design System sin cachear rutas HTML ni datos financieros.
7. **Inconsistencias SpEL detectadas en validación real:** corregidos atributos en `grupo.html` (`gasto.monto`, `pago.monto`, `d.otraPersona`, `d.leDebo()`) e `integraciones.html` (`cuentaTelegram.identificadorExterno`), e interpolación de `param.nombre[0]` en toast de `inicio.html`.

---

## 3. Arquitectura del Design System (`UX.1`–`UX.2`)

| Archivo | Responsabilidad |
|---|---|
| `src/main/resources/static/css/nexus-tokens.css` | Tokens oficiales (`#050914`, `#10D99A`, `#35F5B1`, `#087A59`, `#F4F7F6`, `#475569`, `#B93829`, `#B45309`, `#E2E8F0`), tipografía, radios y anillo de foco. |
| `src/main/resources/static/css/nexus-shell.css` | App Shell desktop (`240px` / `76px`), topbar sticky, drawer móvil, barra inferior `<768px` y modal accesible de cuenta. |
| `src/main/resources/static/css/nexus-components.css` | Botones, tarjetas hero/KPI, badges, alertas, tablas financieras (`tabular-nums`), formularios y toasts flotantes. |
| `src/main/resources/static/css/nexus-views.css` | Grillas de 2/3 columnas para dashboard, grupo, flujos financieros y pantallas split-screen de autenticación (`/login`, `/registro`). |
| `src/main/resources/static/css/nexus.css` | Utilidades transversales (se mantiene público `200 OK`). |
| `src/main/resources/static/js/nexus-shell.js` | Colapso persistente del sidebar, drawer móvil, modal de cuenta con `Escape`/`Tab-trap`/`inert` y auto-cierre de toasts. |

---

## 4. Verificación automatizada

Ejecutado sobre copia limpia fuera de OneDrive con perfil aislado `test` (H2 en memoria) mediante `mvnw.cmd -o clean test`:

- **Total de pruebas ejecutadas:** `69`
- **Fallos:** `0`
- **Errores:** `0`
- **Omitidas:** `0`
- **Cobertura añadida:** `UxRedesignVerificationTest.java` (6 pruebas cubriendo recursos públicos y `sw.js` `nexus-static-v3`, vistas públicas split-screen, ausencia de navegación duplicada y botones simulados en las 9 rutas autenticadas, fecha editable, conteo `totalGruposActivos` con 0 y 5 grupos, formato `.00` en `/asistente`, y renderizado completo de `/grupo/{id}` con gasto/deudas e `/integraciones` vinculada).

---

## 5. Validación visual en navegador (`UX.8`)

Validado contra instancia temporal aislada (`http://localhost:8085`, perfil `test` H2) con usuario sintético y datos sintéticos:

- **Resoluciones evaluadas:** `1366x768`, `1920x1080`, `1024x768` y móvil `390x844`.
- **Resultados:** `0` errores de consola, `0` respuestas HTTP `>= 400`, `0` peticiones externas a `fonts.googleapis.com`, sin desbordamiento horizontal (`scrollWidth <= innerWidth`), colapso de sidebar operativo (`240px <-> 76px`), modal accesible operable con `Escape` y renderizado íntegro de `/grupo/1` tras corrección de expresiones SpEL.
- **Evidencia visual ("antes" y "después"):** conservada fuera del árbol Git en `C:\Users\Johan Viasus\.gemini\antigravity\brain\17ce9e6c-7b7a-4055-9a22-1b722c6acafc\ux-screenshots\`.
