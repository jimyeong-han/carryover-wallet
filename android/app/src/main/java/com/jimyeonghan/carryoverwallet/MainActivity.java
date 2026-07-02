package com.jimyeonghan.carryoverwallet;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.webkit.ValueCallback;

import com.getcapacitor.BridgeActivity;

import org.json.JSONObject;
import org.json.JSONTokener;

/**
 * 앱이 백그라운드로 갈 때 웹앱의 localStorage에서 위젯용 요약값을 읽어
 * SharedPreferences에 저장하고, 홈 화면 위젯을 즉시 갱신한다.
 * 웹앱은 localStorage["wallet_widget"] = {"daily":n,"spent":n,"month":"YYYY-MM"} 를 기록한다.
 */
public class MainActivity extends BridgeActivity {

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
            // evaluateJavascript는 JS 문자열을 JSON 인코딩해서 돌려준다(바깥이 따옴표로 감싸진 문자열).
            Object outer = new JSONTokener(value).nextValue();
            if (!(outer instanceof String)) return;
            JSONObject o = new JSONObject((String) outer);

            int daily = o.optInt("daily", 0);
            int spent = o.optInt("spent", 0);
            String month = o.optString("month", "");

            SharedPreferences sp = getSharedPreferences(WalletWidget.PREFS, MODE_PRIVATE);
            sp.edit()
                .putInt("daily", daily)
                .putInt("spent", spent)
                .putString("month", month)
                .apply();

            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            ComponentName cn = new ComponentName(this, WalletWidget.class);
            int[] ids = mgr.getAppWidgetIds(cn);
            WalletWidget.updateAll(this, mgr, ids);
        } catch (Exception ignored) {}
    }
}
