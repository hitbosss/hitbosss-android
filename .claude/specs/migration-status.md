# Estado de la migración iOS → Android

> Estado vivo. Actualizar cuando una feature se complete o cambie el estado de deploy.
> Última actualización: 2026-09-01.

## Resumen

Migración iOS→Android **TERMINADA**. El trabajo actual son **fixes + features nuevas** y preparar el release
a la tienda. Versión actual: **3.0.1 (versionCode 7)**. `release` va **ofuscado con R8** (ADR-004).

## Features

| Feature | Estado |
|---|---|
| Ranking (global, filtros, orden peso/Points, detalle) | ✅ (spec: feature-ranking.md) |
| Community (grupos/eventos: listar, detalle, crear/editar, rankings) | ✅ (spec: feature-ranking.md) |
| Hit (grabar + trim/pin + subida multipart idempotente + compartir/export + saved hits) | ✅ (spec: feature-hit-pipeline.md) |
| Métricas (Físico + Fuerza) | ✅ (ya en develop) |
| Auth (Firebase + Google Sign-In + check-user-exists) | ✅ |
| ForceUpdate · Navigation root + MainTab · CompleteProfile · Profile · Settings | ✅ |
| Design system (theme + components) | ✅ |
| i18n (es base + values-en) | ✅ base; migración por fases en curso (ADR-005) |
| Deeplinks | ✅ |

## Ramas

- **`develop`** (activa): contiene todo — la migración, la eliminación de la capa de use-cases (ADR-001
  enmienda), la sección de Métricas (antes en `feature/metrics`) y el untracking de artefactos (antes
  `slim_refactor`). Esas ramas ya se integraron/limpiaron; no existen sueltas.
- **iOS** counterpart: `../Hitbosss_iOS` rama `develop` (verificar que esté actualizada antes de paridad).

## Cambios de ESTA sesión en `develop` (SIN COMMITEAR — no commitear Android hasta que José lo diga)

Fixes de bugs reportados por Rustam + preparación de release:
- **Ranking**: bullet "Tú" desincronizado al ordenar por Points → sale de `displayedEntries` (global) y de
  `entries` (grupo/evento). Ver feature-ranking.md.
- **Compartir hit**: empezaba en el segundo 0 → `ClippingConfiguration` en `performedAt` (paridad iOS).
- **Notificación de subida**: no salía en lock screen → canal `hit_upload_v2` con `lockscreenVisibility=PUBLIC`.
- **Localización**: `CommunityScreen` "Ejercicios:" hardcodeado → recurso `community_exercises`.
- **Build**: `release` con `isMinifyEnabled = true` + `proguard-rules.pro` (R8/ofuscación). Versión → 3.0.1/vc7.

Pendiente (no hecho a propósito): localización por fases de varias pantallas (ADR-005); paridad de renumeración
de ranking con filtros (feature-ranking.md).

## Dependencias externas / contraparte API

- La API 3.0.0 está en PRO (rama `main` de `hitbosss-api`, desplegada). `MINIMUM_VERSION` de PRO sigue en
  2.1.0 a propósito hasta que la 3.x esté descargable en la tienda.
