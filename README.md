# AVD time

Temporizador de intervalos para entrenamiento de AVD · Entrenamiento físico deportivo: modos 1 INT, 2 INT, 3+ INT y MULTI, plantillas (Tabata, EMOM, AMRAP, For Time, Boxeo), voz en español, pantalla completa, 20 memorias, historial semanal, cronómetro y reloj.

**Descargar:** https://github.com/adrianvillarroeldelgado-max/AVD-TIME/releases/latest/download/AVD-time.apk

Para publicar versiones y configurar la firma, sigue **[PUBLICAR.md](PUBLICAR.md)**.

## Estructura

| Carpeta | Contenido |
|---|---|
| `app/src/main/assets/index.html` | La app completa (pantallas, temporizador, memorias, historial) |
| `app/src/main/java/.../MainActivity.java` | Voz, vibración, pantalla encendida, portapapeles |
| `app/src/github/` | Versión de descarga directa: busca e instala actualizaciones desde GitHub Releases |
| `app/src/play/` | Versión para Google Play: sin internet ni actualizador (las actualiza Play) |
| `.github/workflows/release.yml` | Publica una versión firmada en cada cambio a `main` |
| `.github/workflows/build-apk.yml` | APK de prueba en otras ramas |
| `docs/` | Página de descarga y política de privacidad (GitHub Pages) |
| `play/` | Icono, gráfico destacado y capturas para Google Play |
| `VERSION` | Los dos primeros números de la versión (1.0) |

## Logo y colores

- Icono de la app: `app/src/main/res/mipmap-*/` (fondo verde `ic_bg` en `values/styles.xml`).
- Colores de la app: bloque `:root` al principio de `app/src/main/assets/index.html` (`--brand` es el verde AVD #2EE59D).

La clave de firma **no** está en el repositorio: va en los secretos de GitHub (PUBLICAR.md, paso 1).
