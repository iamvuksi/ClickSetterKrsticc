package com.krstic.clicksetter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

final class PointPickerOverlay {
    interface OnPointPicked { void onPointPicked(int x, int y); default void onCancelled() { } }

    private PointPickerOverlay() { }

    static void show(Activity activity, OnPointPicked callback) {
        if (!android.provider.Settings.canDrawOverlays(activity)) {
            Toast.makeText(activity, "Allow display over other apps first.", Toast.LENGTH_SHORT).show();
            return;
        }
        WindowManager manager = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
        FrameLayout root = new FrameLayout(activity);
        root.setBackgroundColor(Color.TRANSPARENT);
        Crosshair marker = new Crosshair(activity);
        int markerSize = dp(activity, 48);
        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(markerSize, markerSize);
        markerParams.gravity = Gravity.TOP | Gravity.START;
        root.addView(marker, markerParams);
        marker.setVisibility(View.INVISIBLE);

        TextView instruction = new TextView(activity);
        instruction.setText("TAP THE EXACT TARGET POINT");
        instruction.setTextColor(Color.WHITE);
        instruction.setTextSize(12);
        instruction.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        instruction.setGravity(Gravity.CENTER);
        instruction.setPadding(dp(activity, 16), dp(activity, 12), dp(activity, 16), dp(activity, 12));
        GradientDrawable banner = new GradientDrawable();
        banner.setColor(0xE6173B6C);
        banner.setCornerRadius(dp(activity, 14));
        instruction.setBackground(banner);
        instruction.setClickable(false);
        FrameLayout.LayoutParams instructionParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        instructionParams.topMargin = dp(activity, 32);
        root.addView(instruction, instructionParams);

        TextView cancel = new TextView(activity);
        cancel.setText("CANCEL");
        cancel.setTextColor(Color.WHITE);
        cancel.setTextSize(13);
        cancel.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        cancel.setGravity(Gravity.CENTER);
        cancel.setPadding(dp(activity, 22), dp(activity, 13), dp(activity, 22), dp(activity, 13));
        GradientDrawable cancelBackground = new GradientDrawable();
        cancelBackground.setColor(0xE6173B6C);
        cancelBackground.setCornerRadius(dp(activity, 18));
        cancel.setBackground(cancelBackground);
        cancel.setClickable(false);
        FrameLayout.LayoutParams cancelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
        cancelParams.bottomMargin = dp(activity, 34);
        root.addView(cancel, cancelParams);

        WindowManager.LayoutParams windowParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        windowParams.gravity = Gravity.TOP | Gravity.START;
        try {
            manager.addView(root, windowParams);
        } catch (RuntimeException ex) {
            Toast.makeText(activity, "Could not open point selection overlay.", Toast.LENGTH_LONG).show();
            return;
        }

        root.setOnTouchListener((view, event) -> {
            float rawX = event.getRawX();
            float rawY = event.getRawY();
            marker.setVisibility(View.VISIBLE);
            marker.setX(rawX - markerSize / 2f);
            marker.setY(rawY - markerSize / 2f);
            if (event.getAction() == MotionEvent.ACTION_UP) {
                int[] cancelLocation = new int[2];
                cancel.getLocationOnScreen(cancelLocation);
                if (rawX >= cancelLocation[0] && rawX <= cancelLocation[0] + cancel.getWidth()
                        && rawY >= cancelLocation[1] && rawY <= cancelLocation[1] + cancel.getHeight()) {
                    try { manager.removeView(root); } catch (RuntimeException ignored) { }
                    callback.onCancelled();
                    Intent reopen = new Intent(activity, MainActivity.class);
                    reopen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    activity.startActivity(reopen);
                    return true;
                }
                int x = Math.round(rawX);
                int y = Math.round(rawY);
                try { manager.removeView(root); } catch (RuntimeException ignored) { }
                callback.onPointPicked(x, y);
                return true;
            }
            return true;
        });
    }

    static void showPreview(Context context, int x, int y) {
        if (!android.provider.Settings.canDrawOverlays(context)) return;
        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Color.TRANSPARENT);
        int markerSize = dp(context, 52);
        Crosshair marker = new Crosshair(context);
        FrameLayout.LayoutParams markerParams = new FrameLayout.LayoutParams(markerSize, markerSize);
        markerParams.gravity = Gravity.TOP | Gravity.START;
        root.addView(marker, markerParams);
        marker.setX(x - markerSize / 2f);
        marker.setY(y - markerSize / 2f);

        TextView label = new TextView(context);
        label.setText("SAVED POINT  ·  X " + x + "  Y " + y);
        label.setTextColor(Color.WHITE);
        label.setTextSize(12);
        label.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        label.setGravity(Gravity.CENTER);
        label.setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10));
        GradientDrawable banner = new GradientDrawable();
        banner.setColor(0xE6173B6C);
        banner.setCornerRadius(dp(context, 14));
        label.setBackground(banner);
        FrameLayout.LayoutParams labelParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP | Gravity.CENTER_HORIZONTAL);
        labelParams.topMargin = dp(context, 32);
        root.addView(label, labelParams);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        try {
            manager.addView(root, params);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                try { manager.removeView(root); } catch (RuntimeException ignored) { }
            }, 1400L);
        } catch (RuntimeException ignored) { }
    }

    private static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }

    private static final class Crosshair extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Crosshair(Context context) { super(context); }

        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float radius = Math.min(cx, cy) - dp(getContext(), 5);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(dp(getContext(), 2));
            paint.setColor(Color.WHITE);
            canvas.drawCircle(cx, cy, radius, paint);
            paint.setColor(Color.rgb(235, 63, 76));
            paint.setStrokeWidth(dp(getContext(), 2));
            canvas.drawLine(cx - radius, cy, cx + radius, cy, paint);
            canvas.drawLine(cx, cy - radius, cx, cy + radius, paint);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle(cx, cy, dp(getContext(), 3), paint);
        }
    }
}
