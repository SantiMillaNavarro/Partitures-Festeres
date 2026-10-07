# Partitures Festeres v1.8.2

Aplicació Android per organitzar, visualitzar i anotar partitures, pensada per a assajos i actuacions i adaptada automàticament tant a tablets com a telèfons.


## Novetats v1.8.2
- Metrònom simplificat a dos únics sons: primer temps fort i resta de pulsacions/subdivisions dèbils.
- Compassos disponibles: 2/4, 3/4, 4/4, 6/8, 9/8 i 12/8.
- Minijoc ocult amb hitboxes més estrictes i indulgents visualment.
- Més varietat d'obstacles: figures, silencis, alteracions, lligadures i altres símbols musicals.
- Obstacles aeris a mesura que avança la partida.
- Generació aleatòria controlada perquè cada partida tinga un recorregut diferent sense combinacions impossibles.

## Easter egg v1.7.4
- Pulsació secreta de 3 segons sobre el centre del rosetó inferior dret de la Biblioteca en tablet.
- Transició d'entrada amb aclarit breu de la pantalla.
- Minijoc lleuger tipus endless runner amb un saxo amb potetes i figures musicals com a obstacles.
- Puntuació i rècord local, sense permisos, xarxa ni dependències addicionals.

## Estabilització v1.7.2
- El visor PDF passa a una `PdfViewerActivity` independent de `MainActivity`.
- La interfície principal ja no canvia la seua política d'orientació en entrar o eixir d'una partitura.
- `PdfViewerScreen` ja no escriu ni restaura `requestedOrientation` durant la composició/descomposició.
- El visor usa `fullSensor` com a política pròpia; si l'usuari desactiva la rotació del visor, es fixa a landscape una sola vegada en crear l'Activity.
- S'elimina la pantalla addicional `Carregant partitura…` introduïda en v1.7.1 i es recupera el comportament de càrrega de v1.7.0.
- Les tablets `MEDIUM`, com la Nokia T10, recuperen els noms de Biblioteca, Repertoris, Recents, Favorits i Eines en la barra lateral; la columna continua sent desplaçable verticalment si l'alçària és limitada.
- En tornar del visor, la pantalla principal refresca favorits, recents i ajustos compartits.
- Es manté íntegre el sistema responsive de v1.7.0: navegació inferior en mòbil, barra lateral adaptativa en tablet i disseny complet en pantalles grans.
- Widget, idiomes, anotacions, reparador PDF, metrònom, afinador i nota de referència es mantenen sense canvis funcionals.

## Arquitectura adaptativa
- `COMPACT`: amplària < 600 dp o alçària < 480 dp.
- `MEDIUM`: amplària < 840 dp o alçària < 600 dp.
- `EXPANDED`: resta de finestres.
- La selecció és automàtica segons l'espai real disponible; no depén del model del dispositiu.

## Versió
- versionCode: 33
- versionName: 1.7.5


## Dictat Marina v1.8.2
- Nova eina musical basada en el detector de freqüència de l’afinador.
- Registra notes estables i les representa sobre pentagrames en clau de sol.
- Afig pentagrames cap avall quan s’ompli l’espai disponible.
- En esta primera versió transcriu altura; les notes es dibuixen com a negres.
- En acabar, permet guardar el resultat com a PNG o compartir-lo amb el selector estàndard d’Android.


- v1.8.2: la nova eina passa a dir-se **Dictat Musical** i incorpora un submode **Dictat Marina** pensat per convertir la veu en una seqüència ràpida de notes sobre pentagrama, amb exportació i compartició d’imatge.

- v1.8.2: substituïda la mascota de Dictat Marina per una il·lustració PNG transparent i afegida una normalització d’octava en Dictat Marina perquè la veu es represente dins del rang C4–B5 de clau de sol.
