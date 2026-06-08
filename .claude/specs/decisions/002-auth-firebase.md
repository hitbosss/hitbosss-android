# ADR 002: Autenticación con Firebase (doble capa)

**Estado:** Aceptada

## Contexto
La DB del backend es Supabase, pero la autenticación es **Firebase** (igual que iOS y verificada por la API
con el Admin SDK del proyecto `powerranking-40b11`). Firebase y el registro en la DB son **capas distintas**.

## Decisión
- Login con **Firebase Auth** en Android (Google + email/password; Apple no aplica).
- **Google Sign-In** vía **Credentials Manager** + `GoogleIdTokenCredential` (API moderna), no la antigua
  `GoogleSignInClient`.
- Tras el login Firebase, llamar a `GET /login` (con el ID token) para saber si el usuario existe en la DB.
- El `AuthInterceptor` añade `Authorization: Bearer <idToken>` automáticamente.

## Flujo crítico (no romper)
1. Login Firebase → hay `FirebaseUser`.
2. `GET /login` → `isUserInApi` + `measurementSystem`.
3. Si **no** existe → completar perfil (username, altura, peso, género…).
4. Si la **API falla** → NO asumir que no está registrado. Mostrar error y permitir reintento; nunca dejar
   al usuario atrapado en el bucle de "completar perfil".

## Requisitos
- `google-services.json` en `app/` (registrar app Android `com.hitbosss[.debug]` en Firebase + SHA-1).
- Plugin `google-services` activado.

## Consecuencias
✅ Mismo proveedor de identidad que iOS → tokens válidos contra la misma API.
⚠️ Sin `google-services.json` el login no funciona, pero los endpoints públicos (ranking, config) sí.
