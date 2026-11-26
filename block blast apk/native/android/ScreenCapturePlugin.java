
package com.blockblast.aisolver;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;

import androidx.activity.result.ActivityResult;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.ActivityCallback;
import com.getcapacitor.annotation.CapacitorPlugin;

@CapacitorPlugin(name = "ScreenCapture")
public class ScreenCapturePlugin extends Plugin {

    private static ScreenCapturePlugin instance;

    @Override
    public void load() {
        instance = this;
    }

    @PluginMethod
    public void start(PluginCall call) {
        MediaProjectionManager manager =
            (MediaProjectionManager) getContext()
                .getSystemService(android.content.Context.MEDIA_PROJECTION_SERVICE);

        startActivityForResult(
            call,
            manager.createScreenCaptureIntent(),
            "handlePermissionResult"
        );
    }

    @ActivityCallback
    private void handlePermissionResult(
        PluginCall call,
        ActivityResult result
    ) {
        if (result.getResultCode() != android.app.Activity.RESULT_OK
            || result.getData() == null) {
            call.reject("Ekran paylaşım izni verilmedi.");
            return;
        }

        Intent serviceIntent = new Intent(
            getContext(),
            ScreenCaptureService.class
        );

        serviceIntent.putExtra("resultCode", result.getResultCode());
        serviceIntent.putExtra("resultData", result.getData());

        try {
            androidx.core.content.ContextCompat.startForegroundService(
                getContext(),
                serviceIntent
            );

            JSObject response = new JSObject();
            response.put("started", true);
            call.resolve(response);

        } catch (Exception error) {
            call.reject("Ekran yakalama servisi başlatılamadı: "
                + error.getMessage());
        }
    }

    @PluginMethod
    public void stop(PluginCall call) {
        Intent intent = new Intent(
            getContext(),
            ScreenCaptureService.class
        );

        getContext().stopService(intent);
        call.resolve();
    }

    public static void sendFrame(String imageBase64) {
        if (instance == null) return;

        JSObject data = new JSObject();
        data.put("image", imageBase64);
        instance.notifyListeners("screenFrame", data);
    }
}
