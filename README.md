# KeepTrack

App Android (KeepTrack) para seguir una rutina semanal: ver el día, marcar bloques hechos,
recibir avisos y revisar el progreso. Uso personal en un solo móvil: todo se
guarda en local (Room), sin cuentas, sin servidor, sin analítica y sin permiso
de Internet.

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

## Primer arranque

1. La app trae una rutina de ejemplo. Sustitúyela en Rutina > Importar o edítala.
2. En la pantalla Hoy aparece una tarjeta de permisos. Concede notificaciones,
   alarmas exactas y la exclusión de la optimización de batería; sin ellos los
   avisos pueden no llegar o llegar tarde.
3. Para el widget: mantén pulsada la pantalla de inicio > Widgets > KeepTrack.

## Pantallas

- **Hoy**: línea de tiempo, bloque en curso, cuenta atrás al siguiente, tick para
  marcar hecho y pulsación larga para saltar o posponer. Botón Imprevisto y
  tipo de día (normal, festivo, libre).
- **Semana**: los 7 días con sus bloques por colores y el porcentaje completado.
- **Estadísticas**: horas previstas y hechas por categoría, rachas e historial.
- **Rutina**: crear, modificar, duplicar y borrar bloques, copiar un día en otro,
  aviso de solapamientos, e Importar.
- **Ajustes** (rueda en Hoy): avisos, horas de silencio, permisos y copia de seguridad.

## Importar una rutina

Rutina > Importar. Pega el texto y pulsa Analizar; el formato se detecta solo.
Antes de guardar se muestra una vista previa con los bloques por día, las líneas
no entendidas (editables), solapamientos, huecos y horas de sueño por noche.

### Formato A: texto

Una cabecera por día y una línea por bloque. La categoría entre corchetes es
opcional: si falta, se deduce del título.

```
Lunes
08:00-09:00 DIW [clase]
12:30-12:45 Vuelta a casa [desplazamiento]

Martes y jueves
17:00-19:00 Academia de inglés

Lunes a viernes
23:00-07:00 Dormir
```

Se aceptan:

- Guiones `-`, `–`, `—` y también `a` o `hasta` entre las horas.
- Horas como `8:00`, `08.00`, `8h`, `8h30`, `8`.
- Días con o sin tilde, abreviados (`lun`, `mié`, `X`), rangos (`Lunes a viernes`,
  `L-V`), listas (`martes y jueves`), `entre semana`, `fin de semana`, `todos los días`.
- Viñetas y listas numeradas delante de la línea.
- Tablas Markdown con columnas `Hora | Actividad | Duración`. `Hora` puede ser un
  rango o solo el inicio; en ese caso el fin sale de `Duración` (`1h`, `45 min`,
  `1h 30min`, `1,5 h`) o del inicio de la fila siguiente. Columnas opcionales:
  `Día` y `Categoría`.

Un bloque cuyo fin es menor que su inicio cruza la medianoche y pertenece al
día en que empieza.

### Formato B: JSON

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

### Prompt para IA

El botón "Copiar prompt para IA" copia unas instrucciones con este esquema.
Pégalas en cualquier asistente junto a tus horarios y pega aquí el JSON que te
devuelva. La app no llama a ninguna IA ni usa la red.

## Avisos

- Una alarma exacta (`setExactAndAllowWhileIdle`) para el próximo aviso; al sonar
  se programa la siguiente. Funciona con la app cerrada y en modo Doze.
- Acciones en la notificación: Hecho, Posponer 10 min, Saltar.
- Aviso opcional de fin de día si quedan bloques sin marcar.
- Se reprograma al reiniciar el móvil, al cambiar la hora y al editar la rutina.
- Los bloques de sueño y libre no avisan por defecto. Los días festivos o libres no avisan.

## Copia de seguridad

Ajustes > Copia de seguridad. Exportar guarda un JSON con la plantilla, el
historial, los días especiales y los ajustes. Importar sustituye todos los datos
de la app por los del archivo.

## Cómo se guardan los datos

- `blocks`: plantilla semanal, una fila por bloque y día.
- `day_entries`: bloques de cada fecha con su estado y hora real de marcado. Es
  una copia de la plantilla hecha ese día, así que cambiar la plantilla no altera
  el historial. Los imprevistos solo cambian estas filas.
- `days`: tipo de cada día (normal, festivo, libre).

## Pruebas

```
./gradlew testDebugUnitTest
```

Cubren el parser de importación (incluidos casos mal formados), solapamientos,
estadísticas y rachas, imprevistos y la copia de seguridad.
