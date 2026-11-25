# PWA NEXUS — Instalación y Seguridad de Caché

**Bloque:** B5 — Identidad Visual, Experiencia de Usuario y PWA  
**Rama:** `feature/acelerado-b5-experiencia-pwa`  
**Fecha:** 2026-11-25

---

## 1. Manifiesto Web (`manifest.webmanifest`)

- **Archivo:** `src/main/resources/static/manifest.webmanifest`
- **Configuración principal:**
  - `name`: `"NEXUS — Asistente inteligente de finanzas compartidas"`
  - `short_name`: `"NEXUS"`
  - `start_url`: `"/inicio"`
  - `scope`: `"/"`
  - `display`: `"standalone"`
  - `theme_color`: `"#050914"`
  - `background_color`: `"#F4F7F6"`
  - `icons`: `/img/icon-192.png` (`192x192`, `image/png`) y `/img/icon-512.png` (`512x512`, `image/png`)

## 2. Política Estricta del Service Worker (`sw.js`)

El Service Worker (`src/main/resources/static/sw.js`) aplica una política de seguridad conservadora (**allow-list estática**):

1. **Recursos cacheados (`STATIC_ALLOWLIST`):**
   - `/css/partly.css`
   - `/css/nexus.css`
   - `/img/nexus-isotipo.svg`
   - `/img/nexus-logo-horizontal.svg`
   - `/img/icon-192.png`
   - `/img/icon-512.png`
   - `/js/pwa-register.js`
   - `/offline.html`
   - `/manifest.webmanifest`
2. **Exclusión total de datos sensibles y mutaciones:**
   - Nunca intercepta ni cachea peticiones distintas de `GET` (`POST`, `PUT`, `DELETE`).
   - Nunca cachea vistas HTML autenticadas (`/inicio`, `/mis-grupos`, `/grupo/**`, `/asistente`, `/integraciones`, `/invitaciones`) ni rutas de API/webhook (`/api/**`).
   - Ante fallo de red en navegación (`request.mode === 'navigate'`), responde exclusivamente con el fallback estático `/offline.html`.
3. **Limpieza automática:**
   - En `activate`, elimina versiones previas de caché distintas a `nexus-static-v1`.

## 3. Reglas en `SecurityConfig`

`SecurityConfig.java` permite acceso público (`permitAll()`) únicamente a los recursos públicos PWA necesarios antes y durante la instalación:
- `/manifest.webmanifest`
- `/sw.js`
- `/offline.html`
- `/css/**`, `/js/**`, `/img/**`

Todas las rutas funcionales, financieras y de integración continúan protegidas por autenticación y CSRF.

## 4. Cómo instalar NEXUS como PWA

- **Desktop (Chrome / Edge):** Abrir la aplicación, hacer clic en el ícono de **Instalar NEXUS** en la barra de direcciones y confirmar.
- **Android (Chrome):** Abrir el menú (`⋮`) → **Añadir a pantalla de inicio** / **Instalar aplicación**.
- **iOS (Safari):** Tocar **Compartir** → **Añadir a pantalla de inicio**.
