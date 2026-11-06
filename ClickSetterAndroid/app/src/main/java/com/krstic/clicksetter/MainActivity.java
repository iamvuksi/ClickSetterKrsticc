package com.krstic.clicksetter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.content.pm.ServiceInfo;
import android.text.TextUtils;

public class MainActivity extends Activity {
    static final String ACTION_START = "com.krstic.clicksetter.START";
    static final String ACTION_STOP = "com.krstic.clicksetter.STOP";
    static final String PREFS = "click_setter_prefs";
    static final String PREF_X = "target_x";
    static final String PREF_Y = "target_y";
    static final String PREF_HAS_POINT = "has_point";

    private TextView pointText;
    private TextView statusText;
    private EditText intervalInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(243, 246, 250));
        getWindow().setNavigationBarColor(Color.rgb(243, 246, 250));
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        buildScreen();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePointText();
    }

    private void buildScreen() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.rgb(243, 246, 250));
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(22), dp(20), dp(22), dp(26));
        scroll.addView(page);

        TextView title = text("CLICK SETTER", 27, Color.rgb(23, 59, 108), true);
        page.addView(title, matchWrap());
        TextView byline = text("FORTNITE AUTO PLAY  ·  DESIGNED BY KRSTIC", 11, Color.rgb(99, 113, 132), true);
        LinearLayout.LayoutParams bylineParams = matchWrap();
        bylineParams.topMargin = dp(4);
        page.addView(byline, bylineParams);

        addSectionTitle(page, "TARGET POINT", dp(26));
        pointText = text("Not set", 16, Color.rgb(36, 50, 71), true);
        pointText.setPadding(dp(14), dp(12), dp(14), dp(12));
        pointText.setBackground(rounded(Color.WHITE, 12, Color.rgb(221, 228, 237)));
        page.addView(pointText, matchWrap());

        Button pickButton = button("Set Point", true);
        LinearLayout.LayoutParams pickParams = matchWrap();
        pickParams.topMargin = dp(10);
        page.addView(pickButton, pickParams);
        pickButton.setOnClickListener(v -> beginPointCapture());

        Button previewButton = button("Preview Point", false);
        LinearLayout.LayoutParams previewParams = matchWrap();
        previewParams.topMargin = dp(8);
        page.addView(previewButton, previewParams);
        previewButton.setOnClickListener(v -> previewPoint());

        addSectionTitle(page, "CLICK SCHEDULE", dp(24));
        TextView intervalLabel = text("Repeat every (seconds)", 14, Color.rgb(36, 50, 71), true);
        page.addView(intervalLabel, matchWrap());
        intervalInput = new EditText(this);
        intervalInput.setSingleLine(true);
        intervalInput.setText("10");
        intervalInput.setTextSize(18);
        intervalInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        intervalInput.setPadding(dp(14), dp(8), dp(14), dp(8));
        intervalInput.setBackground(rounded(Color.WHITE, 10, Color.rgb(221, 228, 237)));
        LinearLayout.LayoutParams inputParams = matchWrap();
        inputParams.topMargin = dp(6);
        page.addView(intervalInput, inputParams);
        TextView hint = text("Enter a delay in seconds, such as 10 or 0.5.", 12, Color.rgb(99, 113, 132), false);
        LinearLayout.LayoutParams hintParams = matchWrap();
        hintParams.topMargin = dp(4);
        page.addView(hint, hintParams);

        Button startButton = button("Start", true);
        LinearLayout.LayoutParams startParams = matchWrap();
        startParams.topMargin = dp(20);
        page.addView(startButton, startParams);
        startButton.setOnClickListener(v -> startClicking());

        Button stopButton = button("Stop", false);
        LinearLayout.LayoutParams stopParams = matchWrap();
        stopParams.topMargin = dp(8);
        page.addView(stopButton, stopParams);
        stopButton.setOnClickListener(v -> stopClicking());

        statusText = text("Ready · Set a point and choose a delay.", 13, Color.rgb(36, 50, 71), false);
        statusText.setPadding(dp(12), dp(12), dp(12), dp(12));
        statusText.setBackground(rounded(Color.rgb(232, 239, 248), 10, Color.TRANSPARENT));
        LinearLayout.LayoutParams statusParams = matchWrap();
        statusParams.topMargin = dp(18);
        page.addView(statusText, statusParams);

        TextView accessInfo = text("For screen taps, enable Click Setter in Android Accessibility settings and allow display over other apps.", 12, Color.rgb(99, 113, 132), false);
        LinearLayout.LayoutParams infoParams = matchWrap();
        infoParams.topMargin = dp(12);
        page.addView(accessInfo, infoParams);

        setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getSystemWindowInsetTop(), 0, insets.getSystemWindowInsetBottom());
            return insets;
        });
        updatePointText();
    }

    private void beginPointCapture() {
        if (!Settings.canDrawOverlays(this)) {
            statusText.setText("Allow display over other apps, then tap Set Point again.");
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
            return;
        }
        statusText.setText("Tap the exact spot you want to save. The app returns here afterward.");
        PointPickerOverlay.show(this, (x, y) -> {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            prefs.edit().putInt(PREF_X, x).putInt(PREF_Y, y).putBoolean(PREF_HAS_POINT, true).apply();
            updatePointText();
            statusText.setText("Point saved at X " + x + ", Y " + y + ".");
            Intent reopen = new Intent(this, MainActivity.class);
            reopen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(reopen);
        });
    }

    private void previewPoint() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_HAS_POINT, false)) {
            statusText.setText("Set a point first.");
            return;
        }
        PointPickerOverlay.showPreview(this, prefs.getInt(PREF_X, 0), prefs.getInt(PREF_Y, 0));
        statusText.setText("Showing the saved screen point briefly.");
    }

    private void startClicking() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (!prefs.getBoolean(PREF_HAS_POINT, false)) {
            statusText.setText("Set a point first.");
            return;
        }
        if (!Settings.canDrawOverlays(this)) {
            statusText.setText("Allow display over other apps so the countdown and Stop control can appear.");
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())));
            return;
        }
        if (!isClickServiceEnabled()) {
            statusText.setText("Turn on Click Setter in Accessibility settings, then return and tap Start.");
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            return;
        }
        double seconds;
        try {
            seconds = Double.parseDouble(intervalInput.getText().toString().trim());
        } catch (NumberFormatException ex) {
            statusText.setText("Enter a valid delay in seconds.");
            return;
        }
        if (seconds <= 0 || seconds > 600000) {
            statusText.setText("Enter a delay greater than 0 and no more than 600,000 seconds.");
            return;
        }
        long intervalMs = Math.max(1L, Math.round(seconds * 1000.0));
        Intent command = new Intent(ACTION_START).setPackage(getPackageName());
        command.putExtra("interval_ms", intervalMs);
        sendBroadcast(command);
        statusText.setText("Running. The floating countdown includes a Stop button.");
    }

    private void stopClicking() {
        sendBroadcast(new Intent(ACTION_STOP).setPackage(getPackageName()));
        statusText.setText("Stop requested.");
    }

    private boolean isClickServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) return false;
        String expected = new ComponentName(this, ClickAccessibilityService.class).flattenToString();
        for (String component : enabled.split(":")) {
            if (expected.equalsIgnoreCase(component)) return true;
        }
        return false;
    }

    private void updatePointText() {
        if (pointText == null) return;
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        if (prefs.getBoolean(PREF_HAS_POINT, false)) {
            pointText.setText("X " + prefs.getInt(PREF_X, 0) + "     Y " + prefs.getInt(PREF_Y, 0));
        } else {
            pointText.setText("Not set");
        }
    }

    private void addSectionTitle(LinearLayout parent, String label, int topMargin) {
        TextView section = text(label, 13, Color.rgb(23, 59, 108), true);
        LinearLayout.LayoutParams params = matchWrap();
        params.topMargin = topMargin;
        params.bottomMargin = dp(8);
        parent.addView(section, params);
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private Button button(String label, boolean primary) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(15);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setTextColor(primary ? Color.WHITE : Color.rgb(23, 59, 108));
        button.setBackground(rounded(primary ? Color.rgb(36, 99, 166) : Color.WHITE, 12,
                primary ? Color.TRANSPARENT : Color.rgb(207, 219, 234)));
        button.setMinHeight(dp(48));
        return button;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(fill);
        drawable.setCornerRadius(dp(radiusDp));
        if (stroke != Color.TRANSPARENT) drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
