# Phase 8 — Android foundation

## Stack
- Java/XML, MVVM with AndroidX ViewModel + LiveData
- Retrofit + Gson + OkHttp
- Room baseline
- WorkManager baseline
- Explicit `AppContainer` dependency injection (no Kotlin/Hilt code generation)
- Android Keystore encrypted token storage

## Local API
The debug base URL is `http://10.0.2.2:8080/api/v1/` for the Android Emulator, because `localhost` inside an emulator points to the emulator itself.

For a physical device, override `BuildConfig.API_BASE_URL` in the Android build configuration with the development machine's LAN address and ensure the backend is reachable from that device.

## Run
1. Start PostgreSQL and Spring Boot backend.
2. Open `android/` in Android Studio.
3. Sync Gradle.
4. Start an API 28+ emulator.
5. Run the `app` configuration.
6. Login with the backend Android-compatible credentials.
7. Confirm logout, process restart, offline indicator and session-expiry behavior.

## Security
- Tokens are stored encrypted using Android Keystore + AES/GCM.
- OkHttp logging is BASIC only; request/response bodies are never logged.
- Passwords and token values are not written to logs.
- UI does not contain backend domain/business rules.

## Scope boundary
Phase 8 establishes the app foundation and public content API client baseline. Full catalog, reader, search, offline package sync and subscription flows are implemented in later phases.
