---
name: add-string
description: Añade un string de usuario localizado (es + en) y lo usa sin hardcodear. Uso: /add-string nombre_recurso
disable-model-invocation: true
---

Añade un string localizado para: $ARGUMENTS

Regla base: **cero texto de usuario hardcodeado**. Todo `Text(...)`, label, placeholder o `contentDescription`
visible va por recurso, con entrada en **ambos** ficheros. Ver ADR-005.

## Pasos
1. **¿Ya existe?** Busca en `app/src/main/res/values-en/strings.xml` un recurso equivalente (p.ej.
   `common_cancel`, `common_accept`, `common_back`, `community_exercises`, `legal_privacy_policy`). Si existe,
   **reúsalo** — no dupliques.
2. **Si no existe, créalo en los dos ficheros con la MISMA clave:**
   - `res/values/strings.xml` → **español** (default).
   - `res/values-en/strings.xml` → **inglés**.
   - Nombre `snake_case` con prefijo de feature (`ranking_`, `community_`, `hit_`, `common_`, …).
   - Con formato: `%1$s` / `%1$d` (idéntico en ambos idiomas).
3. **El inglés se saca de iOS**, no se inventa: busca el string equivalente en `../Hitbosss_iOS` (rama
   actualizada — `git -C ../Hitbosss_iOS fetch`) en `Localizable.xcstrings` o en el `String(localized: "…")`
   del `View` equivalente. Para strings triviales/comunes vale la traducción directa.
4. **Úsalo en el composable:** `stringResource(R.string.<clave>)` (o `getString(...)` fuera de Compose).
   Añade los imports si faltan: `androidx.compose.ui.res.stringResource` y `com.hitbosss.R`.
5. **Verifica** que no queda ningún literal español en la pantalla que tocas:
   `grep -nE 'Text\("[A-ZÁÉÍÓÚ]' <fichero>` y revisa acentos/`¿¡`.

## Aviso
La localización va **por fases** (ADR-005). Si una pantalla está entera sin localizar (0 `stringResource` y
textos en un `enum`), no la localices a medias string a string: pásala completa por el script del equipo
(extracción del `.xcstrings`). Sí puedes arreglar strings sueltos olvidados en pantallas ya localizadas.
