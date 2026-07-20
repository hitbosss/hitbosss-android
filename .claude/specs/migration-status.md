# Estado de la migración iOS → Android

> Estado vivo. Actualizar cuando una feature se complete o cambie el estado de deploy.
> Última actualización: 2026-07-10.

## Resumen

La migración está **TERMINADA** — todas las features de iOS portadas, First Alpha publicada
(commits `1c8fe5c` KickOff → `89545c4` Road to Stable). El trabajo actual ya no es migración:
son **fixes + features nuevas** (idempotencia de subidas, sección Métricas).

## Features

| Feature | Estado |
|---|---|
| Ranking (global, filtros, detalle) | ✅ |
| ForceUpdate | ✅ |
| Auth (Firebase + Google Sign-In + check-user-exists) | ✅ |
| Navigation root + MainTab | ✅ |
| CompleteProfile | ✅ |
| Profile (fotos, info, social) | ✅ |
| Hit (grabación + trim + subida multipart + saved hits) | ✅ |
| Community (grupos/eventos: listar, detalle, crear/editar, rankings) | ✅ |
| Settings | ✅ |
| Design system (theme + components) | ✅ |
| i18n (es base + values-en) | ✅ |
| Deeplinks | ✅ |

## En curso (a 2026-07-10)

**En develop, sin commitear:**
- **Subidas de hit idempotentes**: `clientRequestId` (UUID por intento) en upload + saved hits;
  contraparte API ya mergeada en `hitbosss-api` develop ("idempotent hit uploads #10",
  columna `hit.client_request_id` + UNIQUE parcial, ver ADR-007 del api).
- Mapeo de más errores Firebase en `AuthCommon.toAuthMessage` (network, too-many-requests).
- Community: chip de disciplina en Create/EditGroup (`SelectChip`), ranking de grupo multi-deporte,
  pestaña oficial de evento solo si es oficial.
- `CompleteProfileViewModel`: distinguir los dos 409 del backend (username cogido vs perfil ya existente).

**Rama `feature/metrics` (sin mergear, base = punta de develop):**
- Sección **Métricas** con tab propio: Físico (body logs, objetivos, fotos de progreso, BMI)
  + Fuerza (histórico entrenamientos manuales + hits, `LineChart`, `ProgressStats` con test).
- Contraparte API en rama `api-metrics-upgrade` ("First Metrics Crud #9"): `/api/metrics/:id/*`,
  4 tablas nuevas — migraciones en `../hitbosss-api/PENDING-2.2.0.md` (en esa rama).

**Rama `slim_refactor`:** sin commits sobre develop (parada).

## Dependencias externas pendientes

- Deploy API v2.1.0 a PRO requiere las migraciones SQL de `../hitbosss-api/PENDING-2.1.0.md`.
