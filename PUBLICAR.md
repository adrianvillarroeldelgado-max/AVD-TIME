# Publicar AVD time (APK con actualización automática)

Funciona igual que AVD jump:

- **Enlace de descarga fijo** que siempre entrega la última versión:
  `https://github.com/adrianvillarroeldelgado-max/AVD-TIME/releases/latest/download/AVD-time.apk`
- **Página de descarga** con el logo: `https://adrianvillarroeldelgado-max.github.io/AVD-TIME/`
- **La app se actualiza sola**: al abrirse avisa de la versión nueva y la instala encima, sin borrar programas, memorias ni historial.

---

## 1. Guardar la clave de firma como secretos (una sola vez)

La clave de firma hace que cada versión nueva se instale **encima** de la anterior. Te la entregué aparte (`AVD-time-firma.zip`). **No la subas nunca al repositorio.**

En el repositorio: **Settings › Secrets and variables › Actions › New repository secret**. Crea estos cuatro:

| Nombre del secreto | Valor |
|---|---|
| `KEYSTORE_BASE64` | todo el contenido de `avd-time-release.jks.base64` |
| `KEYSTORE_PASSWORD` | la contraseña de `CLAVES-NO-SUBIR.txt` |
| `KEY_ALIAS` | `avdtime` |
| `KEY_PASSWORD` | la misma contraseña |

Después: **Actions › Publicar versión › Run workflow** para publicar la primera versión.

Guarda el zip de la firma en un lugar seguro (por ejemplo, Google Drive personal). Si se pierde, las versiones nuevas no se podrán instalar encima.

## 2. Publicar una versión (automático)

**Cada cambio que llega a la rama `main` publica una versión nueva sola.** GitHub compila, firma y publica en Releases (5–7 min) con el número `1.0.N`. Los cambios que solo tocan `docs/`, `play/` o archivos `.md` no publican versión.

En cada versión aparecen `AVD-time.apk`, `AVD-time-1.0.N.apk`, `AVD-time-1.0.N-play.aab` (para Google Play) y `version.json` (lo consulta la app).

- **Actualización en los teléfonos:** la app revisa al abrirse (como máximo cada 3 horas) si hay una versión nueva y ofrece **Actualizar**, **Más tarde** u **Omitir esta**. También hay un botón **Buscar actualizaciones** al final de la pestaña Historial.
- **La primera vez** Android pide activar "Permitir de esta fuente" para AVD time. Solo una vez.
- **Probar sin publicar:** trabaja en otra rama. Cada cambio en una rama genera un APK de prueba en **Actions** (artefacto *AVD-time-apk*).
- **Cambiar 1.0 a 1.1:** edita el archivo `VERSION`.

## 3. Activar la página de descarga

1. **Settings › Pages**.
2. *Build and deployment*: **Source: Deploy from a branch**, rama **main**, carpeta **/docs**. Guarda.
3. En 1–2 minutos estará en `https://adrianvillarroeldelgado-max.github.io/AVD-TIME/`.

## Instalar en el teléfono

1. Abre el enlace desde el teléfono y descarga el APK.
2. Ábrelo y permite **Instalar apps desconocidas** para el navegador.
3. Pulsa **Instalar**. Si Play Protect avisa: **Más detalles › Instalar de todas formas**.

## Google Play Store

El icono (`play/icono-512.png`), el gráfico destacado (`play/grafico-destacado-1024x500.png`) y las capturas (`play/capturas/`) están listos. El archivo para subir es `AVD-time-1.0.N-play.aab` de cada versión. La política de privacidad queda en `https://adrianvillarroeldelgado-max.github.io/AVD-TIME/privacidad.html`.
