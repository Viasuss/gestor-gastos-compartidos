# Reglas de Desarrollo — Partly

---

## 1. Protocolo de intervención (12 pasos)
Toda tarea técnica sigue obligatoriamente:
1. Inspección y diagnóstico.
2. Diseño técnico.
3. Identificación de archivos afectados.
4. Análisis de dependencias y riesgos.
5. Propuesta de implementación.
6. **Autorización humana.**
7. Implementación controlada.
8. Pruebas específicas.
9. Pruebas de regresión.
10. Informe técnico.
11. Revisión cruzada.
12. Integración y cierre.

## 2. Reglas de código y calidad
- Usar `BigDecimal` con escala y redondeo explícitos; prohibido `float` y `double`.
- No duplicar lógica; mantener funciones pequeñas y cohesivas.
- Preservar comentarios existentes no relacionados con el cambio.
- No realizar refactorizaciones fuera del alcance autorizado.
- No declarar una tarea terminada sin evidencia verificable (tests ejecutados y aprobados en entorno seguro).

## 3. Reglas de Git
- Rama estable: `main`. Rama de integración: `develop`.
- Ramas de trabajo: `feature/A-<tema>`, `feature/B-<tema>`, `fix/<tema>`, `release/<version>`.
- Prohibido `git add .` indiscriminado, `push --force`, `reset --hard`, `merge` o borrado de ramas sin autorización humana.
- Nunca hacer commit de credenciales, `.env` ni archivos generados localmente (como `repomix-output.xml`).

## 4. Seguridad en pruebas
- Prohibido ejecutar pruebas contra la base de datos MySQL real/productiva.
- Toda prueba automatizada debe correr en perfil `test` aislado (H2 / Testcontainers) con datos sintéticos.
