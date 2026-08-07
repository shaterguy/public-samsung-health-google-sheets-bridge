# Samsung Health → Health Connect → Google Sheets Bridge

Samsung Health가 Android Health Connect에 공유한 건강 데이터를 휴대전화에서 사용자가 지정한 Google 스프레드시트로 직접 동기화하는 개인용 Android 앱입니다.

## Public baseline

- Version: `0.3.4`
- Package: `com.personal.healthbridge.reinstall`
- Android 9 (API 28) 이상
- Google Sheets API 직접 사용
- 별도 개인 서버나 GPT Actions 서버 없음

공개 0.3.4는 기존 비공개판과 서명 인증서뿐 아니라 Android application ID도 분리한 충돌 방지 기준선입니다. 기존 비공개 앱의 패키지 상태가 다른 사용자/프로필 등에 남아 있더라도 공개 앱 설치와 충돌하지 않도록 새 application ID를 사용합니다. 공개 0.3.4 이후 버전은 동일한 application ID와 공개판 전용 서명 인증서를 계속 사용하므로 덮어쓰기 업데이트가 가능합니다.

## Data flow

```text
Galaxy Watch / Galaxy Ring
        ↓
Samsung Health
        ↓
Android Health Connect
        ↓ 사용자가 승인한 읽기 권한
Health Bridge Android 앱
        ↓ Google OAuth + Google Sheets API
사용자가 지정한 Google 스프레드시트
```

건강 데이터는 GitHub 저장소나 별도 개인 서버로 전송하지 않습니다. 스프레드시트 ID와 Google 승인 상태는 앱의 암호화 설정에 저장하며, 실제 건강 데이터는 사용자가 선택한 Google Sheet에 직접 기록됩니다.

## First public installation

1. 공개 저장소 Release의 `HealthBridge-v0.3.4.apk`를 설치합니다.
2. 앱에서 Google 스프레드시트 ID를 입력하고 저장합니다.
3. Health Connect 읽기 권한을 승인합니다.
4. `Google Sheets 연결`을 눌러 Google 계정을 승인합니다.
5. 동기화 조건을 저장하고 필요하면 전체 다시 대조를 실행합니다.

Google OAuth는 APK의 패키지명과 서명 SHA-1을 확인합니다. 공개판 전용 패키지명과 SHA-1은 `PUBLIC_OAUTH_SETUP.md`에 기록돼 있습니다.

## Supported data

- steps
- heart rate
- sleep
- weight and height
- exercise
- blood glucose
- oxygen saturation
- blood pressure
- total calories burned
- distance, power and speed
- VO2 max
- nutrition
- body fat
- basal metabolic rate

실제 동기화 범위는 Samsung Health가 Health Connect에 공유했고 사용자가 권한을 허용한 데이터에 한정됩니다.

## Build and release

GitHub Actions는 Android 단위 테스트와 APK 빌드를 수행합니다. 정식 공개 APK는 repository secret `HEALTHBRIDGE_PUBLIC_SIGNING_BUNDLE_BASE64`에서 공개판 전용 고정 서명키를 복원해 서명합니다. 개인 키·스프레드시트 ID·OAuth 토큰·건강 데이터는 저장소에 커밋하지 않습니다.

정식 Release가 성공하면 APK의 패키지명, 버전, 서명 인증서, 16KB ZIP 정렬, SHA-256 체크섬을 다시 검증하고 `.github/release-selftest-report.json`에 결과를 기록합니다.
