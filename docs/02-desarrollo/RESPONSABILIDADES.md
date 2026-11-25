# Matriz de Responsabilidades — Partly

---

## 1. Roles del equipo

| Rol | Responsabilidades principales |
|---|---|
| **Desarrollador A** | Arquitectura backend, motor financiero (`BigDecimal`, deudas, liquidaciones), persistencia y migraciones MySQL/Flyway, seguridad (`Spring Security`, CSRF, OAuth, autorización), pruebas unitarias/dominio, integración de IA, automatizaciones y `ResourceManager`. |
| **Desarrollador B** | Frontend Thymeleaf, experiencia de usuario (UX), accesibilidad (WCAG), diseño responsive, JS modular, PWA (`manifest`, Service Worker, Web Push), procesamiento de archivos en cliente (UI/OCR), flujos de Telegram/notificaciones, visualización de datos y pruebas UI/`@WebMvcTest`. |

---

## 2. Propiedad temporal de archivos (Fase F1)

> [!IMPORTANT]
> **Reglas de alcance y coordinación:**
> - Las autorizaciones generales por desarrollador **no sustituyen los límites específicos de cada subfase** (cada prompt de subfase define los archivos exactos permitidos).
> - Los cambios de seguridad que requieran adaptación simultánea del frontend (por ejemplo, CSRF entre `F1.2.A` y `F1.2.B`) deberán integrarse coordinadamente.

| Ruta / Archivo | Responsable exclusivo en F1 | Regla de coordinación |
|---|---|---|
| `src/main/java/.../entity/*`, `repository/*`, `service/*`, `config/*`, `security/*` | **Desarrollador A** | B no modifica lógica ni entidades Java. |
| `src/main/java/.../controller/*` | **Desarrollador A** | Cambios en atributos de modelo SSR se acuerdan con B en `CONTRATOS_API.md`. |
| `src/main/resources/db/migration/*` (futuro Flyway) | **Desarrollador A** | Único responsable de esquema BD. |
| `pom.xml` | **Desarrollador A** | Cualquier dependencia solicitada por B se aprueba y agrega por A. |
| `src/main/resources/application*.properties` | **Desarrollador A** | B no edita configuraciones de servidor/BD. |
| `src/main/resources/templates/**` | **Desarrollador B** | Inclusión de `_csrf` se coordina tras F1.2.A. |
| `src/main/resources/static/**` | **Desarrollador B** | CSS, JS modular, SVG/iconos. |
| `src/test/java/...` (dominio/seguridad/repositorios) | **Desarrollador A** | — |
| `src/test/java/...` (WebMvc/vistas/UI) | **Desarrollador B** | Depende del entorno aislado creado por A en F1.1.A. |
| `docs/01-arquitectura/CONTRATOS_API.md` | **Compartido (A + B)** | Ningún cambio sin acuerdo previo de ambos roles. |
