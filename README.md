# 💰 이월 가계부 (Carryover Wallet)

하루 사용 가능 금액(일일 예산)을 정하고, **쓰고 남은 잔액을 자동으로 이월**해 생활비를 관리하는 가계부입니다.
매달 초 자동으로 초기화되고, 웹(PWA)과 안드로이드 앱 + **홈 화면 위젯**을 지원합니다.

🔗 **바로 써보기(웹):** https://jimyeong-han.github.io/carryover-wallet/

---

## 핵심 개념 — 이월(carryover)

```
현재 잔액 = 일일예산 × 이번 달 지난 일수 − 이번 달 총 사용액
```

- 하루 2만원으로 정했는데 오늘까지 3일이 지났고 4만원을 썼다면 → `2만 × 3 − 4만 = 2만원`이 지금 쓸 수 있는 돈입니다.
- 덜 쓴 날의 금액이 다음 날 잔액으로 **자동 누적(이월)** 됩니다.
- 과거 달을 볼 때는 "지난 일수" 대신 그 달 전체 일수로 계산해 **그 달 최종 잔액**을 보여줍니다.

---

## 기능

### 앱 (웹 / 안드로이드 공통)
- **일일 예산 설정** — 상단 우측 ⚙ 설정 (다이얼로그는 우측 상단 ✕ 로 닫기)
- **지출 기록** — 금액 · 날짜 지정(기본 오늘) · 카테고리(6종) · 내용(최대 20자)
- **삭제 시 확인** — 오터치 방지, 추가 후 키보드 자동 내림
- **이월 잔액 · 오늘 사용액 · 사용률** 표시
- **분류별 도넛 차트** (접이식) + **지출 내역** (접기 / 높이 제한 스크롤)
- **월 이동** — 상단에서 전체 뷰를 월 단위 전환, 최근 **3개월** 보관(오래된 달 자동 삭제), 월 바뀌면 자동 초기화
- **백업 · 기기 이전** — 설정에서 JSON 내보내기 / 가져오기
- **PWA** — 홈 화면 설치 · 오프라인 동작
- 데이터는 **기기 내 localStorage**에 저장 (서버 없음)

### 안드로이드 홈 화면 위젯
- 기본 크기 **4×1**, 가로는 홈 화면 폭에 맞춰 고정(세로만 리사이즈)
- **배경**: 색상(어두운 6종 + 밝은 4종) · 투명도 슬라이더 · 글자색(흰/검)
- **우측 콘텐츠**(택1): 없음 / 하루 가능액(잔액÷남은 일수) / 최근 7일 막대 / 분류 스택 막대 / **빠른 추가(표시 분류 최대 3개 선택)**
- **하단 콘텐츠**(택1): 없음 / 진행바 / 이번 달 요약 / 최근 지출
- **반응형**: 높이가 낮으면(1줄) 진행바는 요약으로 자동 대체
- **＋ 빠른 추가** — 앱을 열지 않고 뜨는 플로팅 팝업으로 지출 입력(위젯 잔액 즉시 갱신, 이후 앱 열면 가계부에 자동 반영)
- 위젯 배치/재구성 시 뜨는 설정 화면에서 위 항목을 선택

---

## 프로젝트 구조

```
www/                                  웹앱 소스 (단일 소스: Pages 배포 + Capacitor가 함께 사용)
  index.html                          앱 전체 (HTML/CSS/JS 단일 파일)
  sw.js                               서비스워커 (오프라인 캐시)
  manifest.webmanifest, icons/        PWA 매니페스트 · 아이콘
android/                              Capacitor 안드로이드 네이티브 프로젝트
  app/src/main/java/.../carryoverwallet/
    MainActivity.java                 웹 WebView ↔ 위젯 데이터 브리지
    WalletWidget.java                 홈 화면 위젯 (AppWidgetProvider)
    WidgetConfigActivity.java         위젯 설정 화면 (배경/글자색/콘텐츠/분류)
    QuickAddActivity.java             ＋ 빠른 추가 플로팅 팝업
  app/src/main/res/                   위젯·팝업 레이아웃, 드로어블
capacitor.config.json                 appId: com.jimyeonghan.carryoverwallet
package.json
.github/workflows/pages.yml           www/ → GitHub Pages 자동 배포
```

---

## 개발 / 실행

### 웹 로컬 실행
```bash
python -m http.server 8080 -d www
# http://127.0.0.1:8080
```

### 배포 (GitHub Pages)
`www/`가 변경된 채로 `main`에 push하면 GitHub Actions가 자동 배포합니다.
- 배포 실패 시 **"실패한 작업만 재실행"은 금지**(아티팩트 중복으로 막힘) → Actions 탭에서 **워크플로 새로 실행** 하거나 커밋을 다시 push
- 대부분 Pages 일시 오류이며, 실패해도 이전 배포는 유지되어 사이트는 내려가지 않음

---

## 안드로이드 앱 + 위젯 빌드 (Android Studio)

> 복사되는 웹 자산·생성 설정 파일은 git에서 제외되므로 **빌드 전 한 번 동기화**가 필요합니다.

```bash
npm install
npx cap sync android      # www → android 로 웹 자산/설정 복사
npx cap open android      # Android Studio 열기
```

Android Studio에서:
1. Gradle 동기화 후 ▶ **Run** 으로 폰/에뮬레이터에 설치 (SDK/JDK는 Android Studio가 자동 준비)
2. **앱을 한 번 실행** → 위젯용 데이터가 기록됨
3. 홈 화면 길게 누르기 → **위젯** → "이월 가계부" 배치 → 설정 화면에서 배경/콘텐츠 선택

**웹 코드만 고쳤을 때:** `npx cap sync android` 후 다시 Run.
**위젯 색/콘텐츠 변경:** Android 12+는 위젯 길게 눌러 재구성, 그 이하는 위젯을 지웠다 다시 배치.

---

## 릴리스 APK 서명 빌드

`android/keystore.properties`가 있으면 릴리스 빌드에 자동 서명됩니다(없으면 서명 설정 무시).

1. 키스토어 생성 (Android Studio 내장 JDK 사용):
   ```powershell
   & "C:\Program Files\Android\Android Studio\jbr\bin\keytool.exe" -genkeypair -v `
     -keystore "android\carryover-release.jks" -alias carryover -keyalg RSA -keysize 2048 -validity 10000
   ```
2. `android/keystore.properties.example` → `android/keystore.properties` 복사 후 값 채우기:
   ```properties
   storeFile=carryover-release.jks
   storePassword=키스토어_비밀번호
   keyAlias=carryover
   keyPassword=키_비밀번호
   ```
3. Android Studio에서 빌드 배리언트를 **release** 로 → **Build → Build APK(s)**
   또는 마법사: **Build → Generate Signed Bundle / APK**

> ⚠️ `.jks`와 비밀번호는 안전하게 백업하세요. 잃어버리면 같은 앱으로 업데이트를 낼 수 없습니다.
> 업데이트 때마다 `android/app/build.gradle`의 `versionCode`(+1)·`versionName`을 올리세요.
> `keystore.properties`, `*.jks`는 **git에 커밋되지 않습니다.**

---

## 데이터 연동 (웹 ↔ 위젯)

앱과 위젯은 `SharedPreferences("CarryoverWidget")`를 통해 값을 주고받습니다.

- **앱 → 위젯**: 웹앱이 렌더링할 때마다 `localStorage["wallet_widget"]`에 `{daily, spent, month, todaySpent, recent, days7, cats}`(현재 달 기준)를 기록.
  `MainActivity`가 앱이 백그라운드로 갈 때 이를 읽어 SharedPreferences에 저장하고 위젯을 갱신.
- **위젯 → 앱**: ＋ 빠른 추가로 입력한 지출은 `pending_expenses` 큐에 쌓이고, 다음에 앱을 열면 `WidgetBridge`(JS 인터페이스)를 통해 가계부에 병합(중복 없이).
- **위젯 표시값**은 항상 **현재 달** 기준으로 계산되며, 날짜가 바뀌면 주기 갱신(약 30분) 또는 앱 실행 시 반영됩니다.

---

## 알아둘 점

- **데이터는 기기별로 저장**됩니다(크롬 PWA와 네이티브 앱은 저장소가 분리). 기기 간 이전은 **내보내기/가져오기**로 하세요.
- 완전 자동 동기화(여러 기기 실시간)는 별도 클라우드 백엔드가 있어야 합니다 — 현재는 서버 없는 로컬 저장 방식입니다.
- **iOS 미지원**: 앱 본체(웹)는 Capacitor로 iOS 빌드가 가능하지만 macOS/Xcode가 필요하고, 위젯은 WidgetKit(Swift)로 새로 작성해야 하므로 현재 범위에서 제외합니다.

---

## 기술 스택

- **웹앱**: 순수 HTML/CSS/JS (프레임워크·빌드 없음), localStorage, Service Worker(PWA)
- **네이티브 래핑**: Capacitor 6 (Android)
- **위젯**: Android AppWidgetProvider · RemoteViews · Canvas 비트맵(막대/차트) · SharedPreferences 브리지
- **배포**: GitHub Pages (GitHub Actions)
