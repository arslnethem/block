
package com.blockblast.aisolver;

import android.app.Service;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import org.json.JSONArray;

public class OverlayService extends Service {

    public static final String ACTION_SHOW = "SHOW_OVERLAY";
    public static final String ACTION_HIDE = "HIDE_OVERLAY";

    private WindowManager windowManager;
    private OverlayView overlayView;
    private WindowManager.LayoutParams params;

    private float boardX = 0.08f;
    private float boardY = 0.24f;
    private float boardW = 0.84f;
    private float boardH = 0.47f;

    private JSONArray cells = new JSONArray();

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_NOT_STICKY;

        String action = intent.getAction();

        if (ACTION_HIDE.equals(action)) {
            removeOverlay();
            stopSelf();
            return START_NOT_STICKY;
        }

        if (ACTION_SHOW.equals(action)) {
            boardX = intent.getFloatExtra("boardX", 0.08f);
            boardY = intent.getFloatExtra("boardY", 0.24f);
            boardW = intent.getFloatExtra("boardW", 0.84f);
            boardH = intent.getFloatExtra("boardH", 0.47f);

            String data = intent.getStringExtra("cells");
            try {
                cells = data == null ? new JSONArray() : new JSONArray(data);
            } catch (Exception e) {
                cells = new JSONArray();
            }

            showOverlay();
        }

        return START_NOT_STICKY;
    }

    private void showOverlay() {
        if (Build.VERSION.SDK_INT >= 23 &&
            !android.provider.Settings.canDrawOverlays(this)) {
            stopSelf();
            return;
        }

        if (overlayView == null) {
            overlayView = new OverlayView();

            int overlayType = Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;

            params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                    | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            );

            params.gravity = Gravity.TOP | Gravity.START;

            try {
                windowManager.addView(overlayView, params);
            } catch (Exception e) {
                overlayView = null;
                stopSelf();
                return;
            }
        }

        overlayView.invalidate();
    }

    private void removeOverlay() {
        if (overlayView != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {
            }
            overlayView = null;
        }
    }

    private class OverlayView extends View {

        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        OverlayView() {
            super(OverlayService.this);
            setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);

            float cellW = boardW * getWidth() / 8f;
            float cellH = boardH * getHeight() / 8f;
            float left = boardX * getWidth();
            float top = boardY * getHeight();

            for (int i = 0; i < cells.length(); i++) {
                try {
                    JSONArray cell = cells.getJSONArray(i);

                    int row = cell.getInt(0);
                    int col = cell.getInt(1);
                    int step = cell.getInt(2);

                    int color;
                    if (step == 1) {
                        color = Color.rgb(56, 189, 248);
                    } else if (step == 2) {
                        color = Color.rgb(34, 197, 94);
                    } else {
                        color = Color.rgb(245, 158, 11);
                    }

                    paint.setColor(color);
                    paint.setAlpha(115);
                    paint.setStyle(Paint.Style.FILL);

                    float x = left + col * cellW;
                    float y = top + row * cellH;

                    canvas.drawRoundRect(
                        x + 2, y + 2,
                        x + cellW - 2, y + cellH - 2,
                        6, 6, paint
                    );

                    paint.setColor(Color.WHITE);
                    paint.setAlpha(220);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setStrokeWidth(2);

                    canvas.drawRoundRect(
                        x + 2, y + 2,
                        x + cellW - 2, y + cellH - 2,
                        6, 6, paint
                    );

                    paint.setStyle(Paint.Style.FILL);

                } catch (Exception ignored) {
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
