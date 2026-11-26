
package com.blockblast.aisolver;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

public class ScreenCaptureService extends Service {

    private static final String CHANNEL_ID = "block_blast_capture";
    private MediaProjection projection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private HandlerThread handlerThread;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Block Blast ekran okuyucu")
            .setContentText("Ekran paylaşımı etkin")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build();

        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(
                1001,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            );
        } else {
            startForeground(1001, notification);
        }

        int resultCode = intent.getIntExtra("resultCode", 0);
        Intent resultData = getIntentExtra(intent);

        if (resultData == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        MediaProjectionManager manager =
            (MediaProjectionManager) getSystemService(
                MEDIA_PROJECTION_SERVICE
            );

        projection = manager.getMediaProjection(resultCode, resultData);

        startCapture();
        return START_NOT_STICKY;
    }

    private Intent getIntentExtra(Intent intent) {
        if (Build.VERSION.SDK_INT >= 33) {
            return intent.getParcelableExtra("resultData", Intent.class);
        }
        return intent.getParcelableExtra("resultData");
    }

    private void startCapture() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        if (Build.VERSION.SDK_INT >= 30) {
            android.view.Display display = wm.getDefaultDisplay();
            display.getRealMetrics(metrics);
        } else {
            wm.getDefaultDisplay().getRealMetrics(metrics);
        }

        int width = metrics.widthPixels;
        int height = metrics.heightPixels;
        int density = metrics.densityDpi;

        handlerThread = new HandlerThread("BlockBlastCapture");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());

        imageReader = ImageReader.newInstance(
            width, height, PixelFormat.RGBA_8888, 2
        );

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;

            try {
                image = reader.acquireLatestImage();
                if (image == null) return;

                Image.Plane plane = image.getPlanes()[0];
                ByteBuffer buffer = plane.getBuffer();

                int pixelStride = plane.getPixelStride();
                int rowStride = plane.getRowStride();
                int rowPadding = rowStride - pixelStride * width;

                Bitmap bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                );

                bitmap.copyPixelsFromBuffer(buffer);

                Bitmap cropped = Bitmap.createBitmap(
                    bitmap, 0, 0, width, height
                );

                ByteArrayOutputStream output = new ByteArrayOutputStream();
                cropped.compress(Bitmap.CompressFormat.JPEG, 65, output);

                String encoded = Base64.encodeToString(
                    output.toByteArray(), Base64.NO_WRAP
                );

                ScreenCapturePlugin.sendFrame(encoded);

                bitmap.recycle();
                cropped.recycle();

            } catch (Exception ignored) {
            } finally {
                if (image != null) image.close();
            }
        }, handler);

        virtualDisplay = projection.createVirtualDisplay(
            "BlockBlastScreen",
            width,
            height,
            density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.getSurface(),
            null,
            handler
        );

        projection.registerCallback(new MediaProjection.Callback() {
            @Override
            public void onStop() {
                stopCapture();
                stopSelf();
            }
        }, handler);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Ekran yakalama",
                NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                getSystemService(NotificationManager.class);

            manager.createNotificationChannel(channel);
        }
    }

    private void stopCapture() {
        if (virtualDisplay != null) {
            virtualDisplay.release();
            virtualDisplay = null;
        }

        if (imageReader != null) {
            imageReader.close();
            imageReader = null;
        }

        if (handlerThread != null) {
            handlerThread.quitSafely();
            handlerThread = null;
        }

        if (projection != null) {
            projection.stop();
            projection = null;
        }
    }

    @Override
    public void onDestroy() {
        stopCapture();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
