package com.satex.promptvault;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.view.View;
import android.graphics.Color;
import android.os.Build;
import android.view.WindowInsets;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final int PICK_FILE = 1001;
    private static final int SAVE_FILE = 1002;
    private ValueCallback<Uri[]> fileCallback;
    private String pendingBackup = "";
    private WebView webView;
    private LinearLayout rootLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webView = new WebView(this);

        // Match BandWatch's proven layout: apply system-bar insets to a root
        // container, not to the WebView itself. This keeps both the top content
        // and bottom app controls clear of Android's status/navigation bars.
        rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.WHITE);

        rootLayout.setOnApplyWindowInsetsListener((view, insets) -> {
            int topInset;
            int bottomInset;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
                topInset = bars.top;
                bottomInset = bars.bottom;
            } else {
                topInset = insets.getSystemWindowInsetTop();
                bottomInset = insets.getSystemWindowInsetBottom();
            }
            view.setPadding(0, topInset, 0, bottomInset);
            return insets;
        });

        rootLayout.addView(webView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
        setContentView(rootLayout);
        applyNativeTheme("light");
        webView.setBackgroundColor(Color.TRANSPARENT);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportMultipleWindows(false);

        webView.addJavascriptInterface(new AppBridge(), "AndroidBridge");
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/json", "text/json", "application/octet-stream"});
                startActivityForResult(intent, PICK_FILE);
                return true;
            }
        });

        webView.loadUrl("file:///android_asset/index.html");
    }

    private void applyNativeTheme(String theme) {
        boolean dark = "dark".equals(theme);
        int background = Color.parseColor(dark ? "#0b1120" : "#f6f7fb");
        rootLayout.setBackgroundColor(background);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            android.view.WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                int appearance = dark ? 0 : android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS;
                controller.setSystemBarsAppearance(
                        appearance,
                        android.view.WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                );
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            View decor = getWindow().getDecorView();
            int flags = decor.getSystemUiVisibility();
            if (dark) flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            else flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            decor.setSystemUiVisibility(flags);
        }
    }

    private class AppBridge {
        @JavascriptInterface
        public void setTheme(String theme) {
            getSharedPreferences("promptvault_native", MODE_PRIVATE)
                    .edit()
                    .putString("theme", theme)
                    .apply();
            runOnUiThread(() -> applyNativeTheme(theme));
        }

        @JavascriptInterface
        public void copyText(String text) {
            runOnUiThread(() -> {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText("PromptVault prompt", text));
                Toast.makeText(MainActivity.this, "Copied to clipboard", Toast.LENGTH_SHORT).show();
            });
        }

        @JavascriptInterface
        public void syncPrompts(String json) {
            if (json == null) return;
            getSharedPreferences("promptvault_native", MODE_PRIVATE)
                    .edit()
                    .putString("prompts_json", json)
                    .apply();
        }

        @JavascriptInterface
        public void saveBackup(String json) {
            pendingBackup = json;
            runOnUiThread(() -> {
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, "promptvault-backup.json");
                startActivityForResult(intent, SAVE_FILE);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_FILE && fileCallback != null) {
            Uri[] result = resultCode == RESULT_OK && data != null && data.getData() != null
                    ? new Uri[]{data.getData()} : null;
            fileCallback.onReceiveValue(result);
            fileCallback = null;
        } else if (requestCode == SAVE_FILE && resultCode == RESULT_OK && data != null) {
            try (OutputStream output = getContentResolver().openOutputStream(data.getData())) {
                if (output != null) output.write(pendingBackup.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(this, "Backup saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Could not save backup", Toast.LENGTH_LONG).show();
            }
            pendingBackup = "";
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
