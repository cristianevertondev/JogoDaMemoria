# JogoDaMemoria

A memory card game for Android, developed with **Kotlin** and **Jetpack Compose**.

JogoDaMemoria was created as a mobile game project focused on interactive gameplay, state management, responsive UI, and multilingual support.

## Features

* Classic memory card gameplay
* Card matching mechanics
* Interactive game board
* Game state management
* Score and progress tracking
* Multiple game interactions
* Portuguese, English, and Spanish support
* Responsive Jetpack Compose interface
* Custom application theme
* Unit tests
* Android instrumentation tests

## Technologies

* **Kotlin**
* **Jetpack Compose**
* **Android SDK**
* **Android Jetpack**
* **Gradle Kotlin DSL**
* **JUnit**
* **Android Instrumentation Tests**

## Game Architecture

The application is structured around Android's modern UI and state-management approach.

The main game logic and interface are handled through Kotlin and Jetpack Compose, allowing the game board and card states to react dynamically to player interactions.

The project also separates UI theme resources and application resources using Android's standard project structure.

## Localization

JogoDaMemoria supports three languages:

* 🇧🇷 Portuguese
* 🇺🇸 English
* 🇪🇸 Spanish

The application uses Android's localized resource system to provide the appropriate interface language.

## Testing

The project includes:

* Unit tests
* Android instrumentation tests

These tests help validate application behavior and maintain reliability during development.

## Project Structure

```text id="h6p3w2"
app/
└── src/
    ├── androidTest/
    │   └── java/
    │
    ├── main/
    │   ├── java/
    │   │   └── com/cristian/jogodamemoria/
    │   │       ├── MainActivity.kt
    │   │       └── ui/
    │   │           └── theme/
    │   │
    │   └── res/
    │       ├── drawable/
    │       ├── mipmap/
    │       ├── values/
    │       └── xml/
    │
    └── test/
        └── java/
```

## Getting Started

### Requirements

* Android Studio
* JDK
* Android SDK
* Android device or emulator

### Build

Clone the repository:

```bash id="k3m8v1"
git clone https://github.com/cristianevertondev/JogoDaMemoria.git
```

Open the project in Android Studio, allow Gradle to synchronize, and run the application on an Android device or emulator.

## Project Goals

JogoDaMemoria is part of the **ELDREON STUDIOS** Android development portfolio.

The project focuses on:

* Kotlin-first Android development
* Jetpack Compose
* Interactive mobile game development
* UI state management
* Multilingual applications
* Testable Android projects
* Modern Android development practices

## Developer

**ELDREON STUDIOS**

Android development focused on **Kotlin, Jetpack Compose, mobile applications, and games**.

GitHub:
https://github.com/cristianevertondev

LinkedIn:
www.linkedin.com/in/cristian-everton-30388b438

---

## License

This project is part of the ELDREON STUDIOS development portfolio.
