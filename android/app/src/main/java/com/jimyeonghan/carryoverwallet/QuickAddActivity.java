package com.jimyeonghan.carryoverwallet;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * 홈 화면 위젯의 ＋ 버튼이 띄우는 플로팅 팝업. 앱(WebView)을 열지 않고 지출을 추가한다.
 * 저장 시 SharedPreferences의 대기열(pending_expenses)에 쌓고, 위젯 잔액을 즉시 갱신한다.
 * 다음에 앱을 열면 웹앱이 대기열을 읽어 localStorage 가계부에 반영한다.
 */
public class QuickAddActivity extends AppCompatActivity {

    // 웹앱의 CATS와 동일한 순서/식별자
    static final String[] CAT_IDS = {"food", "cafe", "trans", "shop", "fun", "etc"};
    static final String[] CAT_LABELS = {"🍚 식비", "☕ 카페", "🚌 교통", "🛒 생활", "🎮 여가", "📦 기타"};

    private String selectedCat = "food";
    private Button[] catButtons;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quick_add);

        // 다이얼로그 폭을 화면에 맞게 넓힘 + 키보드 자동 표시
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        final EditText amount = findViewById(R.id.qa_amount);
        final EditText memo = findViewById(R.id.qa_memo);
        LinearLayout catRow = findViewById(R.id.qa_cats);

        // 카테고리 버튼 동적 생성
        catButtons = new Button[CAT_IDS.length];
        for (int i = 0; i < CAT_IDS.length; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(CAT_LABELS[i]);
            b.setAllCaps(false);
            b.setTextColor(0xFFE8EAED);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, dp(8), 0);
            b.setLayoutParams(lp);
            b.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { selectCat(idx); }
            });
            catButtons[i] = b;
            catRow.addView(b);
        }
        selectCat(0);

        amount.requestFocus();

        findViewById(R.id.qa_cancel).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.qa_save).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { save(amount, memo); }
        });
    }

    private void selectCat(int idx) {
        selectedCat = CAT_IDS[idx];
        for (int i = 0; i < catButtons.length; i++) {
            catButtons[i].setBackgroundColor(i == idx ? 0xFF4F8CFF : 0xFF22262F);
        }
    }

    private void save(EditText amountEl, EditText memoEl) {
        int amount;
        try {
            amount = Integer.parseInt(amountEl.getText().toString().trim());
        } catch (Exception e) {
            amount = 0;
        }
        if (amount <= 0) {
            Toast.makeText(this, "금액을 입력하세요", Toast.LENGTH_SHORT).show();
            amountEl.requestFocus();
            return;
        }
        String memo = memoEl.getText().toString().trim();
        String date = today();

        try {
            SharedPreferences sp = getSharedPreferences(WalletWidget.PREFS, MODE_PRIVATE);

            // 1) 대기열에 추가 (앱 실행 시 웹앱이 가져감)
            JSONArray queue;
            try { queue = new JSONArray(sp.getString("pending_expenses", "[]")); }
            catch (Exception e) { queue = new JSONArray(); }
            JSONObject item = new JSONObject();
            item.put("id", System.currentTimeMillis());
            item.put("date", date);
            item.put("amount", amount);
            item.put("memo", memo);
            item.put("cat", selectedCat);
            queue.put(item);

            // 2) 위젯 즉시 반영을 위한 낙관적 사용액 갱신
            String curMonth = date.substring(0, 7);
            String prevMonth = sp.getString("month", "");
            int spent = sp.getInt("spent", 0);
            SharedPreferences.Editor ed = sp.edit();
            ed.putString("pending_expenses", queue.toString());
            if (curMonth.equals(prevMonth)) {
                ed.putInt("spent", spent + amount);
            } else {
                ed.putString("month", curMonth);
                ed.putInt("spent", amount);
            }
            ed.apply();

            // 3) 위젯 갱신
            AppWidgetManager mgr = AppWidgetManager.getInstance(this);
            ComponentName cn = new ComponentName(this, WalletWidget.class);
            WalletWidget.updateAll(this, mgr, mgr.getAppWidgetIds(cn));

            Toast.makeText(this, "지출 " + String.format(Locale.KOREA, "%,d원", amount) + " 추가됨", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "저장 실패", Toast.LENGTH_SHORT).show();
        }
        finish();
    }

    private String today() {
        Calendar c = Calendar.getInstance();
        return String.format(Locale.US, "%04d-%02d-%02d",
                c.get(Calendar.YEAR), c.get(Calendar.MONTH) + 1, c.get(Calendar.DAY_OF_MONTH));
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
