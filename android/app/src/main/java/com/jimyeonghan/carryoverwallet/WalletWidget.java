package com.jimyeonghan.carryoverwallet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.RemoteViews;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * 홈 화면 위젯. 잔액 + (우측 가로 콘텐츠) + (하단 세로 섹션)을 설정에 따라 표시.
 *  widget_style(하단): 0=없음 1=진행바 2=요약 3=최근 4=빠른추가
 *  widget_right(우측): 0=없음 1=하루가능 2=7일막대 3=분류막대
 */
public class WalletWidget extends AppWidgetProvider {

    public static final String PREFS = "CarryoverWidget";

    // 분류 색상(웹 CATS와 동일 순서/값)
    static final String[] CAT_IDS = {"food", "cafe", "trans", "shop", "fun", "etc"};
    static final int[] CAT_COLORS = {0xFF3987E5, 0xFF199E70, 0xFFC98500, 0xFF9085E9, 0xFFE66767, 0xFF8A93A3};

    @Override
    public void onUpdate(Context ctx, AppWidgetManager mgr, int[] ids) {
        updateAll(ctx, mgr, ids);
    }

    @Override
    public void onAppWidgetOptionsChanged(Context ctx, AppWidgetManager mgr, int id, Bundle newOptions) {
        updateOne(ctx, mgr, id);
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
        int style = sp.getInt("widget_style", 0);   // 하단
        int right = sp.getInt("widget_right", 1);    // 우측

        Calendar cal = Calendar.getInstance();
        String curMonth = String.format(Locale.US, "%04d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int dim = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        boolean isCur = month.equals(curMonth);

        int elapsed = isCur ? day : dim;
        long accBudget = (long) daily * elapsed;
        long balance = accBudget - spent;
        if (month.isEmpty()) balance = (long) daily * day;

        // 높이 낮으면 진행바/빠른추가 → 요약
        Bundle opts = mgr.getAppWidgetOptions(id);
        int minH = opts != null ? opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 0) : 0;
        boolean shortWidget = minH > 0 && minH < 110;
        if (shortWidget && (style == 1 || style == 4)) style = 2;

        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.wallet_widget);

        // 배경
        v.setInt(R.id.widget_bg_img, "setColorFilter", sp.getInt("widget_bg_color", 0xFF1A1D24));
        v.setInt(R.id.widget_bg_img, "setImageAlpha", sp.getInt("widget_bg_alpha", 255));

        // 글자색
        int textColor = sp.getInt("widget_text_color", 0xFFFFFFFF);
        int muted = (textColor & 0x00FFFFFF) | 0xB0000000;
        v.setTextColor(R.id.widget_balance, textColor);
        v.setTextColor(R.id.widget_label, muted);
        v.setTextColor(R.id.rc_value, textColor);
        v.setTextColor(R.id.rc_label, muted);
        v.setTextColor(R.id.widget_progress_text, muted);
        v.setTextColor(R.id.sec_summary, muted);
        v.setTextColor(R.id.widget_rec1, muted);
        v.setTextColor(R.id.widget_rec2, muted);
        v.setTextColor(R.id.widget_rec3, muted);

        v.setTextViewText(R.id.widget_label, "오늘 쓸 수 있는 돈");
        v.setTextViewText(R.id.widget_balance, hasData ? won(balance) : "앱을 한 번 열어주세요");

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        // ---- 우측 가로 콘텐츠 ----
        if (!hasData) right = 0;
        v.setViewVisibility(R.id.sec_right, right == 0 ? View.GONE : View.VISIBLE);
        v.setViewVisibility(R.id.rc_text, View.GONE);
        v.setViewVisibility(R.id.rc_img, View.GONE);
        if (right == 1) {                 // 하루 가능
            int remainDays = isCur ? (dim - day + 1) : 1;
            if (remainDays < 1) remainDays = 1;
            long perDay = balance / remainDays;
            v.setTextViewText(R.id.rc_label, "하루 가능 · " + remainDays + "일 남음");
            v.setTextViewText(R.id.rc_value, won(perDay));
            v.setViewVisibility(R.id.rc_text, View.VISIBLE);
        } else if (right == 2) {          // 최근 7일 막대
            v.setImageViewBitmap(R.id.rc_img, drawBars(parseLongArray(sp.getString("days7", "[]"))));
            v.setViewVisibility(R.id.rc_img, View.VISIBLE);
        } else if (right == 3) {          // 분류 막대
            v.setImageViewBitmap(R.id.rc_img, drawCatBar(sp.getString("cats", "[]")));
            v.setViewVisibility(R.id.rc_img, View.VISIBLE);
        }

        // ---- 하단 섹션 ----
        v.setViewVisibility(R.id.sec_progress, View.GONE);
        v.setViewVisibility(R.id.sec_summary, View.GONE);
        v.setViewVisibility(R.id.sec_recent, View.GONE);
        v.setViewVisibility(R.id.sec_quick, View.GONE);

        if (hasData) {
            switch (style) {
                case 2: {
                    long budget = (long) daily * dim;
                    v.setTextViewText(R.id.sec_summary,
                        "예산 " + shortWon(budget) + " · 사용 " + shortWon(spent) + " · 남음 " + shortWon(budget - spent));
                    v.setViewVisibility(R.id.sec_summary, View.VISIBLE);
                    break;
                }
                case 3:
                    fillRecent(v, sp);
                    v.setViewVisibility(R.id.sec_recent, View.VISIBLE);
                    break;
                case 4:
                    v.setOnClickPendingIntent(R.id.widget_q_food, quickIntent(ctx, "food", 2, flags));
                    v.setOnClickPendingIntent(R.id.widget_q_cafe, quickIntent(ctx, "cafe", 3, flags));
                    v.setOnClickPendingIntent(R.id.widget_q_etc, quickIntent(ctx, "etc", 4, flags));
                    v.setViewVisibility(R.id.sec_quick, View.VISIBLE);
                    break;
                case 1: {
                    int pct = accBudget > 0 ? (int) Math.min(100, Math.round(spent * 100.0 / accBudget)) : 0;
                    v.setProgressBar(R.id.widget_progress, 100, pct, false);
                    v.setTextViewText(R.id.widget_progress_text,
                        isCur ? ("오늘 사용 " + won(todaySpent) + " · 사용률 " + pct + "%")
                              : ("이번 달 사용률 " + pct + "%"));
                    v.setViewVisibility(R.id.sec_progress, View.VISIBLE);
                    break;
                }
                default: /* 0=없음 */ break;
            }
        }

        // 클릭
        Intent open = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (open != null) {
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            v.setOnClickPendingIntent(R.id.widget_root, PendingIntent.getActivity(ctx, 0, open, flags));
        }
        v.setOnClickPendingIntent(R.id.widget_add, quickIntent(ctx, null, 1, flags));

        mgr.updateAppWidget(id, v);
    }

    private static PendingIntent quickIntent(Context ctx, String cat, int reqCode, int flags) {
        Intent i = new Intent(ctx, QuickAddActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (cat != null) i.putExtra("cat", cat);
        return PendingIntent.getActivity(ctx, reqCode, i, flags);
    }

    private static void fillRecent(RemoteViews v, SharedPreferences sp) {
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

    // ---- 비트맵 그리기 ----
    private static long[] parseLongArray(String json) {
        try {
            JSONArray a = new JSONArray(json);
            long[] out = new long[a.length()];
            for (int i = 0; i < a.length(); i++) out[i] = a.optLong(i, 0);
            return out;
        } catch (Exception e) { return new long[0]; }
    }

    private static Bitmap drawBars(long[] days) {
        int W = 420, H = 112;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int n = days.length > 0 ? days.length : 7;
        long max = 1;
        for (long d : days) if (d > max) max = d;
        float gap = W * 0.05f;
        float barW = (W - gap * (n - 1)) / n;
        float top = 4, bottom = H;
        for (int i = 0; i < n; i++) {
            long val = i < days.length ? days[i] : 0;
            float h = (float) ((val / (double) max) * (bottom - top));
            if (h < 3) h = 3; // 최소 표시
            float x = i * (barW + gap);
            boolean today = (i == n - 1);
            p.setColor(today ? 0xFF6FA8FF : 0xFF4F8CFF);
            if (val == 0) p.setColor(0x334F8CFF);
            RectF r = new RectF(x, bottom - h, x + barW, bottom);
            c.drawRoundRect(r, 6, 6, p);
        }
        return bmp;
    }

    private static Bitmap drawCatBar(String catsJson) {
        int W = 640, H = 40;
        Bitmap bmp = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        float top = H * 0.28f, bottom = H * 0.72f;
        // 데이터
        long total = 0;
        long[] amts;
        int[] cols;
        try {
            JSONArray a = new JSONArray(catsJson);
            amts = new long[a.length()];
            cols = new int[a.length()];
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.getJSONObject(i);
                long amt = o.optLong("amount", 0);
                amts[i] = amt; total += amt;
                cols[i] = colorOf(o.optString("cat", "etc"));
            }
        } catch (Exception e) { amts = new long[0]; cols = new int[0]; }

        if (total <= 0) {
            p.setColor(0x33FFFFFF);
            c.drawRoundRect(new RectF(0, top, W, bottom), 12, 12, p);
            return bmp;
        }
        float gap = 4;
        int count = amts.length;
        float usable = W - gap * (count - 1);
        float x = 0;
        for (int i = 0; i < count; i++) {
            float segW = (float) ((amts[i] / (double) total) * usable);
            p.setColor(cols[i]);
            c.drawRoundRect(new RectF(x, top, x + segW, bottom), 8, 8, p);
            x += segW + gap;
        }
        return bmp;
    }

    private static int colorOf(String cat) {
        for (int i = 0; i < CAT_IDS.length; i++) if (CAT_IDS[i].equals(cat)) return CAT_COLORS[i];
        return 0xFF8A93A3;
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
