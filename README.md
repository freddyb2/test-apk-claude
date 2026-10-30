# Météo Bourges

Application Android minimaliste qui affiche la météo de Bourges (Cher) :

- conditions actuelles (température, ressenti, humidité, vent, rafales, précipitations, lever/coucher du soleil) ;
- prévisions heure par heure sur 24 h ;
- prévisions sur 7 jours.

Les données proviennent de l'API gratuite [Open-Meteo](https://open-meteo.com) (aucune clé requise).
Les dernières données reçues sont conservées pour un affichage hors ligne.

## Récupérer l'APK

Chaque push déclenche le workflow GitHub Actions **Build APK**, qui :

- publie l'APK en artefact du workflow (`MeteoBourges-apk`) ;
- crée une pré-release `build-N` contenant `MeteoBourges.apk`.

Sur le téléphone, autorisez l'installation depuis des sources inconnues puis ouvrez le fichier.
Android 8.0 (API 26) minimum.

## Compiler localement

```bash
./gradlew assembleRelease
# -> app/build/outputs/apk/release/app-release.apk
```

Nécessite le SDK Android (variable `ANDROID_HOME` ou `local.properties`).
