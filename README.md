# Partitures Festeres v1.6.5

Aplicació Android per organitzar, visualitzar i anotar partitures, pensada especialment per a ús en tablet durant assajos i actuacions.

## Canvis de la v1.6.5
- Revisió visual global dels contenidors i controls: els panells principals comparteixen ara el mateix acabat de pergamí, contorn daurat subtil i sense dobles requadres blancs/grisos.
- Camps de text unificats amb fons de pergamí i vores coherents amb la resta de la interfície.
- `Eines musicals` revisat a fons: pestanyes, selectors, octaves i accessos ràpids ja no depenen dels botons clars de Material que podien mostrar un segon rectangle intern.
- `Nota de referència`: quadrícula de notes reorganitzada en 4 columnes perquè `Do♯/Re♭` i `Fa♯/Sol♭` no es retallen ni deformen.
- Metrònom reescrit amb un motor `AudioTrack` de bucle PCM, amb temporització basada en mostres en lloc de llançar sons des de la UI.
- Nous compassos: 2/4, 3/4, 4/4, 2/2, 3/8, 6/8, 9/8 i 12/8.
- En 6/8, 9/8 i 12/8 el BPM representa el pols principal i el motor genera les tres subdivisions de corxera de cada pols.
- Nou so sec tipus metrònom mecànic, amb lleugera alternança tick/tock, accent de primer temps i accents secundaris en compassos compostos.
- Manté valencià per defecte, castellà seleccionable, iconografia adaptativa, widget i copyright de versions anteriors.

## Versió
- versionCode: 27
- versionName: 1.6.5
