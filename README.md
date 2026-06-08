# Hitbosss Android (ALPHA)

![Kotlin](https://img.shields.io/badge/SDK-35-F54A2A?logo=android&logoColor=white)
![Android](https://img.shields.io/badge/Android-15+-000000?logo=android&logoColor=white)
![Android Studio](https://img.shields.io/badge/AndroidStudio-Quali1-147EFB?logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-Clean_MVVM-5C6BC0)

App de fuerza donde el usuario compite en un ranking global grabando videos de sus ejercicios directamente desde la app. Soporta **Powerlifting** y **Crossfit**, cada uno con su ranking oficial y sistema de puntuación propio.

---

## ✨ Deportes soportados

| Deporte | Ejercicios | Ranking oficial |
|---|---|---|
| **Powerlifting** | Squat · Bench Press · Deadlift · Sumo Deadlift | Squat + Bench Press + mejor Deadlift |
| **Crossfit** | Snatch · Clean · Clean & Jerk | Snatch + Clean + Clean & Jerk |

---

## 📋 Requisitos

| Herramienta | Versión |
|---|---|
| Android Studio | Quali 1  |
| SDK Objetivo | 35+ |


---

## ⚙️ Setup inicial

```bash
git clone <repo-url>
cd hitbosss-android
cp Hitbosss/Secrets.xcconfig.example Hitbosss/Secrets.xcconfig
cp app/build.gradle.kts.example app/build.gradle.kts
```

Editar `app/build.gradle.kts` con los hosts de cada entorno:

```
API_HOST_DEV = ...
API_HOST_RENDER = ...
API_HOST_TESTFLIGHT = ...
API_HOST_PROD = ...
```

Abrir carpeta/proyecto en Android Studio -> Sincronizar con gradle -> Buildear/Simular

---

## 🏗️ Arquitectura

Clean Architecture + MVVM + Coordinator con cuatro capas bien definidas.

```
┌──────────────────────────────────────────────────────────┐
│                    Composition Root                       │
│              UseCaseFactory (.live / .mock)               │
└──────────────────────────┬───────────────────────────────┘
                           │ inyecta
┌──────────────────────────▼───────────────────────────────┐
│                      Presentation                         │
│        View (SwiftUI)  ◄──►  ViewModel (@MainActor)      │
│                    AppCoordinator                         │
└──────────────────────────┬───────────────────────────────┘
                           │ protocol
┌──────────────────────────▼───────────────────────────────┐
│                        Domain                             │
│     UseCaseType ◄── UseCaseLive ──► RepositoryType       │
│                        Entities                           │
└──────────────────────────┬───────────────────────────────┘
                           │ implementa
┌──────────────────────────▼───────────────────────────────┐
│                          Data                             │
│          RepositoryLive  ·  Mapper  ·  DTOs              │
│          APIDataSource  ·  URLSessionHTTPClient           │
└──────────────┬───────────────────────────┬───────────────┘
               │                           │
     ┌─────────▼────────┐       ┌──────────▼──────────┐
     │    REST API       │       │    Firebase Auth     │
     └──────────────────┘       └─────────────────────┘
```

| Capa | Responsabilidad |
|---|---|
| **Composition Root** | Factories de DI — construyen y cablean el grafo completo |
| **Presentation** | Views (SwiftUI), ViewModels (`@MainActor`), Coordinators |
| **Domain** | Entities, Use Cases, protocolos de Repository — cero dependencias externas |
| **Data** | DTOs, Mappers, Repositories Live/Mock, Networking |

Para convenciones de código, patrones y decisiones: [`Hitbosss/CLAUDE.md`](Hitbosss/CLAUDE.md).

---


## 🌿 Flujo de ramas

```
develop -> Desarrollo/Pruebas
main -> Producción
```
(Aún por definir)

---

## 🛠️ Flujo de trabajo

(Por solidificar y definir)

---

## 📐 Decisiones arquitectónicas

(Por solidificar y definir)
