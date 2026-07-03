package com.jimyeonghan.carryoverwallet;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

/**
 * 위젯 배치 시(또는 재구성 시) 뜨는 설정 화면. 배경색과 투명도를 지정한다.
 * 값은 SharedPreferences("CarryoverWidget")에 전역 저장되어 모든 위젯에 적용된다.
 */
public class WidgetConfigActivity extends AppCompatActivity {

    // 선택 가능한 배경색 (어두운 톤 + 밝은 톤)
    static final int[] COLORS = {
        0xFF1A1D24, // 다크(기본)
        0xFF000000, // 블랙
        0xFF23262B, // 차콜
        0xFF10233F, // 딥블루
        0xFF241B3A, // 딥퍼플
        0xFF10261C, // 딥그린
        0xFFFFFFFF, // 화이트
        0xFFF2F3F5, // 라이트그레이
        0xFFFDF6E3, // 크림
        0xFFDCE7F5  // 연블루
    };

    static final int TEXT_WHITE = 0xFFFFFFFF;
    static final int TEXT_BLACK = 0xFF15181F;

    static final String[] STYLE_LABELS = {"없음", "진행바", "요약", "최근 지출"};
    static final String[] RIGHT_LABELS = {"없음", "하루 가능", "7일 막대", "분류 막대", "빠른 추가"};

    // 빠른추가 표시 분류 후보 (WalletWidget와 동일 순서)
    static final String[] QC_IDS = {"food", "cafe", "trans", "shop", "fun", "etc"};
    static final String[] QC_LABELS = {"🍚 식비", "☕ 카페", "🚌 교통", "🛒 생활", "🎮 여가", "📦 기타"};
    static final int QC_MAX = 3;

    private int appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
    private int selectedColor = COLORS[0];
    private int alpha = 255;               // 0~255 (불투명도)
    private int textColor = TEXT_WHITE;
    private int styleIndex = 0;            // 하단
    private int rightIndex = 1;            // 우측
    private ImageView preview;
    private TextView previewText;
    private View[] swatches;
    private Button textWhiteBtn, textBlackBtn;
    private Button[] styleButtons;
    private Button[] rightButtons;
    private Button[] qcButtons;
    private boolean[] qcSel = new boolean[QC_IDS.length];
    private View qcBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 취소로 끝나도 되도록 기본 결과 설정
        setResult(RESULT_CANCELED);

        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        setContentView(R.layout.activity_widget_config);
        if (getWindow() != null) {
            getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        preview = findViewById(R.id.cfg_preview);
        previewText = findViewById(R.id.cfg_preview_text);
        final TextView alphaLabel = findViewById(R.id.cfg_alpha_label);
        SeekBar alphaBar = findViewById(R.id.cfg_alpha);
        LinearLayout colorRow = findViewById(R.id.cfg_colors);
        textWhiteBtn = findViewById(R.id.cfg_text_white);
        textBlackBtn = findViewById(R.id.cfg_text_black);
        LinearLayout styleRow = findViewById(R.id.cfg_styles);
        LinearLayout rightRow = findViewById(R.id.cfg_rights);

        // 현재 저장된 값 불러오기
        SharedPreferences sp = getSharedPreferences(WalletWidget.PREFS, MODE_PRIVATE);
        selectedColor = sp.getInt("widget_bg_color", COLORS[0]);
        alpha = sp.getInt("widget_bg_alpha", 255);
        textColor = sp.getInt("widget_text_color", TEXT_WHITE);
        styleIndex = sp.getInt("widget_style", 0);
        rightIndex = sp.getInt("widget_right", 1);

        // 우측 내용 버튼
        rightButtons = new Button[RIGHT_LABELS.length];
        for (int i = 0; i < RIGHT_LABELS.length; i++) {
            final int idx = i;
            Button rb = makeChip(RIGHT_LABELS[i]);
            rb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { rightIndex = idx; refreshRightButtons(); }
            });
            rightButtons[i] = rb;
            rightRow.addView(rb);
        }
        refreshRightButtons();

        // 빠른추가 표시 분류 (다중 선택, 최대 3)
        qcBox = findViewById(R.id.cfg_quickcats_box);
        LinearLayout qcRow = findViewById(R.id.cfg_quickcats);
        loadQuickCats(sp.getString("widget_quick_cats", "food,cafe,etc"));
        qcButtons = new Button[QC_IDS.length];
        for (int i = 0; i < QC_IDS.length; i++) {
            final int idx = i;
            Button qb = makeChip(QC_LABELS[i]);
            qb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { toggleQuickCat(idx); }
            });
            qcButtons[i] = qb;
            qcRow.addView(qb);
        }
        refreshQuickCats();
        refreshRightButtons();   // qcBox 할당 후 초기 표시 상태 반영(빠른추가면 바로 표시)

        // 하단 내용 버튼
        styleButtons = new Button[STYLE_LABELS.length];
        for (int i = 0; i < STYLE_LABELS.length; i++) {
            final int idx = i;
            Button sb = makeChip(STYLE_LABELS[i]);
            sb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { styleIndex = idx; refreshStyleButtons(); }
            });
            styleButtons[i] = sb;
            styleRow.addView(sb);
        }
        refreshStyleButtons();

        textWhiteBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { textColor = TEXT_WHITE; refreshTextButtons(); updatePreview(); }
        });
        textBlackBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { textColor = TEXT_BLACK; refreshTextButtons(); updatePreview(); }
        });
        refreshTextButtons();

        // 색상 스와치 생성
        swatches = new View[COLORS.length];
        for (int i = 0; i < COLORS.length; i++) {
            final int idx = i;
            View sw = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(40), dp(40));
            lp.setMargins(0, 0, dp(10), 0);
            sw.setLayoutParams(lp);
            sw.setBackgroundColor(COLORS[i]);
            sw.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { selectedColor = COLORS[idx]; refreshSwatches(); updatePreview(); }
            });
            swatches[i] = sw;
            colorRow.addView(sw);
        }
        refreshSwatches();

        int pct = Math.round(alpha * 100f / 255f);
        alphaBar.setProgress(pct);
        alphaLabel.setText(String.format(Locale.US, "투명도 %d%%", 100 - pct));
        alphaBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                alpha = Math.round(progress * 255f / 100f);
                alphaLabel.setText(String.format(Locale.US, "투명도 %d%%", 100 - progress));
                updatePreview();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        updatePreview();

        findViewById(R.id.cfg_cancel).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { finish(); }
        });
        findViewById(R.id.cfg_save).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { saveAndFinish(); }
        });
    }

    private void refreshSwatches() {
        for (int i = 0; i < swatches.length; i++) {
            // 선택된 스와치에 흰 테두리 표시
            swatches[i].setBackgroundColor(COLORS[i]);
            swatches[i].setAlpha(COLORS[i] == selectedColor ? 1f : 0.55f);
            swatches[i].setScaleX(COLORS[i] == selectedColor ? 1f : 0.85f);
            swatches[i].setScaleY(COLORS[i] == selectedColor ? 1f : 0.85f);
        }
    }

    private Button makeChip(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(0xFFE8EAED);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, dp(8), 0);
        b.setLayoutParams(lp);
        return b;
    }

    private void refreshStyleButtons() {
        for (int i = 0; i < styleButtons.length; i++) {
            styleButtons[i].setBackgroundResource(i == styleIndex ? R.drawable.widget_save_bg : R.drawable.widget_add_bg_ghost);
        }
    }

    private void refreshRightButtons() {
        for (int i = 0; i < rightButtons.length; i++) {
            rightButtons[i].setBackgroundResource(i == rightIndex ? R.drawable.widget_save_bg : R.drawable.widget_add_bg_ghost);
        }
        if (qcBox != null) qcBox.setVisibility(rightIndex == 4 ? View.VISIBLE : View.GONE);
    }

    private void loadQuickCats(String csv) {
        for (int i = 0; i < qcSel.length; i++) qcSel[i] = false;
        if (csv == null) return;
        for (String s : csv.split(",")) {
            String c = s.trim();
            for (int i = 0; i < QC_IDS.length; i++) if (QC_IDS[i].equals(c)) qcSel[i] = true;
        }
    }

    private int qcCount() { int c = 0; for (boolean b : qcSel) if (b) c++; return c; }

    private void toggleQuickCat(int idx) {
        if (!qcSel[idx] && qcCount() >= QC_MAX) {
            android.widget.Toast.makeText(this, "최대 " + QC_MAX + "개까지 선택할 수 있어요", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        qcSel[idx] = !qcSel[idx];
        refreshQuickCats();
    }

    private void refreshQuickCats() {
        for (int i = 0; i < qcButtons.length; i++)
            qcButtons[i].setBackgroundResource(qcSel[i] ? R.drawable.widget_save_bg : R.drawable.widget_add_bg_ghost);
    }

    private String quickCatsCsv() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < QC_IDS.length; i++) if (qcSel[i]) { if (sb.length() > 0) sb.append(","); sb.append(QC_IDS[i]); }
        return sb.toString();
    }

    private void refreshTextButtons() {
        boolean white = textColor == TEXT_WHITE;
        textWhiteBtn.setBackgroundResource(white ? R.drawable.widget_save_bg : R.drawable.widget_add_bg_ghost);
        textBlackBtn.setBackgroundResource(!white ? R.drawable.widget_save_bg : R.drawable.widget_add_bg_ghost);
    }

    private void updatePreview() {
        preview.setColorFilter(selectedColor);
        preview.setImageAlpha(alpha);
        previewText.setTextColor(textColor);
    }

    private void saveAndFinish() {
        SharedPreferences sp = getSharedPreferences(WalletWidget.PREFS, MODE_PRIVATE);
        sp.edit()
            .putInt("widget_bg_color", selectedColor)
            .putInt("widget_bg_alpha", alpha)
            .putInt("widget_text_color", textColor)
            .putInt("widget_style", styleIndex)
            .putInt("widget_right", rightIndex)
            .putString("widget_quick_cats", quickCatsCsv())
            .apply();

        // 모든 위젯 갱신
        AppWidgetManager mgr = AppWidgetManager.getInstance(this);
        ComponentName cn = new ComponentName(this, WalletWidget.class);
        WalletWidget.updateAll(this, mgr, mgr.getAppWidgetIds(cn));

        Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        setResult(RESULT_OK, result);
        finish();
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
