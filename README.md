# Nexus IPTV — Android TV (Jetpack Compose + Media3)

Arquivos Gradle (Groovy): `settings.gradle`, `build.gradle`, `app/build.gradle`, `gradle.properties`.
Requisitos do build: **JDK 17 · Gradle 8.7 · Android Gradle Plugin 8.5.0 · compileSdk 34**.
Comando: `gradle assembleDebug` (ou `assembleRelease`) → `app/build/outputs/apk/<tipo>/app-<tipo>.apk`
O release já sai assinado com a chave debug (instala direto).

Controles: D-pad navega · OK seleciona · Menu/Amarelo = favoritar · Live: 1º OK prévia, 2º OK tela cheia ·
Player: ◀▶ ±10s, ↑↓ troca canal (ao vivo), OK pausa.
