# HB-010 — Inventario de Duplicaciones Frontend y Propuesta de Modularización

**Proyecto:** Partly (`gestor-gastos`)  
**Fase:** F1.1.B (Solo inventario — sin refactorización estructural)  
**Responsable:** Desarrollador B  
**Rama:** `feature/B-f1-1-saneamiento-frontend`  
**Estado:** `COMPLETADO (Solo inventario; refactorización diferida a F1.3.B)`

---

## 1. Alcance y restricción

Este documento inventaría las duplicaciones reales detectadas en `src/main/resources/templates/*.html` y `src/main/resources/static/` tras las intervenciones puntuales de `F1.1.B`.  
**No se ha ejecutado refactorización estructural** ni se han eliminado recursos gráficos o logos durante esta fase.

---

## 2. Inventario detallado de duplicaciones

### 2.1 Bloque `<head>` repetido (8/8 plantillas)

Las 8 plantillas Thymeleaf duplican de forma literal la cabecera base (`charset`, `viewport`, favicon `logo-icon.svg`, preconexión y hoja `Inter` de Google Fonts, y `partly.css`):

| Plantilla | Líneas aproximadas | Diferencias específicas |
|---|---|---|
| `login.html` | 3–15 | Título + `<script src="https://accounts.google.com/gsi/client" async defer>` |
| `registro.html` | 3–15 | Título + `<script src="https://accounts.google.com/gsi/client" async defer>` |
| `inicio.html` | 3–12 | Solo título |
| `mis-grupos.html` | 3–12 | Solo título |
| `crear-grupo.html` | 3–12 | Solo título |
| `invitaciones.html` | 3–12 | Solo título |
| `grupo.html` | 3–12 | Título dinámico `th:text="${grupo.name} + ' · Partly'"` |
| `agregar-gasto.html` | 3–12 | Solo título |

---

### 2.2 Google Identity Services (Auth y SVG)

1. **Carga de SDK externa:** `<script src="https://accounts.google.com/gsi/client" async defer></script>` duplicado en `login.html` (línea 14) y `registro.html` (línea 14).
2. **Formulario oculto de envío:** `<form id="googleSubmitForm" th:action="@{/auth/google}" method="post">` con `<input type="hidden" name="credential" id="googleCredentialInput">` duplicado en `login.html` (líneas 100–102) y `registro.html` (líneas 125–127).
3. **Lógica JavaScript inline:** `handleGoogleCredentialResponse(response)` e `iniciarGoogleAuth()` duplicadas entre `login.html` (líneas 104–129, más bloque One Tap en 132–140) y `registro.html` (líneas 129–154). Solo difieren en el texto del `alert()` de configuración.
4. **SVG multicolor del icono Google (`viewBox="0 0 24 24"`, 4 `<path>`):** repetido **4 veces** en HTML:
   - `login.html` (líneas 82–87)
   - `registro.html` (líneas 107–112)
   - `inicio.html` (líneas 104–109 y 119–124)

---

### 2.3 Sistema de Toasts (HTML y JS)

1. **Marcado `.toast-container` / `.toast-card`:** implementado de forma separada en `crear-grupo.html` (líneas 15–51) e `inicio.html` (líneas 15–29).
2. **Lógica JS duplicada:**
   - `crear-grupo.html`: `cerrarToast(id)` y auto-cierre de `.toast-card` tras `4200 ms`, más `mostrarToastDinamico(...)` (ahora sanitizado sin `innerHTML`).
   - `inicio.html`: `cerrarToastInicio(id)` y auto-cierre de `#toastGrupoCreado` tras `4200 ms` con la misma animación `.toast-hiding` y `setTimeout(..., 350)`.

---

### 2.4 JavaScript inline (5/8 plantillas, 0 módulos externos)

Actualmente `src/main/resources/static/` no contiene carpeta `js/`. Todo el JavaScript vive embebido en `<script>`:

| Plantilla | Responsabilidad del `<script>` inline |
|---|---|
| `login.html` | Google Sign-In + One Tap (`th:inline="javascript"`). |
| `registro.html` | Google Sign-In (`th:inline="javascript"`) + validación en vivo `validarContraseñas()`. |
| `inicio.html` | Cierre/auto-cierre de toast + gestión accesible del modal `#panelControlBackdrop` + `mostrarRequerimientosGoogle()`. |
| `crear-grupo.html` | Cierre/auto-cierre de toasts + `mostrarToastDinamico()` seguro + validación submit de `#formCrearGrupo`. |
| `agregar-gasto.html` | Toggle pagadores/participantes + sincronización `aria-checked` + formato moneda COP + validación suma en vivo. |

---

### 2.5 Estilos inline (`style="..."`)

Conteo verificado en las 8 plantillas (**66 ocurrencias totales**):

| Plantilla | Ocurrencias `style="..."` | Patrones principales repetidos |
|---|---|---|
| `agregar-gasto.html` | **25** | `margin-bottom: 0` en `.form-group`, `padding-top: 4px` en `.section-block`, `font-size: 13px; font-weight: 500`, `height: 52px; background-color: #F5F0E5`, stacks flex (`display: flex; flex-direction: column; gap: ...`), `display: none` en checkboxes ocultos, color inline en `#mensajeSumaPagos` vía JS. |
| `mis-grupos.html` | **10** | `height: 50px; margin-top: 0` en `.btn-submit`, `padding: 16px` en `.group-card`, `color: #2D8B71` (sobrescritura local de contraste inferior a 4.5:1), subtítulos `font-size: 11px; color: var(--text-muted)`. |
| `inicio.html` | **8** | Botón invitaciones (tamaño/flex/fondo), punto rojo de notificación inline, `margin: 0` en `<form>`, estilos del botón cerrar sesión, `color: var(--positive-text)` en estado de cuenta, `color: var(--text-muted)` en balance `$0`. |
| `invitaciones.html` | **6** | `margin: 0` en `<form>` de aceptar/rechazar (x2), punto rojo inline, stack flex de tarjetas (`gap: 14px`), `font-weight: 500`. |
| `crear-grupo.html` | **5** | `margin: 0` en `<form>` quitar persona (x2), alturas explícitas en inputs/botón (`height: 50px/52px/54px`). |
| `grupo.html` | **5** | `height: 50px; margin-top: 0` en `.btn-submit`, `font-size: 16px` en título sección, layout fila de integrantes. |
| `registro.html` | **4** | `height: 50px; font-size: 14px` en botón Google, `width/height: 20px` en SVG Google, `#passwordError`, `display: none` en `#googleSubmitForm`. |
| `login.html` | **3** | `height: 50px; font-size: 14px` en botón Google, `width/height: 20px` en SVG Google, `display: none` en `#googleSubmitForm`. |

---

### 2.6 Recursos SVG e iconografía duplicada

1. **Archivo gráfico duplicado en disco:**
   - `src/main/resources/static/img/logo-icon.svg` (referenciado por las 8 plantillas).
   - `src/main/resources/static/css/img/logo-icon.svg` (copia no referenciada; **conservada intacta** en F1.1.B).
2. **SVG inline repetidos en HTML:**
   - Icono Google (`4` apariciones).
   - Icono check `12x12` en `.custom-checkbox` (`2` apariciones en `agregar-gasto.html`).

---

### 2.7 Componentes visuales reutilizables detectados

1. **Cabecera de subpágina (`.subpage-header`):** repetida en `mis-grupos.html`, `crear-grupo.html`, `invitaciones.html`, `grupo.html` y `agregar-gasto.html`.
2. **Tarjeta de grupo (`.group-card`):** estructura casi idéntica entre `inicio.html` y `mis-grupos.html` (icono, nombre, subtítulo de integrantes/miembros y columna derecha de balance positivo/negativo/cero).
3. **Estado vacío (`.empty-state`):** repetido en `inicio.html`, `mis-grupos.html`, `crear-grupo.html`, `invitaciones.html` y `grupo.html`.
4. **Alertas de error/éxito (`.alert-message`):** repetido en `login.html`, `registro.html` y `agregar-gasto.html`.
5. **Formularios sin margen (`<form ... style="margin: 0;">`):** repetido en botones de acción dentro de tarjetas (`crear-grupo.html`, `invitaciones.html`, `inicio.html`).

---

## 3. Propuesta futura de fragmentos Thymeleaf y módulos JS (Para F1.3.B)

> **Estado:** `PROPUESTA DIFERIDA A F1.3.B`. No implementada en `F1.1.B`.

### 3.1 Fragmentos Thymeleaf propuestos (`src/main/resources/templates/fragments/`)

| Archivo propuesto | Fragmento(s) | Uso |
|---|---|---|
| `fragments/head.html` | `commonHead(title, includeGoogle)` | Unificar metadatos, favicon, Google Fonts y `partly.css` (más futuro `<meta name="_csrf">` en `F1.2.B`). |
| `fragments/header.html` | `subpageHeader(title, backUrl, showDot)` | Reemplazar las 5 cabeceras `.subpage-header` repetidas. |
| `fragments/toasts.html` | `toastContainer(errorMsg, successMsg)` | Centralizar el contenedor `#toastContainer` y tarjetas SSR/dinámicas. |
| `fragments/icons.html` | `googleIcon()`, `checkIcon()`, `mailIcon()` | Evitar duplicación de SVGs inline en plantillas. |
| `fragments/group-card.html` | `groupCard(grupo, subtitulo, balance)` | Unificar tarjetas de `inicio.html` y `mis-grupos.html` corrigiendo además el inline `#2D8B71` de `mis-grupos.html` hacia `--positive-text`. |

### 3.2 Módulos JavaScript propuestos (`src/main/resources/static/js/`)

| Archivo propuesto | Origen actual | Responsabilidad |
|---|---|---|
| `static/js/toasts.js` | `crear-grupo.html` + `inicio.html` | `cerrarToast`, auto-cierre y `mostrarToastDinamico` seguro (sin `innerHTML`). |
| `static/js/google-auth.js` | `login.html` + `registro.html` | Inicialización GSI, `handleGoogleCredentialResponse`, fallback UI (sin `alert()`). |
| `static/js/panel-modal.js` | `inicio.html` | Apertura/cierre accesible del modal de perfil (`inert`, focus trap, `Escape`, restauración de foco). |
| `static/js/gasto-form.js` | `agregar-gasto.html` | Toggles ARIA pagadores/participantes, formato COP y validación de sumas. |
| `static/js/crear-grupo.js` | `crear-grupo.html` | Validación previa al submit de creación de grupo y feedback con `toasts.js`. |
| `static/js/registro-form.js` | `registro.html` | Validación en vivo de confirmación de contraseña. |

### 3.3 Limpieza CSS propuesta para `F1.3.B`

- Migrar los 66 `style="..."` inline a clases semánticas o utilitarias en `partly.css` (`.form-inline`, `.btn-submit-compact`, `.stack-sm`, `.stack-md`, `.text-muted-xs`, `.group-card-balance-col`, `.nav-icon-btn`, `.notification-dot--sm`).
- Unificar `.group-card-amount.pos` en `mis-grupos.html` (línea 70, actualmente con `#2D8B71` inline) hacia `var(--positive-text)` (`#1E6B55`).
- Evaluar en `F1.3.B` / `F1.5.B` la remoción coordinada de `static/css/img/logo-icon.svg` una vez validado que no tiene referencias externas.
