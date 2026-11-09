package com.avd.time;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInstaller;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Actualización automática de la versión descargada desde GitHub (igual que AVD jump).
 *
 * Cada versión publicada en GitHub Releases incluye "version.json" con su código de versión.
 * Al abrir la app (como máximo cada 3 horas) se consulta la última versión; si es más nueva,
 * se ofrece actualizar: se descarga el APK y Android pide confirmar la instalación.
 * Como todas las versiones están firmadas con la misma clave, se instala encima sin perder datos.
 * Solo se consulta GitHub; no se envía ningún dato del usuario.
 */
public final class Updater {

    /** La versión de Google Play no tiene actualizador (Play actualiza la app). */
    public static final boolean ENABLED = true;

    private static final String PREFS = "avdtime";
    private static final String KEY_LAST = "updateCheckMs";
    private static final String KEY_SKIP = "updateSkipCode";
    private static final long EVERY_MS = 3L * 60 * 60 * 1000;

    private static final class Info {
        final int code; final String name; final String apkUrl; final String notes;
        Info(int code, String name, String apkUrl, String notes) {
            this.code = code; this.name = name; this.apkUrl = apkUrl; this.notes = notes;
        }
    }

    /** Versión que espera el permiso "instalar apps desconocidas" para continuar. */
    private static Info pending;
    private static volatile boolean busy = false;

    private Updater() { }

    private static String base() {
        return "https://github.com/" + BuildConfig.UPDATE_REPO + "/releases/latest/download";
    }

    /** Busca una versión nueva. manual = el usuario pulsó "Buscar actualizaciones". */
    public static void check(Activity a, boolean manual) {
        if (busy) return;
        SharedPreferences p = a.getSharedPreferences(PREFS, Context.MODE_PRIVATE);

        // Volvió de dar el permiso de instalación: seguir con la descarga.
        if (pending != null && canInstall(a)) {
            Info info = pending; pending = null; download(a, info); return;
        }
        long now = System.currentTimeMillis();
        if (!manual && now - p.getLong(KEY_LAST, 0L) < EVERY_MS) return;
        p.edit().putLong(KEY_LAST, now).apply();

        busy = true;
        new Thread(() -> {
            Info info;
            try { info = fetch(); } catch (Exception e) { info = null; }
            busy = false;
            final Info found = info;
            a.runOnUiThread(() -> {
                if (a.isFinishing() || a.isDestroyed()) return;
                if (found == null) {
                    if (manual) toast(a, "No se pudo comprobar. Revisa la conexión a internet.");
                } else if (found.code <= BuildConfig.VERSION_CODE) {
                    if (manual) toast(a, "Ya tienes la última versión (" + BuildConfig.VERSION_NAME + ").");
                } else if (!manual && p.getInt(KEY_SKIP, -1) == found.code) {
                    // El usuario eligió omitir esta versión.
                } else {
                    offer(a, found);
                }
            });
        }).start();
    }

    /** Abre una conexión siguiendo las redirecciones de GitHub. */
    private static HttpURLConnection open(String url, int connectMs, int readMs) throws Exception {
        for (int hops = 0; hops < 6; hops++) {
            HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
            c.setConnectTimeout(connectMs);
            c.setReadTimeout(readMs);
            c.setInstanceFollowRedirects(false);
            c.setRequestProperty("User-Agent", "AVD-time");
            int code = c.getResponseCode();
            if (code >= 300 && code < 400) {
                String loc = c.getHeaderField("Location");
                c.disconnect();
                if (loc == null) break;
                url = loc;
                continue;
            }
            return c;
        }
        throw new Exception("demasiadas redirecciones");
    }

    private static Info fetch() throws Exception {
        HttpURLConnection c = open(base() + "/version.json", 8000, 8000);
        try {
            if (c.getResponseCode() != 200) return null;
            InputStream in = c.getInputStream();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            JSONObject o = new JSONObject(out.toString("UTF-8"));
            String apk = o.optString("apk", "");
            if (apk.trim().isEmpty()) apk = base() + "/AVD-time.apk";
            return new Info(o.getInt("versionCode"), o.optString("versionName", "?"), apk, o.optString("notes", ""));
        } finally {
            c.disconnect();
        }
    }

    private static void offer(Activity a, Info info) {
        StringBuilder msg = new StringBuilder();
        msg.append("Tienes la ").append(BuildConfig.VERSION_NAME).append(". ");
        msg.append("La nueva versión se instala encima: no se pierden tus programas, memorias ni historial.");
        if (!info.notes.trim().isEmpty()) {
            String notes = info.notes.length() > 600 ? info.notes.substring(0, 600) : info.notes;
            msg.append("\n\nNovedades:\n").append(notes);
        }
        new AlertDialog.Builder(a)
                .setTitle("Nueva versión " + info.name)
                .setMessage(msg.toString())
                .setPositiveButton("Actualizar", (d, w) -> start(a, info))
                .setNegativeButton("Más tarde", null)
                .setNeutralButton("Omitir esta", (d, w) ->
                        a.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putInt(KEY_SKIP, info.code).apply())
                .show();
    }

    private static boolean canInstall(Context ctx) {
        return Build.VERSION.SDK_INT < 26 || ctx.getPackageManager().canRequestPackageInstalls();
    }

    private static void start(Activity a, Info info) {
        if (canInstall(a)) { download(a, info); return; }
        pending = info;
        new AlertDialog.Builder(a)
                .setTitle("Permitir actualizaciones")
                .setMessage("Para instalar la versión nueva, activa “Permitir de esta fuente” para AVD time y vuelve a la app. Solo hay que hacerlo una vez.")
                .setPositiveButton("Abrir ajustes", (d, w) -> {
                    try {
                        a.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                Uri.parse("package:" + a.getPackageName())));
                    } catch (Exception e) {
                        toast(a, "Abre Ajustes > Apps > AVD time > Instalar apps desconocidas.");
                    }
                })
                .setNegativeButton("Cancelar", (d, w) -> pending = null)
                .show();
    }

    private static void download(Activity a, Info info) {
        float dp = a.getResources().getDisplayMetrics().density;
        TextView status = new TextView(a);
        status.setText("Descargando 0 %");
        status.setTextSize(15f);
        status.setPadding((int) (24 * dp), (int) (16 * dp), (int) (24 * dp), (int) (8 * dp));
        AlertDialog dlg = new AlertDialog.Builder(a)
                .setTitle("AVD time " + info.name)
                .setView(status)
                .setCancelable(false)
                .show();
        busy = true;
        new Thread(() -> {
            File f = new File(a.getCacheDir(), "actualizacion.apk");
            String err = null;
            try {
                HttpURLConnection c = open(info.apkUrl, 10000, 20000);
                if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
                long total = c.getContentLengthLong();
                InputStream in = c.getInputStream();
                OutputStream out = new FileOutputStream(f);
                byte[] buf = new byte[64 * 1024];
                long done = 0;
                int last = -1;
                int n;
                while ((n = in.read(buf)) > 0) {
                    out.write(buf, 0, n);
                    done += n;
                    if (total > 0) {
                        int pct = (int) (done * 100 / total);
                        if (pct != last) {
                            last = pct;
                            a.runOnUiThread(() -> status.setText("Descargando " + pct + " %"));
                        }
                    }
                }
                out.close();
                in.close();
                c.disconnect();
                a.runOnUiThread(() -> status.setText("Preparando la instalación…"));
                install(a.getApplicationContext(), f);
            } catch (Exception e) {
                err = e.getMessage() != null ? e.getMessage() : "error desconocido";
            }
            busy = false;
            final String error = err;
            a.runOnUiThread(() -> {
                try { dlg.dismiss(); } catch (Exception ignored) { }
                if (error != null) toast(a, "No se pudo descargar la actualización: " + error);
            });
        }).start();
    }

    private static void install(Context ctx, File apk) throws Exception {
        PackageInstaller pi = ctx.getPackageManager().getPackageInstaller();
        PackageInstaller.SessionParams params =
                new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
        params.setAppPackageName(ctx.getPackageName());
        if (Build.VERSION.SDK_INT >= 31) {
            // Desde Android 12, si AVD time ya instaló una versión antes, las siguientes pueden no pedir confirmación.
            params.setRequireUserAction(PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED);
        }
        int id = pi.createSession(params);
        try (PackageInstaller.Session s = pi.openSession(id)) {
            try (OutputStream out = s.openWrite("avd-time.apk", 0, apk.length());
                 InputStream in = new FileInputStream(apk)) {
                byte[] buf = new byte[64 * 1024];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                s.fsync(out);
            }
            int flags = PendingIntent.FLAG_UPDATE_CURRENT
                    | (Build.VERSION.SDK_INT >= 31 ? PendingIntent.FLAG_MUTABLE : 0);
            PendingIntent pend = PendingIntent.getBroadcast(ctx, id, new Intent(ctx, UpdateReceiver.class), flags);
            s.commit(pend.getIntentSender());
        }
    }

    private static void toast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }
}
