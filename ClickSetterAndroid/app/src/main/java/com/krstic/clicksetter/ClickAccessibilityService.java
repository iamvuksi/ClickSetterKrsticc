package com.krstic.clicksetter;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class ClickAccessibilityService extends AccessibilityService {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager windowManager;
    private View timerPanel;
    private TextView countdownView;
    private Button stopButton;
    private boolean receiverRegistered;
    private boolean running;
    private int targetX;
    private int targetY;
    private long intervalMs;
    private long nextClickAt;

    private final Runnable clickRunnable = new Runnable() {
        @Override public void run() {
            if (!running) return;
            performTap(targetX, targetY);
            nextClickAt = SystemClock.elapsedRealtime() + intervalMs;
            handler.postDelayed(this, intervalMs);
        }
    };

    private final Runnable countdownRunnable = new Runnable() {
        @Override public void run() {
            if (!running) return;
            long leftMs = Math.max(0L, nextClickAt - SystemClock.elapsedRealtime());
            long leftSeconds = (leftMs + 999L) / 1000L;
            long minutes = leftSeconds / 60L;
            long seconds = leftSeconds % 60L;
            if (countdownView != null) {
                countdownView.setText(String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds));
            }
            handler.postDelayed(this, 200L);
        }
    };

    private final Runnable hidePanelRunnable = this::removeTimerPanel;

    private final BroadcastReceiver commandReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) return;
            if (MainActivity.ACTION_START.equals(intent.getAction())) {
                startTimer(intent.getLongExtra("interval_ms", 10000L));
            } else if (MainActivity.ACTION_STOP.equals(intent.getAction())) {
                stopTimer();
            }
        }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(MainActivity.ACTION_START);
        filter.addAction(MainActivity.ACTION_STOP);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(commandReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(commandReceiver, filter);
        }
        receiverRegistered = true;
    }

    private void startTimer(long delayMs) {
        if (!SettingsOverlayAllowed()) return;
        if (delayMs <= 0L) delayMs = 10000L;
        stopTimerInternal(false);
        boolean hasPoint = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
                .getBoolean(MainActivity.PREF_HAS_POINT, false);
        if (!hasPoint) return;
        targetX = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).getInt(MainActivity.PREF_X, 0);
        targetY = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE).getInt(MainActivity.PREF_Y, 0);
        intervalMs = delayMs;
        nextClickAt = SystemClock.elapsedRealtime() + intervalMs;
        running = true;
        showTimerPanel();
        handler.postDelayed(clickRunnable, intervalMs);
        handler.post(countdownRunnable);
    }

    private boolean SettingsOverlayAllowed() {
        return Build.VERSION.SDK_INT < 23 || android.provider.Settings.canDrawOverlays(this);
    }

    private void performTap(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke = new GestureDescription.StrokeDescription(path, 0L, 70L);
        GestureDescription gesture = new GestureDescription.Builder().addStroke(stroke).build();
        dispatchGesture(gesture, null, handler);
    }

    private void stopTimer() {
        stopTimerInternal(true);
    }

    private void stopTimerInternal(boolean showStoppedState) {
        running = false;
        handler.removeCallbacks(clickRunnable);
        handler.removeCallbacks(countdownRunnable);
        handler.removeCallbacks(hidePanelRunnable);
        if (showStoppedState && timerPanel != null) {
            if (countdownView != null) countdownView.setText("STOPPED");
            if (stopButton != null) stopButton.setVisibility(View.GONE);
            handler.postDelayed(hidePanelRunnable, 2500L);
        } else if (timerPanel != null) {
            removeTimerPanel();
        }
    }

    private void showTimerPanel() {
        handler.removeCallbacks(hidePanelRunnable);
        if (timerPanel != null) removeTimerPanel();
        if (windowManager == null) windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.HORIZONTAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(dp(13), dp(9), dp(9), dp(9));
        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.rgb(23, 59, 108));
        background.setCornerRadius(dp(18));
        panel.setBackground(background);

        LinearLayout textStack = new LinearLayout(this);
        textStack.setOrientation(LinearLayout.VERTICAL);
        textStack.setGravity(Gravity.CENTER_VERTICAL);
        TextView heading = new TextView(this);
        heading.setText("NEXT CLICK IN");
        heading.setTextSize(10);
        heading.setTextColor(Color.rgb(217, 231, 245));
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textStack.addView(heading);
        countdownView = new TextView(this);
        countdownView.setText("--:--");
        countdownView.setTextSize(22);
        countdownView.setTextColor(Color.WHITE);
        countdownView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        textStack.addView(countdownView);
        panel.addView(textStack, new LinearLayout.LayoutParams(0, dp(60), 1f));

        stopButton = new Button(this);
        stopButton.setText("STOP");
        stopButton.setAllCaps(false);
        stopButton.setTextSize(12);
        stopButton.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        stopButton.setTextColor(Color.rgb(23, 59, 108));
        GradientDrawable stopBackground = new GradientDrawable();
        stopBackground.setColor(Color.WHITE);
        stopBackground.setCornerRadius(dp(12));
        stopButton.setBackground(stopBackground);
        stopButton.setMinWidth(dp(66));
        stopButton.setMinHeight(dp(44));
        stopButton.setOnClickListener(v -> stopTimer());
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(dp(68), dp(44));
        stopParams.leftMargin = dp(8);
        panel.addView(stopButton, stopParams);

        timerPanel = panel;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                dp(210), dp(82),
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        int screenWidth = getResources().getDisplayMetrics().widthPixels;
        int screenHeight = getResources().getDisplayMetrics().heightPixels;
        int panelWidth = dp(210);
        int panelHeight = dp(82);
        int margin = dp(12);
        int upper = dp(72);
        int lower = Math.max(upper, screenHeight - panelHeight - upper);
        int[][] corners = {
                {margin, upper},
                {Math.max(margin, screenWidth - panelWidth - margin), upper},
                {margin, lower},
                {Math.max(margin, screenWidth - panelWidth - margin), lower}
        };
        int chosen = 0;
        long farthest = Long.MIN_VALUE;
        for (int i = 0; i < corners.length; i++) {
            int left = corners[i][0];
            int top = corners[i][1];
            boolean coversTarget = targetX >= left && targetX <= left + panelWidth
                    && targetY >= top && targetY <= top + panelHeight;
            long dx = targetX - (left + panelWidth / 2);
            long dy = targetY - (top + panelHeight / 2);
            long distance = dx * dx + dy * dy;
            if (!coversTarget) {
                chosen = i;
                break;
            }
            if (distance > farthest) {
                farthest = distance;
                chosen = i;
            }
        }
        boolean placeLeft = corners[chosen][0] < screenWidth / 2;
        boolean placeTop = corners[chosen][1] < screenHeight / 2;
        params.gravity = (placeTop ? Gravity.TOP : Gravity.BOTTOM)
                | (placeLeft ? Gravity.START : Gravity.END);
        params.x = margin;
        params.y = upper;
        try {
            windowManager.addView(timerPanel, params);
        } catch (RuntimeException ex) {
            timerPanel = null;
            countdownView = null;
            stopButton = null;
        }
    }

    private void removeTimerPanel() {
        handler.removeCallbacks(hidePanelRunnable);
        if (timerPanel != null && windowManager != null) {
            try {
                windowManager.removeView(timerPanel);
            } catch (RuntimeException ignored) { }
        }
        timerPanel = null;
        countdownView = null;
        stopButton = null;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) { }

    @Override public void onInterrupt() {
        stopTimer();
    }

    @Override public void onDestroy() {
        stopTimerInternal(false);
        if (receiverRegistered) {
            try { unregisterReceiver(commandReceiver); } catch (IllegalArgumentException ignored) { }
            receiverRegistered = false;
        }
        super.onDestroy();
    }
}
