
package com.blockblast.aisolver;

import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "Overlay")
public class OverlayPlugin extends Plugin {

    @PluginMethod
    public void requestPermission(PluginCall call) {
        if (Settings.canDrawOverlays(getContext())) {
            call.resolve();
            return;
        }

        Intent intent = new Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:" + getContext().getPackageName())
        );

        startActivityForResult(
            call,
            intent,
            "handleOverlayPermission"
        );
    }

    @ActivityCallback
    private void handleOverlayPermission(
        PluginCall call,
        ActivityResult result
    ) {
        if (Settings.canDrawOverlays(getContext())) {
            call.resolve();
        } else {
            call.reject("Diğer uygulamaların üzerinde gösterme izni verilmedi.");
        }
    }

    @PluginMethod
    public void show(PluginCall call) {
        if (!Settings.canDrawOverlays(getContext())) {
            call.reject("Önce hayalet ekran iznini vermelisiniz.");
            return;
        }

        String cells = call.getString("cells", "[]");

        Intent intent = new Intent(
            getContext(),
            OverlayService.class
        );

        intent.setAction(OverlayService.ACTION_SHOW);
        intent.putExtra("cells", cells);

        try {
            getContext().startService(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("Hayalet ekran başlatılamadı: " + e.getMessage());
        }
    }

    @PluginMethod
    public void hide(PluginCall call) {
        Intent intent = new Intent(
            getContext(),
            OverlayService.class
        );

        intent.setAction(OverlayService.ACTION_HIDE);

        try {
            getContext().startService(intent);
            call.resolve();
        } catch (Exception e) {
            call.reject("Hayalet ekran kapatılamadı: " + e.getMessage());
        }
    }
}
