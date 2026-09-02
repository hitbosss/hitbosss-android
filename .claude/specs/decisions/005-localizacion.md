# ADR 005: Localización es/en

**Estado:** Aceptada · migración por fases (en curso)

## Contexto
La app se localiza es/en. El idioma es **por app** (`localeConfig` + manifest, verificado). El **inglés es la
fuente de verdad de iOS** (`Localizable.xcstrings`); no se traduce a mano. La migración de strings a recursos
va **por fases** (fase 1: Settings + Calculator); quedan pantallas con español hardcodeado pendientes de fase.

## Decisión / reglas
- `app/src/main/res/values/strings.xml` = **español** (default). `values-en/strings.xml` = **inglés**.
- **Nunca** hardcodear texto de usuario en un composable (`Text("Cancelar")`). Siempre
  `stringResource(R.string.xxx)` con la entrada en **ambos** ficheros.
- El inglés se **extrae del `.xcstrings` de iOS** (rama actualizada) con el script Python del equipo, no a mano
  (ver memoria `ios-localized-content-extraction`). Para strings sueltos ya existentes, reusar el recurso.
- CHANGELOG/PENDING y los ficheros `.claude/` pueden ir en español (son internos).

## Consecuencias / clase de bug a vigilar (IMPORTANTE)
⚠️ Un string hardcodeado en español **se ve en español aunque la app esté en inglés** — es el bug que reportan
como "sale en el idioma que no es". Casos encontrados (2026-09-01, pendientes de la fase de localización, NO
arreglados a mano para no dejar pantallas a medias):
- `CommunityScreen.kt` "Ejercicios:" → **arreglado** (era un string suelto olvidado en pantalla ya localizada;
  usa `community_exercises`).
- `RankingOnboarding.kt` (coach-marks, guardan textos en un `enum` String → requiere refactor a `@StringRes`),
  `HitActionsUi.kt` ("Compartir"/"Denunciar"/"Cancelar"/"Aceptar"/"Exportando vídeo…"/report title),
  `ForceUpdateScreen.kt` ("Actualiza HitBoss"/mensaje/"Actualizar"), `PrivacyPolicyScreen.kt`
  ("Privacidad y seguridad"/"Política de privacidad" + el texto legal completo). → localizar por fase con el script.

**Regla:** al crear/tocar una pantalla, cero literales de usuario; recurso es+en (ver skill `add-string`).
El inglés equivalente para esos ficheros ya existe en iOS (p.ej. onboarding en `OnboardingCardView.swift`,
force-update en `ForceUpdateView.swift` = "A new version of Hitbosss is available. Update to continue using the app.").
