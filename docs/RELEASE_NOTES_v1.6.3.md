# Partitures Festeres v1.6.3

Projecte Android natiu (Kotlin + Jetpack Compose) per gestionar i mostrar partitures PDF en tauleta.

## Novetats v1.6.3

- Nou **icona de llançador** amb saxòfon i partitura, basat en el disseny seleccionat per a la publicació del projecte.
- L'icona s'ha convertit a **Adaptive Icon** d'Android: el sistema pot aplicar correctament la màscara circular, quadrada arrodonida o la forma pròpia del launcher sense afegir el marc blanc de la versió anterior.
- S'inclou també `roundIcon` específic per als launchers que l'utilitzen.
- El **widget no s'ha modificat**: conserva exactament el seu disseny i el recurs `widget_round_icon.png` de v1.6.2.
- No hi ha canvis funcionals respecte de v1.6.2.

## Internacionalització

- Interfície bilingüe **Valencià / Castellano**.
- El valencià continua sent l'idioma predeterminat.
- Selector a **Ajustos → Idioma de l'aplicació** i persistència de l'idioma seleccionat.

## Versió

- `versionCode = 25`
- `versionName = 1.6.3`
- `minSdk = 26`
- `compileSdk / targetSdk = 36`

Per generar l'APK en Android Studio:

**Build → Generate App Bundles or APKs → Generate APKs**
