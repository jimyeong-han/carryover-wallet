package com.jimyeonghan.carryoverwallet;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.widget.RemoteViews;

import java.util.Calendar;
import java.util.Locale;

/**
 * 홈 화면 위젯: "오늘 쓸 수 있는 돈(이월 포함)"을 표시.
 * 값은 SharedPreferences("CarryoverWidget")에서 읽어(웹앱이 MainActivity를 통해 기록),
 * 위젯이 갱신될 때마다 현재 날짜 기준으로 잔액을 다시 계산한다.
 *   잔액 = 일일예산 × 이번 달 지난 일수 − 이번 달 사용액
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
        String month = sp.getString("month", "");
        boolean hasData = sp.contains("daily");

        Calendar cal = Calendar.getInstance();
        String curMonth = String.format(Locale.US, "%04d-%02d",
                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        long balance;
        if (month.equals(curMonth)) {
            balance = (long) daily * day - spent;      // 이번 달: 이월 포함 현재 잔액
        } else {
            balance = (long) daily * day;              // 달이 바뀐 뒤 앱 미실행: 새 달로 가정
        }

        RemoteViews v = new RemoteViews(ctx.getPackageName(), R.layout.wallet_widget);

        // 배경 색상/투명도 (설정에서 지정, 기본 다크 불투명)
        int bgColor = sp.getInt("widget_bg_color", 0xFF1A1D24);
        int bgAlpha = sp.getInt("widget_bg_alpha", 255);
        v.setInt(R.id.widget_bg_img, "setColorFilter", bgColor);
        v.setInt(R.id.widget_bg_img, "setImageAlpha", bgAlpha);

        v.setTextViewText(R.id.widget_label, "오늘 쓸 수 있는 돈");
        v.setTextViewText(R.id.widget_balance, hasData ? formatWon(balance) : "앱을 한 번 열어주세요");
        v.setTextViewText(R.id.widget_sub, hasData ? ("일 " + formatWon(daily) + " · " + Integer.parseInt(curMonth.substring(5)) + "월") : "");

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= 23) flags |= PendingIntent.FLAG_IMMUTABLE;

        // 위젯 본문 탭 → 앱 열기
        Intent open = ctx.getPackageManager().getLaunchIntentForPackage(ctx.getPackageName());
        if (open != null) {
            open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            PendingIntent pi = PendingIntent.getActivity(ctx, 0, open, flags);
            v.setOnClickPendingIntent(R.id.widget_root, pi);
        }

        // ＋ 버튼 → 빠른 추가 팝업 (앱 미실행)
        Intent quick = new Intent(ctx, QuickAddActivity.class);
        quick.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent qpi = PendingIntent.getActivity(ctx, 1, quick, flags);
        v.setOnClickPendingIntent(R.id.widget_add, qpi);

        mgr.updateAppWidget(id, v);
    }

    static String formatWon(long n) {
        return String.format(Locale.KOREA, "%,d원", n);
    }
}
