plugins {
    // AGP 9+는 Kotlin을 내장 지원한다 — org.jetbrains.kotlin.android 플러그인은 더 이상 필요/허용되지 않는다.
    // https://kotl.in/gradle/agp-built-in-kotlin
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.google.services) apply false
}
