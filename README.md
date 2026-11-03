# Nexus IPTV — Android TV (Jetpack Compose + Media3)

App IPTV nativo (landscape, 100% D-pad) com login Xtream Codes, Home, Live TV (3 colunas), Filmes/Séries (2 colunas),
Detalhes/Episódios, Busca, Minha Lista, Configurações e player Media3 ExoPlayer.

## Como gerar o APK
**Opção A – GitHub Actions (sem instalar nada):** suba esta pasta num repositório → aba *Actions* → *Build APK* → baixe o artefato `NexusIPTV-apk`.
**Opção B – Android Studio:** *File > Open* nesta pasta (Hedgehog+ / JDK 17) → *Build > Build APK(s)*.
**Opção C – terminal:** `gradle wrapper --gradle-version 8.7 && ./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.

## Controles
D-pad navega · OK seleciona · Menu/Amarelo = favoritar (canal) ou Minha Lista (pôster) · Live: 1º OK pré-visualiza, 2º OK tela cheia ·
Player: ◀▶ ±10s, ↑↓ troca canal (ao vivo), OK pausa.
