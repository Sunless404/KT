# KeepTrack

App Android (KeepTrack) para seguir una rutina semanal: ver el día, marcar bloques hechos,
recibir avisos y revisar el progreso. Uso personal en un solo móvil: todo se
guarda en local (Room), sin cuentas, sin servidor y sin analítica. La app solo
usa Internet para el asistente de IA, y únicamente cuando lo usas.

Kotlin, Jetpack Compose (Material 3, tema claro y oscuro), minSdk 26, targetSdk 35.

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

Al actualizar, instala siempre un APK generado en el mismo ordenador: la firma
de depuración cambia de una máquina a otra y Android rechaza la actualización
si no coincide. Si ocurre, exporta una copia de seguridad, desinstala e instala.

## Actualizaciones automáticas

Cada cambio que llega a `main` lanza el flujo `.github/workflows/publicar.yml`:
pasa las pruebas, compila un APK firmado siempre con la misma clave y lo publica
como Release (`build-N`, versión `1.N`) con el mensaje del commit como novedades.

La app consulta la última Release al abrirse (como mucho cada 30 min). Si hay
una versión más nueva, muestra «Nueva versión disponible» con las novedades;
«Actualizar» la descarga y abre el instalador de Android. La primera vez Android
pide permitir a KeepTrack instalar aplicaciones. Los datos se conservan.
En Perfil > Ajustes > Versión se puede buscar a mano.

Secrets necesarios en GitHub (Settings > Secrets and variables > Actions):

| Secret | Contenido |
|---|---|
| `KEYSTORE_BASE64` | La clave de firma (`.jks`) en base64 |
| `KEYSTORE_PASSWORD` | Contraseña de la clave |
| `KEY_ALIAS` | `keeptrack` |
| `KEY_PASSWORD` | Contraseña de la clave (la misma) |
| `GOOGLE_SERVICES_JSON` | Contenido de `google-services.json` |

La clave de firma no está en el repositorio. Guárdala bien: si se pierde, las
versiones nuevas no podrán actualizar las ya instaladas.

Un APK compilado en otro ordenador (`assembleDebug`) va firmado con otra clave y
no puede actualizar al publicado, ni al revés.

## Primer arranque

1. Al abrir la app puedes entrar con tu cuenta, crear una o seguir como invitado
   (todo se queda en el móvil; la cuenta se puede crear después desde el Perfil).
2. La app trae una rutina de ejemplo. Cámbiala desde Rutina o pídeselo al Asistente.
3. En Hoy aparece una tarjeta de permisos: concede notificaciones, alarmas exactas
   y la exclusión de la optimización de batería, o los avisos pueden no llegar.
4. Para el widget: mantén pulsada la pantalla de inicio > Widgets > KeepTrack.

En cada apartado, el botón «?» explica brevemente cómo usarlo, y la foto de
arriba a la derecha abre tu Perfil.

## Pantallas

- **Hoy**: desliza a los lados para cambiar de día. Arriba, la semana; debajo, el
  progreso del día, el bloque en curso con su cuenta atrás, las tareas extra de ese
  día y la línea de tiempo de la rutina. Tick para marcar; pulsación larga para
  saltar o posponer. Botón Imprevisto y tipo de día (normal, festivo, libre).
- **Tareas**: calendario mensual con el día de hoy en un círculo y las tareas bajo
  cada número. Al tocar un día se abre el detalle. "Nueva tarea" pide nombre, día,
  hora (opcional), descripción (opcional) y si quieres recordatorio y a qué hora.
- **Estadísticas**: rachas animadas, horas previstas y hechas por categoría e
  historial de semanas. Tocar la racha de gimnasio abre una sección secreta.
- **Rutina**: lunes a domingo; al tocar un día se despliega su rutina para
  añadir, modificar, duplicar o borrar bloques y copiar el día en otros.
- **Asistente**: chat con IA (texto, fotos de la galería o la cámara y notas de voz).
- **Perfil** (foto arriba a la derecha): foto de la galería, nombre visible,
  biografía, estadísticas destacadas, logros y la cuenta.
- **Ajustes** (rueda en Perfil): avisos, horas de silencio, permisos, cuenta,
  clave de la IA y copia de seguridad.

Rachas: cuanto más larga, más "arde" el icono: Chispa (1 día), Llama (3),
Hoguera (7), Incendio (14) y Leyenda (30), con halo, llama y chispas.

## Gimnasio (sección secreta)

Registra sesiones con ejercicios, series, peso y repeticiones. Puntos:

- Máximo estimado a 1 repetición (Epley): peso × (1 + reps / 30), hasta 12 reps.
- Fuerza relativa: ese máximo dividido por tu peso corporal.
- Semanal (se reinicia el lunes): 10 por sesión (hasta 5), por cada récord
  5 × % de mejora (entre 5 y 50; un ejercicio nuevo da 5) y 5 por cada semana
  seguida yendo (hasta 25).
- Histórica: suma de la fuerza relativa de tus 5 mejores ejercicios × 100.

## Formato de rutina (JSON)

Es el formato que usa el asistente para proponer rutinas.

```json
{
  "version": 1,
  "bloques": [
    {
      "titulo": "DIW",
      "categoria": "clase",
      "dias": ["lunes", "miercoles"],
      "inicio": "08:00",
      "fin": "09:00",
      "notas": "Aula 2",
      "aviso": true,
      "antelacion": 5
    }
  ]
}
```

| Campo | Obligatorio | Valor |
|---|---|---|
| `titulo` | sí | Texto del bloque |
| `dias` | sí | Lista de nombres de día o números 1 (lunes) a 7 (domingo). También un texto como `"L-V"` |
| `inicio`, `fin` | sí | `"HH:MM"` en 24 h. `fin` menor que `inicio` cruza la medianoche |
| `categoria` | no | `clase`, `academia`, `estudio`, `gimnasio`, `desplazamiento`, `comida`, `sueno`, `lectura`, `higiene`, `libre`, `otro`. Si falta, se deduce del título |
| `notas` | no | Texto libre |
| `aviso` | no | `true` o `false`. Por defecto `false` en `sueno` y `libre`, `true` en el resto |
| `antelacion` | no | Minutos antes del inicio (0 a 180). Si falta, se usa el valor de Ajustes |

También se acepta directamente la lista de bloques sin el objeto exterior, y el
JSON dentro de un bloque de código Markdown.

## Asistente IA (Google Gemini, gratis)

En la pestaña Asistente puedes pedir una rutina nueva, cambios en la actual
("pasa el gimnasio al martes a las 19:00") o tareas ("dentista el viernes a las
17:00"), por escrito, con una foto (por ejemplo de tu horario de clases) o con
una nota de voz. Los cambios aparecen en una tarjeta y solo se guardan si pulsas
"Aplicar".

Configuración, una sola vez, en Ajustes > Asistente IA:

1. "Conseguir clave" abre Google AI Studio (aistudio.google.com/apikey). Inicia
   sesión con una cuenta de Google y crea una clave de API. No pide tarjeta.
2. Pégala y pulsa "Guardar y probar". La app lista los modelos de tu clave y
   elige uno "flash", que son los del plan gratuito.

Qué se envía: tus mensajes y adjuntos, la plantilla semanal y las tareas de los
próximos 30 días. El historial, las estadísticas, el gimnasio y los ajustes no
se envían. En el plan gratuito Google puede usar ese contenido para
mejorar sus productos. La clave se guarda cifrada con el Android Keystore y no
va en la copia de seguridad. Si se alcanza el límite gratuito, la app lo dice y
basta con esperar.

## Cuenta, nube y amigos (Firebase)

Opcional. Con una cuenta (correo, contraseña y nombre de usuario) tus datos se
guardan en la nube y puedes competir con tus amigos en el gimnasio.

- Se entra al abrir la app, desde Perfil o desde Gimnasio > Clasificación.
- Tu nombre visible, biografía y foto (256 px, guardada en tu perfil de Firestore)
  los ven tus amigos en la clasificación.
- Sincronización: unos segundos después de cada cambio se sube una copia
  completa (rutina, tareas, historial, gimnasio y ajustes) a tu cuenta. Al
  iniciar sesión en un móvil donde ya hay datos, la app pregunta si te quedas
  con los de la nube o con los del móvil.
- Amigos: los añades por su nombre de usuario y ves su puntuación. Para que
  ellos te vean, tienen que añadirte también.
- La clave de la IA no se sube.

### Configuración (una vez)

1. Proyecto de Firebase con una app Android con paquete `com.sunless404.keeptrack`.
2. Descarga `google-services.json` y ponlo en `app/google-services.json`. No se
   sube al repositorio (está en `.gitignore`). Sin él la app compila igual, pero
   sin funciones de nube.
3. Authentication: activa "Correo electrónico/contraseña".
4. Firestore Database: edición Standard, modo producción.
5. Firestore > Reglas: pega el contenido de `firestore.rules` y pulsa Publicar.

Datos en Firestore:

- `users/{uid}`: perfil público (usuario, nombre, biografía, foto, puntos y fuerza).
- `users/{uid}/friends/{uid}`: amigos que has añadido (privado).
- `users/{uid}/sync/*`: copia de tus datos comprimida (privado).
- `usernames/{usuario}`: garantiza que cada nombre de usuario es único.

Las puntuaciones las calcula el móvil; las reglas limitan los valores, pero
alguien con conocimientos técnicos podría falsear las suyas.

## Avisos

- Una alarma exacta (`setExactAndAllowWhileIdle`) para el próximo aviso; al sonar
  se programa la siguiente. Funciona con la app cerrada y en modo Doze.
- Acciones en la notificación: Hecho, Posponer 10 min, Saltar.
- Aviso opcional de fin de día si quedan bloques sin marcar.
- Se reprograma al reiniciar el móvil, al cambiar la hora y al editar la rutina.
- Los bloques de sueño y libre no avisan por defecto. Los días festivos o libres no avisan.

## Copia de seguridad

Ajustes > Copia de seguridad. Exportar guarda un JSON con la plantilla, el
historial, los días especiales, las tareas, el gimnasio y los ajustes. Importar sustituye todos los datos
de la app por los del archivo.

## Cómo se guardan los datos

- `blocks`: plantilla semanal, una fila por bloque y día.
- `day_entries`: bloques de cada fecha con su estado y hora real de marcado. Es
  una copia de la plantilla hecha ese día, así que cambiar la plantilla no altera
  el historial. Los imprevistos solo cambian estas filas.
- `days`: tipo de cada día (normal, festivo, libre).
- `tasks`: tareas puntuales con su recordatorio.
- `gym_sessions` y `gym_sets`: sesiones de gimnasio y sus series.

El esquema de cada versión de la base de datos se guarda en `app/schemas`.

## Pruebas

```
./gradlew testDebugUnitTest
```

Cubren el parser de rutinas (incluidos casos mal formados), solapamientos,
estadísticas y rachas, imprevistos, el asistente, los puntos del gimnasio y la
copia de seguridad.

Capturas de las pantallas principales sin emulador (Robolectric + Roborazzi),
en `app/build/capturas`:

```
./gradlew testDebugUnitTest -Pcapturas --tests '*ScreenshotTest*'
```
