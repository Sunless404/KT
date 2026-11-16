# Rutina

App Android para seguir una rutina semanal: ver el día, marcar bloques hechos,
recibir avisos y revisar el progreso. Uso personal, todo local (Room), sin
cuentas, sin servidor y sin permiso de Internet.

## Generar el APK

Requisitos: JDK 17 o superior y el SDK de Android (plataforma 35). Con Android
Studio instalado ya los tienes.

```
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

Si Gradle no encuentra el SDK, crea `local.properties` en la raíz con la ruta:

```
sdk.dir=/ruta/al/Android/Sdk
```

## Instalarlo en el móvil

Con cable y depuración USB activada (Ajustes > Opciones de desarrollador):

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Sin cable: copia el APK al móvil, ábrelo desde el gestor de archivos y permite
"instalar aplicaciones desconocidas" para esa app cuando lo pida.

## Pruebas

```
./gradlew testDebugUnitTest
```
