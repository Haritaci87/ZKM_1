package com.zikirmatik.app;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.webkit.WebViewAssetLoader;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Zikirmatik — uygulamanın tamamı assets/www içindeki sayfadır.
 * Bu sınıf sayfayı açar ve telefona özgü işleri yapar:
 *  - Geri tuşu: önce açık pencereyi (sûre, sayaç vb.) kapatır, çıkmak için iki kez basılır
 *  - Yedek dosyası seçme (içe aktarma) ve kaydetme (dışa aktarma)
 *  - Namaz vakitleri için konum izni
 *  - Zikir çekerken ekranın kararmaması
 */
public class MainActivity extends Activity {

    // Dosyalar güvenli (https) bir adresten sunulur; konum izni ve kayıtlar bu sayede çalışır.
    private static final String APP_URL = "https://appassets.androidplatform.net/assets/www/index.html";
    private static final String APP_HOST = "appassets.androidplatform.net";

    private static final int REQ_FILE_CHOOSER = 1001;
    private static final int REQ_SAVE_BACKUP = 1002;
    private static final int REQ_LOCATION = 1003;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private GeolocationPermissions.Callback geoCallback;
    private String geoOrigin;
    private String pendingBackupContent;
    private long lastBackPress = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);
        webView.setBackgroundColor(0xFFF3ECD8);
        setContentView(webView);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);   // zikir kayıtları (localStorage) için
        settings.setGeolocationEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                if (APP_HOST.equals(uri.getHost())) return false;
                // Uygulama dışındaki bağlantılar tarayıcıda açılır
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, uri));
                } catch (ActivityNotFoundException ignored) {
                }
                return true;
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (filePathCallback != null) filePathCallback.onReceiveValue(null);
                filePathCallback = callback;
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(intent, "Yedek dosyasını seç"), REQ_FILE_CHOOSER);
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    return false;
                }
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false);
                    return;
                }
                geoOrigin = origin;
                geoCallback = callback;
                requestPermissions(new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }, REQ_LOCATION);
            }
        });

        webView.addJavascriptInterface(new Bridge(), "AndroidBridge");

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
        } else {
            webView.loadUrl(APP_URL);
        }
    }

    /** Geri tuşu: açık pencere varsa sayfa onu kapatır; yoksa iki kez basınca uygulamadan çıkılır. */
    @Override
    public void onBackPressed() {
        webView.evaluateJavascript(
                "(window.zkHandleBack && window.zkHandleBack()) ? 'y' : 'n'",
                value -> {
                    if (value != null && value.contains("y")) return;
                    long now = System.currentTimeMillis();
                    if (now - lastBackPress < 2000) {
                        finish();
                    } else {
                        lastBackPress = now;
                        Toast.makeText(MainActivity.this, "Çıkmak için tekrar geri basın", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_FILE_CHOOSER) {
            if (filePathCallback != null) {
                filePathCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                filePathCallback = null;
            }
        } else if (requestCode == REQ_SAVE_BACKUP) {
            boolean ok = false;
            if (resultCode == RESULT_OK && data != null && data.getData() != null && pendingBackupContent != null) {
                try (OutputStream out = getContentResolver().openOutputStream(data.getData(), "w")) {
                    if (out != null) {
                        out.write(pendingBackupContent.getBytes(StandardCharsets.UTF_8));
                        ok = true;
                    }
                } catch (Exception e) {
                    ok = false;
                }
            }
            pendingBackupContent = null;
            webView.evaluateJavascript("window.zkOnBackupSaved && window.zkOnBackupSaved(" + ok + ")", null);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_LOCATION && geoCallback != null) {
            geoCallback.invoke(geoOrigin, hasLocationPermission(), false);
            geoCallback = null;
            geoOrigin = null;
        }
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
    }

    @Override
    protected void onPause() {
        webView.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.removeJavascriptInterface("AndroidBridge");
            webView.destroy();
        }
        super.onDestroy();
    }

    /** Sayfadaki JavaScript'in çağırdığı telefon işlevleri (window.AndroidBridge). */
    private class Bridge {

        /** Yedeği kullanıcının seçtiği klasöre (ör. İndirilenler, Drive) kaydeder. */
        @JavascriptInterface
        public void saveBackup(final String fileName, final String content) {
            runOnUiThread(() -> {
                pendingBackupContent = content;
                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                intent.putExtra(Intent.EXTRA_TITLE, fileName);
                try {
                    startActivityForResult(intent, REQ_SAVE_BACKUP);
                } catch (ActivityNotFoundException e) {
                    pendingBackupContent = null;
                    webView.evaluateJavascript("window.zkOnBackupSaved && window.zkOnBackupSaved(false)", null);
                }
            });
        }

        /** Sayaç ekranı açıkken ekranın kararmasını engeller. */
        @JavascriptInterface
        public void keepScreenOn(final boolean on) {
            runOnUiThread(() -> {
                if (on) {
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                } else {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            });
        }
    }
}
