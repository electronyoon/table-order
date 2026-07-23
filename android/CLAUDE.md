# android

> 구현 전 요구사항은 [REQUIREMENTS.md](REQUIREMENTS.md) 참고 — 취소 제스처(탭/길게누르기), 반응형 레이아웃(태블릿/폰) 명세.

Kotlin, Jetpack Compose, 단일 앱. 태블릿(PRIMARY)/폰(BACKUP)은 같은 앱을 설치하고 런타임에 `device.role`만 다르게 설정한다 (별도 빌드 변형 아님).

## 빌드/테스트

- `./gradlew :app:assembleDebug` — Android SDK 필요 (`ANDROID_HOME` 또는 `local.properties`의 `sdk.dir`)
- macOS에서 SDK 없을 때: `brew install --cask android-commandlinetools` → `sdkmanager --licenses` 동의 → `sdkmanager "platform-tools" "platforms;android-37.1" "build-tools;37.0.0"` → `android/local.properties`에 `sdk.dir=/opt/homebrew/share/android-commandlinetools` 작성 (이 파일은 `.gitignore` 처리됨, 커밋 금지)
- AGP 9+ **built-in Kotlin**을 사용한다. `org.jetbrains.kotlin.android` 플러그인은 적용하지 않는다 (적용 시 빌드 실패). Compose 컴파일러는 `org.jetbrains.kotlin.plugin.compose`로 별도 적용한다. 참고: https://kotl.in/gradle/agp-built-in-kotlin
- 버전은 `gradle/libs.versions.toml`(Version Catalog)에서 관리한다.

## google-services.json (FCM)

- 실파일은 커밋하지 않는다 (`.gitignore`에 `android/app/google-services.json` 등록됨). 사람이 Firebase 콘솔에서 받아 `android/app/`에 직접 배치해야 한다 (별도 체크리스트 항목).
- `app/build.gradle.kts`는 이 파일이 있을 때만 `com.google.gms.google-services` 플러그인을 적용하도록 조건 처리되어 있다 — 파일 없이도 로컬/CI 빌드는 깨지지 않는다.

## 코드 컨벤션

- 패키지 루트: `com.electronyoon.tableorder`
- minSdk 26 / compileSdk·targetSdk 37 (androidx 최신 라이브러리가 API 37 컴파일을 요구함)

## 금지사항

- 백엔드 도메인 로직(품절 판정, 상태 전이 등)을 앱에서 재구현하지 않는다 — 항상 서버 API 응답을 신뢰한다.
- API 변경 시 [contracts/openapi.yaml](../contracts/openapi.yaml)을 먼저 수정하고 여기서 따라간다.
