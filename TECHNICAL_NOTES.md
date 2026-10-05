# Technical notes — v1.6.5

Base: Partitures Festeres v1.6.4.

## UI consistency audit
- Nou `FestiveComponents.kt` amb `FestivePanel`, `FestiveButton`, `FestiveChoice` i colors comuns per a `OutlinedTextField`.
- Els `Card` decoratius principals de Biblioteca, Repertoris, Ajustos, Importació i Eines passen a superfícies planes de pergamí amb una sola vora.
- Els únics `Card` blancs que es mantenen són les previsualitzacions de pàgina/PDF, perquè representen deliberadament el paper blanc de la partitura.
- Els controls clars interactius s'han migrat progressivament a components propis per evitar l'efecte de doble rectangle observat en alguns dispositius.
- El visor conserva deliberadament les barres blanques translúcides perquè formen part del disseny net de lectura, no de les targetes generals de l'aplicació.

## Reference tone
- Notes distribuïdes en 4 columnes en lloc de 6.
- `maxLines = 1` i més amplària útil per als noms enharmònics llargs.
- Botons de nota, octava i accessos ràpids reutilitzen els components visuals comuns.

## Metronome
- El sistema anterior usava `SoundPool` + `delay()` en una coroutine. El planificador d'Android podia introduir jitter i, en modificar el BPM, la coroutine es reiniciava immediatament.
- Nou `MetronomeEngine.kt` basat en `AudioTrack.MODE_STATIC`.
- Cada compàs es genera com un buffer PCM complet i Android el repeteix amb `setLoopPoints`, de manera que l'espai entre polsos queda determinat per mostres d'àudio i no pel planificador de la UI.
- El slider de tempo no reinicia el motor mentre s'arrossega; el nou BPM s'aplica en acabar el gest. Els botons +/- s'apliquen de forma immediata.
- Compassos disponibles: 2/4, 3/4, 4/4, 2/2, 3/8, 6/8, 9/8 i 12/8.
- Els compassos compostos utilitzen tres subdivisions per pols principal.
- Síntesi de click mecànic seca, amb ressonàncies amortides, transitori curt i lleugera alternança tick/tock.

## Validation
- XML de recursos validat.
- Valencià i castellà contenen les mateixes 275 claus de text.
- Revisió de sintaxi Kotlin amb `kotlinc`: no s'han detectat errors de parser. L'entorn no disposa de l'SDK Android complet, de manera que la compilació final s'ha de validar en Android Studio.

## Version
- versionCode 27
- versionName 1.6.5
