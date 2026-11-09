# AVD time · App Android

Temporizador de intervalos con modos 1 INT, 2 INT, 3+ INT y MULTI, cronómetro, reloj, 20 memorias, historial, plantillas, voz y pantalla completa.

La app completa está en `app/src/main/assets/index.html`. La parte nativa (`MainActivity.java`) añade voz en español, vibración, pantalla siempre encendida y copiar al portapapeles.

## Opción A: compilar gratis con GitHub (sin instalar nada)

1. Crea una cuenta en github.com y un repositorio nuevo (por ejemplo `avd-time`).
2. Sube **el contenido** de esta carpeta (no la carpeta en sí), incluida la carpeta oculta `.github`.
3. Ve a la pestaña **Actions**. La compilación arranca sola y tarda unos 3–5 minutos.
4. Al terminar (marca verde), entra en la ejecución y descarga **AVD-time-apk**. Dentro está `AVD-time-release.apk`.
5. En el teléfono, abre el APK y permite "Instalar apps desconocidas" cuando Android lo pida.

## Opción B: Android Studio

1. Abre esta carpeta con **File > Open**.
2. Espera a que termine la sincronización de Gradle.
3. **Build > Build App Bundle(s) / APK(s) > Build APK(s)**.

## Logo y colores

- Logo AVD time en `logo/` (versión cuadrada de 1024 px para redes o Play Store, y versiones negra y verde con fondo transparente).
- Icono de la app: `app/src/main/res/mipmap-*/` (fondo verde en `values/styles.xml`, color `ic_bg`).
- Colores de la app: bloque `:root` al principio de `app/src/main/assets/index.html` (`--brand` es el verde AVD #2EE59D).

## Notas

- Cada APK se firma siempre con la misma clave (`app/avd-time-upload.jks`, contraseña en `keystore.properties`), así que las versiones nuevas se instalan encima de la anterior sin borrar datos.
- Cada cambio subido a `main` compila una versión nueva (1.0.N) y la publica en Releases. La app la detecta al abrirse y muestra el botón **Actualizar**.
- Aviso: el repositorio es público y la clave de firma es visible. Para protegerla, pon el repositorio en privado (Settings > General > Danger Zone > Change visibility).
- GitHub genera dos archivos: el AAB (para Google Play) y el APK (para instalar directo en un teléfono).
- Con la pantalla bloqueada Android puede pausar la app. Mientras entrenas la pantalla se mantiene encendida sola.
- Los programas, memorias e historial se guardan en el teléfono.
