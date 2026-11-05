# HSC Academic Solution — Phase 1
Monorepo: Spring Boot API, Java/XML Android, React/TypeScript Admin.

## Pinned baseline
Backend: Java 21, Spring Boot 3.5.16, Maven 3.9.x.
Android: JDK 17, AGP 8.13.2, Gradle 8.13, compileSdk 36, minSdk 28 (Android 9).
Admin: Node 22.12+, npm 10+, React 19.2, Vite 8.3.5, TypeScript 5.9.
Local services: PostgreSQL 17.6; optional MinIO.

## Run backend
Copy `.env.example` to `.env`, then:
`docker compose -f infra/docker-compose.yml --env-file .env up -d postgres`
`cd backend && mvn clean verify`
`mvn spring-boot:run`
Health: http://localhost:8080/api/v1/health
Actuator: http://localhost:8080/actuator/health
OpenAPI: http://localhost:8080/api-docs
Swagger: http://localhost:8080/swagger-ui.html

## Run admin
`cd admin && npm install`
`npm run lint && npm run typecheck && npm run test && npm run build`
`npm run dev` — http://localhost:5173

## Android
Open `android/` in Android Studio, install SDK Platform 36, sync and run.
CLI: `gradle testDebugUnitTest`, `gradle lintDebug`, `gradle assembleDebug`.
A local Gradle installation can generate wrapper scripts with `gradle wrapper --gradle-version 8.13`.

## Optional MinIO
`docker compose -f infra/docker-compose.yml --env-file .env --profile object-storage up -d`
Console: http://localhost:9001. Credentials are local placeholders only.

## Decisions and boundaries
Android 9/API 28 minimum. No SMS OTP in initial plan; evaluate Google Sign-In (Credential Manager) plus email/password in the authentication feature phase. Email verification and password recovery may require a paid email-delivery service. Payment provider is not selected; compare aamarPay and ShurjoPay using current merchant quotations and terms before integration. Backend must verify payment callbacks; never trust client success flags.
Bangla/English and light/dark foundations are represented in the UI shells. No real authentication, business features, payment integration or production deployment are implemented in Phase 1.
Never commit `.env`, secrets, OAuth credentials, signing keys or payment credentials.


## Phase 1 validation and GitHub CI checklist
1. Push the project to a GitHub repository's `main` or `master` branch.
2. Open **Actions** and enable workflows if prompted.
3. Select **phase1-ci** and click **Run workflow**, or push a commit / open a pull request.
4. Confirm `backend`, `admin`, and `android` jobs are green for the same commit.
5. On failure, open the failed job, expand the first red step, fix the first actionable error, commit, and push again.

### Dependency reproducibility
- This starter does not include Gradle wrapper scripts/JAR or `admin/package-lock.json`. CI currently installs Gradle 8.13 and uses `npm install`, so dependency resolution is not fully reproducible yet.
- With Gradle 8.13 installed, run `gradle wrapper --gradle-version 8.13` inside `android/`; commit `gradlew`, `gradlew.bat`, and `gradle/wrapper/gradle-wrapper.jar`. Then use `./gradlew` in CI.
- On a network-enabled machine, run `npm install` inside `admin/` and commit `package-lock.json`; then change CI to `npm ci`.
- Phase 1 should be considered fully closed only after those files are committed and all three CI jobs pass.

### Local run notes
- Compose `--env-file .env` supplies variables to Compose interpolation; it does not automatically export them to a separately launched Spring Boot process.
- Backend (Maven installed): `cd backend && mvn spring-boot:run`
- Admin: `cd admin && npm install && npm run dev`
- Android (Android SDK and Gradle installed): `cd android && gradle assembleDebug`
