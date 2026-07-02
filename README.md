# 💰 이월 가계부 (Carryover Wallet)

하루 사용 가능 금액(일일 예산)을 정하고, 쓰고 남은 잔액을 자동으로 **이월**해 생활비를 관리하는 가계부입니다. 웹(PWA) + 안드로이드 홈 화면 위젯을 지원합니다.

## 핵심 로직

```
현재 잔액 = 일일예산 × 이번 달 지난 일수 − 이번 달 총 사용액
```

덜 쓴 날의 금액이 다음 날 잔액으로 자동 누적(이월)됩니다.

## 주요 기능

- 일일 예산 수동 설정 (⚙ 설정, 상단 우측)
- 지출 기록 (금액 · 날짜 지정 · 카테고리 · 메모), 삭제 시 확인
- 이월 잔액 · 오늘 남은 몫 · 사용률 표시
- 분류별 지출 도넛 차트(접이식), 지출 내역 접기/스크롤
- 상단 월 이동으로 전체 뷰를 월 단위 전환, 최근 3개월 보관
- PWA — 홈 화면 설치 · 오프라인 동작
- **안드로이드 홈 화면 위젯** — "오늘 쓸 수 있는 돈" 표시
- 데이터는 기기 내 localStorage에 저장

## 프로젝트 구조

```
www/                 웹앱 소스(단일 소스). GitHub Pages(Actions)와 Capacitor가 함께 사용
android/             Capacitor 안드로이드 네이티브 프로젝트 (홈 화면 위젯 포함)
capacitor.config.json
package.json
```

## 웹 실행 / 배포

- 로컬: `python -m http.server 8080 -d www` 후 `http://127.0.0.1:8080`
- 배포: `www/`가 변경되면 GitHub Actions가 Pages로 자동 배포 → https://jimyeong-han.github.io/carryover-wallet/

## 안드로이드 앱 + 위젯 빌드 (Android Studio)

> 복사되는 웹 자산과 생성 설정 파일은 git에서 제외되므로, **빌드 전 한 번 동기화**가 필요합니다.

```bash
npm install
npx cap sync android      # www → android 로 웹 자산/설정 복사
npx cap open android      # Android Studio 열기
```

Android Studio에서:

1. Gradle 동기화가 끝나면 상단 ▶ **Run** 으로 폰(또는 에뮬레이터)에 설치
2. 처음 설치 후 **앱을 한 번 실행**하면 위젯용 데이터가 기록됩니다
3. 홈 화면 길게 누르기 → **위젯** → "이월 가계부" 위젯을 배치

- 위젯은 앱을 백그라운드로 보낼 때(onPause)와 약 30분 주기로 갱신됩니다.
- 위젯 값은 항상 **현재 달** 기준 "오늘 쓸 수 있는 돈"이며, 날짜가 바뀌면 주기 갱신 시 자동 반영됩니다.

### 데이터 연동 방식

웹앱은 렌더링할 때마다 `localStorage["wallet_widget"]`에 `{daily, spent, month}`(현재 달 기준)를 기록합니다. 네이티브 `MainActivity`가 이를 읽어 `SharedPreferences("CarryoverWidget")`에 저장하고, `WalletWidget`(AppWidgetProvider)이 그 값으로 잔액을 계산해 표시합니다.

## 웹 코드 수정 시

`www/` 안의 파일만 고치면 됩니다.
- Pages: push 하면 Actions가 자동 배포
- 안드로이드: `npx cap sync android` 후 다시 빌드
