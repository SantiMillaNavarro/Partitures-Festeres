# Technical notes — v1.7.5

Base funcional: Partitures Festeres v1.7.0. La lògica experimental d'orientació i la pantalla de càrrega afegides en v1.7.1 no s'han reutilitzat.

## Orientation architecture
El canvi principal és separar la política d'orientació de la interfície principal i la del visor.

### MainActivity
- Conserva el comportament de v1.7.0.
- Tablets (`smallestScreenWidthDp >= 600`) prefereixen `SENSOR_LANDSCAPE`.
- Telèfons utilitzen `FULL_SENSOR`.
- No canvia d'orientació quan s'obri o es tanca una partitura.

### PdfViewerActivity
- Nova Activity dedicada al visor.
- Declarada en el Manifest amb `screenOrientation="fullSensor"` i `configChanges="orientation|screenSize|keyboardHidden"`.
- Si `allowViewerRotation=false`, sol·licita `SENSOR_LANDSCAPE` una única vegada en `onCreate`.
- Rep la seqüència de partitures, índex inicial, mode d'inici i opció de bucle mitjançant extras de l'Intent.
- Manté navegació entre partitures, favorits, recents, estat del visor, anotacions, reparació PDF i eines ràpides.

### PdfViewerScreen / ViewerEnvironment
- Ja no importa ni utilitza `ActivityInfo`.
- No modifica `requestedOrientation` i no intenta restaurar-la en `onDispose`.
- Continua gestionant pantalla activa, brillantor i barres del sistema dins de la finestra del visor.

Aquesta separació evita el cicle anterior:
`SENSOR_LANDSCAPE -> FULL_SENSOR -> SENSOR_LANDSCAPE`
dins de la mateixa Activity, que provocava animacions i intents repetits de rotació en algunes tablets OnePlus i Nokia.

## Shared state on return
`MainActivity` incrementa un `refreshToken` en `onResume`. `PartituresFesteresApp` usa aquest token per recarregar favorits, recents i ajustos després de tornar de `PdfViewerActivity`.

## Medium navigation
- `MEDIUM` usa una barra lateral de 198 dp.
- Manté iconos + noms de les cinc seccions principals.
- La barra és verticalment scrollable per evitar opcions inaccessibles en finestres baixes.
- `COMPACT` continua usant navegació inferior.
- `EXPANDED` conserva la barra completa original de 236 dp.

## Loading behavior
- No s'utilitza la pantalla intermèdia `Carregant partitura…` de v1.7.1.
- Es recupera el flux de v1.7.0: l'Activity del visor obri directament el contingut i el propi `PdfViewerScreen` gestiona el renderitzat inicial.

## Validation
- XML del Manifest i recursos validats estructuralment.
- Revisió de sintaxi Kotlin amb `kotlinc`: no s'han detectat errors de parser en els fitxers modificats.
- Aquest entorn no disposa d'un SDK/Gradle Android complet; la compilació final s'ha de validar en Android Studio.

## Version
- versionCode 33
- versionName 1.7.5


## Easter egg musical (v1.7.4)
- Nova pantalla `EasterEggGameScreen.kt`, implementada només amb Compose Canvas.
- Activació oculta: mantindre 3 s el centre del rosetó inferior dret des de Biblioteca (pantalles no compactes).
- Entrada amb aclarit breu de la interfície abans de mostrar el joc.
- Endless runner sense motor extern: salt, gravetat, obstacles musicals, col·lisions, dificultat progressiva i rècord en SharedPreferences.
- No afegeix permisos ni dependències.


## Ajustes del easter egg (v1.7.4)

- El saxofón se ha redibujado programáticamente con una silueta más reconocible (boquilla, tudel, cuerpo, llaves, codo y campana), conservando cara y patas.
- Las hitboxes del saxofón y de las figuras musicales se han reducido para hacer los saltos más justos.
- En pantallas compactas, mantener pulsada durante 3 s la cabecera de marca (emblema + “Partitures Festeres”) abre el easter egg; en tablet se conserva el acceso por el rosetón.
- Eines incorpora una pestaña “???” con una pista sutil del secreto.


## Ajustos v1.7.5
- `MetronomeEngine` utilitza només dos timbres estables: accent del primer temps i clic dèbil per a qualsevol altre esdeveniment.
- S'eliminen 2/2 i 3/8 del selector; es mantenen 2/4, 3/4, 4/4, 6/8, 9/8 i 12/8.
- Els compassos compostos conserven les subdivisions ternàries, però totes les subdivisions no inicials sonen igual per evitar confusió tímbrica.
- `EasterEggGameScreen` redueix de nou les hitboxes del saxo i dels obstacles.
- El generador d'obstacles incorpora figures, silencis, alteracions, clau de sol, calderó i notes lligades, amb desbloqueig progressiu.
- A partir d'una certa duració poden aparéixer obstacles aeris a dues altures.
- L'espai entre obstacles i el tipus seleccionat es randomitzen en cada partida mantenint marges que eviten seqüències injustes.
## v1.8.2
- Metrònom: sis compassos distribuïts en una graella 3x2, amb botons de la mateixa mida.
- Joc ocult: parella de corxeres unides amb una sola barra dibuixada explícitament.
- Joc ocult: grup de quatre semicorxeres amb dues barres, afegit com a obstacle independent.


## v1.8.2 — Dictat Marina
- `DictationMarinaPanel.kt`: captura amb `TunerEngine`, estabilització temporal (~150 ms), conversió MIDI i pentagrama responsive.
- Exportació PNG amb `MediaStore` (Android 10+) i carpeta externa pròpia en Android 8/9.
- Compartició mitjançant `FileProvider` i `ACTION_SEND`.
- No s’afegeixen permisos nous: reutilitza `RECORD_AUDIO`.


- v1.8.2: la nova eina passa a dir-se **Dictat Musical** i incorpora un submode **Dictat Marina** pensat per convertir la veu en una seqüència ràpida de notes sobre pentagrama, amb exportació i compartició d’imatge.

- v1.8.2: substituïda la mascota de Dictat Marina per una il·lustració PNG transparent i afegida una normalització d’octava en Dictat Marina perquè la veu es represente dins del rang C4–B5 de clau de sol.
