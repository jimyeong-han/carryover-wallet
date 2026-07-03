package com.jimyeonghan.carryoverwallet;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;

import com.getcapacitor.BridgeActivity;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * 웹앱(WebView) ↔ 네이티브 위젯 브리지.
 *  - onPause: 웹앱 localStorage의 위젯 요약값을 SharedPreferences에 저장하고 위젯 갱신
 *  - WidgetBridge(JS 인터페이스): 위젯 ＋ 팝업이 쌓아둔 대기열 지출을 웹앱이 가져가도록 제공
 */
public class MainActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            getBridge().getWebView().addJavascriptInterface(new WidgetBridge(this), "WidgetBridge");
        } catch (Exception ignored) {}
    }

    @Override
    public void onPause() {
        super.onPause();
        syncWidget();
    }

    private void syncWidget() {
        try {
            getBridge().getWebView().evaluateJavascript(
                "localStorage.getItem('wallet_widget')",
                new ValueCallback<String>() {
                    @Override
                    public void onReceiveValue(String value) {
                        applyWidgetPayload(value);
                    }
                });
        } catch (Exception ignored) {}
    }

    private void applyWidgetPayload(String value) {
        try {
            if (value == null || value.equals("null")) return;
            Object outer = new JSONTokener(value).nextValue();
            if (!(outer instanceof String)) return;
            JSONObject o = new JSONObject((String) outer);

            int daily = o.optInt("daily", 0);
            int spent = o.optInt("spent", 0);
            String month = o.optString("month", "");
            int todaySpent = o.optInt("todaySpent", 0);
            String recent = o.has("recent") ? o.getJSONArray("recent").toString() : "[]";

            SharedPreferences sp = getSharedPreferences(WalletWidget.PREFS, MODE_PRIVATE);
            sp.edit()
                .putInt("daily", daily)
                .putInt("spent", spent)
                .putString("month", month)
                .putInt("todaySpent", todaySpent)
                .putString("recent", recent)
                .apply();

            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            ComponentName cn = new ComponentName(this, WalletWidget.class);
            WalletWidget.updateAll(this, mgr, mgr.getAppWidgetIds(cn));
        } catch (Exception ignored) {}
    }

    /** 위젯 팝업이 쌓아둔 대기열 지출을 웹앱에 전달/삭제. */
    public static class WidgetBridge {
        private final Context ctx;
        WidgetBridge(Context ctx) { this.ctx = ctx.getApplicationContext(); }

        @JavascriptInterface
        public String getPending() {
            SharedPreferences sp = ctx.getSharedPreferences(WalletWidget.PREFS, Context.MODE_PRIVATE);
            return sp.getString("pending_expenses", "[]");
        }

        @JavascriptInterface
        public void clearPending() {
            SharedPreferences sp = ctx.getSharedPreferences(WalletWidget.PREFS, Context.MODE_PRIVATE);
            sp.edit().remove("pending_expenses").apply();
        }
    }
}
