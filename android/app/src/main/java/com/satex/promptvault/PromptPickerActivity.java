package com.satex.promptvault;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;\nimport android.os.SystemClock;\nimport android.util.Log;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class PromptPickerActivity extends Activity {\n    private static final String TAG = "PromptVaultTiming";
    private final List<PromptItem> allPrompts = new ArrayList<>();
    private LinearLayout listContainer;
    private EditText searchInput;
    private boolean dark;
    private int background, surface, text, muted, accent, border;

    private static class PromptItem {
        String title, category, content;
        boolean favorite;
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        SharedPreferences prefs = getSharedPreferences("promptvault_native", MODE_PRIVATE);
        dark = "dark".equals(prefs.getString("theme", "light"));\n        Log.i(TAG, "PICKER_PREFS_READ after " + (SystemClock.elapsedRealtime() - startedAt) + " ms");
        // Theme is also stored in WebView localStorage; the native bridge keeps
        // a copy so this screen can render before the main WebView is opened.
        configureColors();
        configureSystemBars();
        readPrompts(prefs.getString("prompts_json", "[]"));
        buildScreen();
        renderPrompts("");
    }

    @Override\n    protected void onResume() {\n        super.onResume();\n        Log.i(TAG, "PICKER_ON_RESUME");\n    }\n\n    private void configureColors() {
        background = Color.parseColor(dark ? "#0b1120" : "#f6f7fb");
        surface = Color.parseColor(dark ? "#121c2d" : "#ffffff");
        text = Color.parseColor(dark ? "#edf2fa" : "#182230");
        muted = Color.parseColor(dark ? "#9aa8bd" : "#778294");
        accent = Color.parseColor(dark ? "#aaa4ff" : "#635bdb");
        border = Color.parseColor(dark ? "#26344a" : "#e8ebf1");
    }

    private void configureSystemBars() {
        Window window = getWindow();
        window.setStatusBarColor(background);
        window.setNavigationBarColor(background);
        if (android.os.Build.VERSION.SDK_INT >= 23) {
            View decor = window.getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (!dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                if (!dark) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
                else flags &= ~View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    private void readPrompts(String json) {
        try {
            JSONArray array = new JSONArray(json);
            for (int i = 0; i < array.length(); i++) {
                JSONObject object = array.optJSONObject(i);
                if (object == null) continue;
                String title = object.optString("title", "").trim();
                String content = object.optString("content", "");
                if (title.isEmpty() || content.trim().isEmpty()) continue;
                PromptItem item = new PromptItem();
                item.title = title;
                item.content = content;
                item.category = object.optString("category", "Other");
                item.favorite = object.optBoolean("favorite", false);
                allPrompts.add(item);
            }
        } catch (Exception ignored) {
            allPrompts.clear();
        }
        Collections.sort(allPrompts, new Comparator<PromptItem>() {
            @Override public int compare(PromptItem a, PromptItem b) {
                if (a.favorite != b.favorite) return a.favorite ? -1 : 1;
                return a.title.compareToIgnoreCase(b.title);
            }
        });
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(16), dp(20), dp(12));
        root.setBackgroundColor(background);

        TextView title = new TextView(this);
        title.setText("Choose a prompt");
        title.setTextColor(text);
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Tap any prompt to copy it to your clipboard.");
        subtitle.setTextColor(muted);
        subtitle.setTextSize(13);
        LinearLayout.LayoutParams subParams = new LinearLayout.LayoutParams(-1, -2);
        subParams.topMargin = dp(5);
        subParams.bottomMargin = dp(16);
        root.addView(subtitle, subParams);

        searchInput = new EditText(this);
        searchInput.setSingleLine(true);
        searchInput.setTextSize(14);
        searchInput.setHint("Search your prompts");
        searchInput.setHintTextColor(muted);
        searchInput.setTextColor(text);
        searchInput.setPadding(dp(14), 0, dp(14), 0);
        searchInput.setBackground(roundRect(surface, border, 10));
        root.addView(searchInput, new LinearLayout.LayoutParams(-1, dp(48)));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, 0, 1);
        scrollParams.topMargin = dp(14);
        root.addView(scroll, scrollParams);

        listContainer = new LinearLayout(this);
        listContainer.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(listContainer);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderPrompts(s.toString());
            }
            @Override public void afterTextChanged(Editable e) {}
        });

        setContentView(root);
    }

    private void renderPrompts(String query) {
        listContainer.removeAllViews();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        int shown = 0;
        for (final PromptItem item : allPrompts) {
            if (!item.title.toLowerCase(Locale.ROOT).contains(q)
                    && !item.content.toLowerCase(Locale.ROOT).contains(q)
                    && !item.category.toLowerCase(Locale.ROOT).contains(q)) continue;

            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(15), dp(13), dp(15), dp(13));
            card.setBackground(roundRect(surface, border, 11));
            card.setClickable(true);
            card.setFocusable(true);
            card.setOnClickListener(v -> copyPrompt(item));

            TextView name = new TextView(this);
            name.setText((item.favorite ? "★  " : "") + item.title);
            name.setTextColor(text);
            name.setTextSize(15);
            name.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            card.addView(name);

            TextView detail = new TextView(this);
            detail.setText(item.category + "  ·  " + preview(item.content));
            detail.setTextColor(muted);
            detail.setTextSize(12);
            detail.setMaxLines(2);
            LinearLayout.LayoutParams detailParams = new LinearLayout.LayoutParams(-1, -2);
            detailParams.topMargin = dp(6);
            card.addView(detail, detailParams);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2);
            cardParams.bottomMargin = dp(9);
            listContainer.addView(card, cardParams);
            shown++;
        }
        if (shown == 0) {
            TextView empty = new TextView(this);
            empty.setText(allPrompts.isEmpty()
                    ? "No saved prompts yet. Open PromptVault and add one first."
                    : "No prompts match your search.");
            empty.setTextColor(muted);
            empty.setTextSize(14);
            empty.setPadding(dp(8), dp(24), dp(8), dp(24));
            listContainer.addView(empty);
        }
    }

    private void copyPrompt(PromptItem item) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("PromptVault prompt", item.content));
        Toast.makeText(this, "Copied: " + item.title, Toast.LENGTH_SHORT).show();
    }

    private String preview(String content) {
        String value = content.replaceAll("\\s+", " ").trim();
        return value.length() > 100 ? value.substring(0, 100) + "…" : value;
    }

    private GradientDrawable roundRect(int color, int stroke, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        drawable.setStroke(dp(1), stroke);
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
