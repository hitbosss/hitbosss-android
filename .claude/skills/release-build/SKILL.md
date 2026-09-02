---
name: release-build
description: Compila y verifica el APK/AAB de release (R8, versión, firma). Uso: /release-build
disable-model-invocation: true
---

Genera y verifica un build de **release** (producción, ofuscado). Ver ADR-004.

## Gotcha: no hay `gradlew` en el repo
Se usa Android Studio, así que el wrapper no está commiteado. Para compilar por CLI, usa la distribución
Gradle cacheada + el JBR de Android Studio (ajusta la ruta de la dist si cambia la versión):
```bash
A=/Users/joselopez/Documents/Fitboss/repos/hitbosss-android
G=$(ls -d ~/.gradle/wrapper/dists/gradle-*/*/gradle-*/bin/gradle | tail -1)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
ANDROID_HOME="$HOME/Library/Android/sdk" \
"$G" -p "$A" :app:assembleRelease --console=plain
```
(No pipes a `tail` en background: enmascaran el exit code de gradle. Redirige a un log y greppéalo.)

## 1. Versión (si vas a subir a la tienda)
En `app/build.gradle.kts` (gitignored) `defaultConfig`:
- `versionCode` **estrictamente mayor** que el último subido a Play (si no, Play rechaza).
- `versionName` = marketing (= iOS).

## 2. Compilar
`:app:assembleRelease` (APK para probar en el móvil) o `:app:bundleRelease` (AAB para subir a Play).
Debe terminar en `BUILD SUCCESSFUL`.

## 3. Verificar (robustez — ADR-004)
- **Ofuscación activa**: existe `app/build/outputs/mapping/release/mapping.txt` (grande). Guárdalo por versión.
- **Firma de producción**: `$SDK/build-tools/*/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk`
  → `Verifies` + `Signer #1 certificate DN: CN=HitBosss, O=Hitbosss innovations SL` (NO "Android Debug").
  Requiere `keystore.properties` en la raíz (fuera de git).
- **Versión en el APK**: `$SDK/build-tools/*/aapt dump badging <apk> | grep version`.
- **Runtime bajo R8** (si tocaste DTOs/serialización): instala en emulador y comprueba en logcat que no hay
  `SerializationException`/`NoClassDefFound`/`VerifyError` al arrancar/loguear. Si un `com.hitbosss` con otra
  firma ya está instalado, desinstálalo antes (`adb uninstall com.hitbosss`).

## 4. Recordatorios
- El APK release firma con la keystore de prod → para actualizar instalaciones existentes debe ser la **misma
  clave** que la registrada en Play (App Signing). Firmar con otra → rechazo por firma distinta.
- **NUNCA commitees en Android** sin que José lo pida (ver memoria `android-never-commit`).
- No subir `MINIMUM_VERSION` de PRO en la API hasta que la 3.x esté descargable en la tienda.
