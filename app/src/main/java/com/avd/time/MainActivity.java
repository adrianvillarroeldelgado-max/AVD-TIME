package com.avd.time;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.os.VibratorManager;
import android.speech.tts.TextToSpeech;
import android.graphics.Insets;
import android.view.View;
import android.view.WindowInsets;
import android.window.OnBackInvokedDispatcher;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import org.json.JSONArray;


import java.util.Locale;

/**
 * AVD time: muestra la app (assets/index.html) en un WebView y le da acceso
 * a voz (TextToSpeech), vibración, pantalla encendida y portapapeles
 * mediante el objeto JavaScript window.AVDNative.
 */
public class MainActivity extends Activity {

    private WebView web;
    private TextToSpeech tts;
    private boolean ttsReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#0d0f12"));
        getWindow().setNavigationBarColor(Color.parseColor("#0d0f12"));

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#0d0f12"));
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setTextZoom(100);
        web.setWebViewClient(new WebViewClient());
        web.setWebChromeClient(new WebChromeClient());
        web.addJavascriptInterface(new Bridge(), "AVDNative");
        setContentView(web);

        // Android 15+ dibuja la app bajo las barras del sistema: dejamos ese espacio libre.
        if (Build.VERSION.SDK_INT >= 30) {
            web.setOnApplyWindowInsetsListener((v, insets) -> {
                Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                v.setPadding(i.left, i.top, i.right, i.bottom);
                return WindowInsets.CONSUMED;
            });
        }

        // Botón atrás: primero cierra la pantalla completa del temporizador.
        if (Build.VERSION.SDK_INT >= 33) {
            getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, this::handleBack);
        }

        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                int r = tts.setLanguage(new Locale("es", "ES"));
                if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts.setLanguage(new Locale("es"));
                }
                tts.setSpeechRate(1.05f);
                ttsReady = true;
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Versión de descarga directa: busca actualizaciones (como máximo cada 3 horas).
        Updater.check(this, false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    private void handleBack() {
        web.evaluateJavascript("window.AVDBack ? window.AVDBack() : false", value -> {
            if (!"true".equals(value)) finish();
        });
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        handleBack();
    }

    @Override
    protected void onDestroy() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        if (web != null) web.destroy();
        super.onDestroy();
    }

    private Vibrator vibrator() {
        if (Build.VERSION.SDK_INT >= 31) {
            VibratorManager vm = (VibratorManager) getSystemService(Context.VIBRATOR_MANAGER_SERVICE);
            return vm != null ? vm.getDefaultVibrator() : null;
        }
        return (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    private class Bridge {
        @JavascriptInterface
        public void speak(String text) {
            if (ttsReady && text != null) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "avd");
        }

        @JavascriptInterface
        public void stopSpeak() {
            if (ttsReady) tts.stop();
        }

        @JavascriptInterface
        public void vibrate(String json) {
            try {
                JSONArray a = new JSONArray(json);
                long[] timings = new long[a.length() + 1];
                timings[0] = 0;
                for (int i = 0; i < a.length(); i++) timings[i + 1] = a.getLong(i);
                Vibrator v = vibrator();
                if (v != null && v.hasVibrator()) v.vibrate(VibrationEffect.createWaveform(timings, -1));
            } catch (Exception ignored) { }
        }

        @JavascriptInterface
        public void keepAwake(boolean on) {
            runOnUiThread(() -> {
                if (on) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            });
        }

        @JavascriptInterface
        public String version() {
            return BuildConfig.VERSION_NAME;
        }

        @JavascriptInterface
        public boolean hasUpdater() {
            return Updater.ENABLED;
        }

        @JavascriptInterface
        public void checkUpdate() {
            runOnUiThread(() -> Updater.check(MainActivity.this, true));
        }

        @JavascriptInterface
        public void copy(String text) {
            runOnUiThread(() -> {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("AVD time", text));
            });
        }
    }
}
