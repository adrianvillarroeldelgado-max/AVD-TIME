package com.avd.time;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Build;
import android.widget.Toast;

/** Recibe el estado de la instalación y abre la confirmación de Android cuando hace falta. */
public class UpdateReceiver extends BroadcastReceiver {
    @Override
    @SuppressWarnings("deprecation")
    public void onReceive(Context ctx, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            Intent confirm = Build.VERSION.SDK_INT >= 33
                    ? intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent.class)
                    : (Intent) intent.getParcelableExtra(Intent.EXTRA_INTENT);
            if (confirm != null) {
                confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                try { ctx.startActivity(confirm); } catch (Exception ignored) { }
            }
        } else if (status != PackageInstaller.STATUS_SUCCESS) {
            String msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
            Toast.makeText(ctx, "No se pudo instalar la actualización. " + (msg != null ? msg : ""), Toast.LENGTH_LONG).show();
        }
    }
}
