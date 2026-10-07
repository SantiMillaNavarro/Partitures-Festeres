# Notes tècniques — v1.6.3

Base: v1.6.2 bilingüe i estable.

## Adaptive launcher icon

La v1.6.3 substitueix l'antic bitmap de l'icona de l'aplicació per una configuració compatible amb **Adaptive Icons** (API 26+):

- `res/mipmap-anydpi-v26/ic_launcher.xml`
- `res/mipmap-anydpi-v26/ic_launcher_round.xml`
- `res/drawable-nodpi/ic_launcher_foreground.png`
- `res/values/colors.xml` (`ic_launcher_background`)
- fallback bitmap en `res/mipmap-nodpi/`

El `AndroidManifest.xml` apunta ara a:

- `android:icon="@mipmap/ic_launcher"`
- `android:roundIcon="@mipmap/ic_launcher_round"`

Això permet que el launcher d'Android determine la forma final sense encapsular el gràfic dins d'una icona blanca addicional.

## Widget

El widget **no forma part dels recursos del launcher**. Continua utilitzant:

`@drawable/widget_round_icon`

Tant el layout (`widget_partitures.xml`) com la previsualització (`partitures_widget_info.xml`) continuen apuntant al mateix recurs de v1.6.2.

## Compatibilitat

- `versionCode = 25`
- `versionName = 1.6.3`
- `minSdk = 26`
- `compileSdk / targetSdk = 36`

## Validació

S'han comprovat l'estructura dels recursos, el XML de les icones adaptatives, les referències del manifest i que el fitxer del widget siga binàriament idèntic al de v1.6.2. La validació final continua sent generar l'APK en Android Studio i comprovar la representació que fa el launcher concret de la tauleta.
