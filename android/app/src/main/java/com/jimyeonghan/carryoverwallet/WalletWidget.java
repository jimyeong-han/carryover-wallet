package com.jimyeonghan.carryoverwallet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * 홈 화면 위젯: "오늘 쓸 수 있는 돈(이월 포함)"을 표시하고,
 * 설정에서 고른 스타일에 따라 하단 영역을 다르게 채운다.
 *   0=진행바+오늘사용, 1=이번달요약, 2=최근지출, 3=분류별 빠른추가
 * 값은 SharedPreferences("CarryoverWidget")에서 읽는다(웹앱이 기록).
 */
public class WalletWidget extends AppWidgetProvider {

    public static final String PREFS = "CarryoverWidget";

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        updateAll(ctx, mgr, ids);
    }

    public static void updateAll(Context ctx, AppWidgetManager mgr, int[] ids) {
        if (ids == null) return;
        for (int id : ids) updateOne(ctx, mgr, id);
    }

    static void updateOne(Context ctx, AppWidgetManager mgr, int id) {
        SharedPreferences sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int daily = sp.getInt("daily", 0);
        int spent = sp.getInt("spent", 0);
        int todaySpent = sp.getInt("todaySpent", 0);
        String month = sp.getString("month", "");
        boolean hasData = sp.contains("daily");
        int style = sp.getInt("widget_style", 0);

        Calendar cal = Calendar.getInstance();
        String curMonth = String.format(Locale.US, "%04d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int dim = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        boolean isCur = month.equals(curMonth);

        int elapsed = isCur ? day : dim;
        long accBudget = (long) daily * elapsed;
        long balance = accBudget - spent;
        if (month.isEmpty()) balance = (long) daily * day; // 데이터 없을 때 방어

        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.wallet_widget);

        // 배경 색상/투명도
        int bgColor = sp.getInt("widget_bg_color", 0xFF1A1D24);
        int bgAlpha = sp.getInt("widget_bg_alpha", 255);
        v.setInt(R.id.widget_bg_img, "setColorFilter", bgColor);
        v.setInt(R.id.widget_bg_img, "setImageAlpha", bgAlpha);

        // 글자색 (흰/검)
        int textColor = sp.getInt("widget_text_color", 0xFFFFFFFF);
        int muted = (textColor & 0x00FFFFFF) | 0xB0000000;
        v.setTextColor(R.id.widget_balance, textColor);
        v.setTextColor(R.id.widget_label, muted);
        v.setTextColor(R.id.widget_progress_text, muted);
        v.setTextColor(R.id.widget_summary, muted);
        v.setTextColor(R.id.widget_rec1, muted);
        v.setTextColor(R.id.widget_rec2, muted);
        v.setTextColor(R.id.widget_rec3, muted);

        v.setTextViewText(R.id.widget_label, "오늘 쓸 수 있는 돈");
        v.setTextViewText(R.id.widget_balance, hasData ? won(balance) : "앱을 한 번 열어주세요");

        // 모든 섹션 숨김
        v.setViewVisibility(R.id.sec_progress, View.GONE);
        v.setViewVisibility(R.id.sec_summary, View.GONE);
        v.setViewVisibility(R.id.sec_recent, View.GONE);
        v.setViewVisibility(R.id.sec_quick, View.GONE);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        if (hasData) {
            switch (style) {
                case 1: { // 이번 달 요약
                    long budget = (long) daily * dim;
                    long remain = budget - spent;
                    v.setTextViewText(R.id.widget_summary,
                        "예산 " + shortWon(budget) + " · 사용 " + shortWon(spent) + " · 남음 " + shortWon(remain));
                    v.setViewVisibility(R.id.sec_summary, View.VISIBLE);
                    break;
                }
                case 2: { // 최근 지출
                    fillRecent(ctx, v, sp);
                    v.setViewVisibility(R.id.sec_recent, View.VISIBLE);
                    break;
                }
                case 3: { // 분류별 빠른 추가
                    v.setOnClickPendingIntent(R.id.widget_q_food, quickIntent(ctx, "food", 2, flags));
                    v.setOnClickPendingIntent(R.id.widget_q_cafe, quickIntent(ctx, "cafe", 3, flags));
                    v.setOnClickPendingIntent(R.id.widget_q_etc, quickIntent(ctx, "etc", 4, flags));
                    v.setViewVisibility(R.id.sec_quick, View.VISIBLE);
                    break;
                }
                default: { // 0: 진행 바 + 오늘 사용
                    int pct = accBudget > 0 ? (int) Math.min(100, Math.round(spent * 100.0 / accBudget)) : 0;
                    v.setProgressBar(R.id.widget_progress, 100, pct, false);
                    v.setTextViewText(R.id.widget_progress_text,
                        isCur ? ("오늘 사용 " + won(todaySpent) + " · 사용률 " + pct + "%")
                              : ("이번 달 사용률 " + pct + "%"));
                    v.setViewVisibility(R.id.sec_progress, View.VISIBLE);
                    break;
                }
            }
        }

        // 위젯 본문 탭 → 앱 열기
        Intent open = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (open != null) {
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            v.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(ctx, 0, open, flags));
        }
        // ＋ → 빠른 추가(기본 분류)
        v.setOnClickPendingIntent(R.id.widget_add, quickIntent(ctx, null, 1, flags));

        mgr.updateAppWidget(id, v);
    }

    private static PendingIntent quickIntent(Context ctx, String cat, int reqCode, int flags) {
        Intent i = new Intent(ctx, QuickAddActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (cat != null) i.putExtra("cat", cat);
        return PendingIntent.getActivity(ctx, reqCode, i, flags);
    }

    private static void fillRecent(Context ctx, RemoteViews v, SharedPreferences sp) {
        int[] ids = { R.id.widget_rec1, R.id.widget_rec2, R.id.widget_rec3 };
        JSONArray arr;
        try { arr = new JSONArray(sp.getString("recent", "[]")); }
        catch (Exception e) { arr = new JSONArray(); }
        for (int i = 0; i < ids.length; i++) {
            if (i < arr.length()) {
                JSONObject o = arr.optJSONObject(i);
                String name = o != null ? o.optString("name", "지출") : "지출";
                long amt = o != null ? o.optLong("amount", 0) : 0;
                v.setViewVisibility(ids[i], View.VISIBLE);
                v.setTextViewText(ids[i], "· " + name + "  −" + won(amt));
            } else {
                v.setViewVisibility(ids[i], View.GONE);
            }
        }
    }

    static String won(long n) {
        return String.format(Locale.KOREA, "%,d원", n);
    }

    static String shortWon(long n) {
        if (Math.abs(n) >= 10000) {
            double m = n / 10000.0;
            if (m == Math.floor(m)) return String.format(Locale.KOREA, "%,d만", (long) m);
            return String.format(Locale.KOREA, "%.1f만", m);
        }
        return String.format(Locale.KOREA, "%,d", n);
    }
}
