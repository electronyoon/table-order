# backend

Java 21, Spring Boot 4.0, Gradle(Kotlin DSL), PostgreSQL, Flyway, Testcontainers.

## 빌드/테스트

- 빌드: `./gradlew build`
- 테스트: `./gradlew test` — **Testcontainers**로 Postgres를 자동 기동한다. 로컬 `docker-compose`는 필요 없음. Docker Desktop만 실행 중이면 된다.
- 로컬 서버 실행: 루트 `docker-compose up -d`로 Postgres를 띄운 뒤 `./gradlew bootRun` (`application.yml`이 `localhost:5432/table_order`를 바라봄)
- Homebrew로 설치한 `openjdk@21`은 keg-only라 `JAVA_HOME`을 명시해야 한다:
  ```
  export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
  ```

## 코드 컨벤션

- 마이그레이션은 `src/main/resources/db/migration`에 Flyway 네이밍(`V{n}__description.sql`)으로 추가한다. 기존 마이그레이션은 수정하지 않고 새 버전을 추가한다.
- JPA `ddl-auto`는 `validate`로 고정 — 스키마 변경은 항상 Flyway 마이그레이션을 통해서만 한다.

## 금지사항

- Android 앱에서 처리해야 할 화면/알림 로직을 백엔드에 중복 구현하지 않는다.
- API 변경 시 [contracts/openapi.yaml](../contracts/openapi.yaml)을 먼저 수정하고 여기서 따라간다 (역방향 금지).
