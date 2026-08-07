# Health Bridge Android 0.3.3

이 폴더는 Samsung Health가 Health Connect에 공유한 건강 데이터를 사용자가 지정한 Google 스프레드시트에 직접 동기화하는 Android 앱입니다.

## 최초 공개판 설치

기존 비공개판과 공개 0.3.3은 서명 인증서가 다르므로 기존 앱을 제거한 뒤 설치합니다. 공개 0.3.3 이후 버전은 같은 공개판 전용 서명키를 사용하므로 덮어쓰기 업데이트가 가능합니다.

설치 후 앱에서 다음 순서로 한 번 설정합니다.

1. Google 스프레드시트 ID를 입력하고 `스프레드시트 ID 저장`을 누릅니다.
2. Health Connect 읽기 권한을 승인합니다.
3. `Google Sheets 연결`을 눌러 Google 계정을 승인합니다.
4. 동기화 주기와 최소 배터리 조건을 저장합니다.
5. 필요하면 `전체 다시 대조`를 실행합니다.

스프레드시트 ID와 Google 계정 연결 상태는 앱의 암호화 저장소에 보관됩니다. 실제 건강 데이터나 Google OAuth 토큰은 소스 저장소에 들어가지 않습니다.

## 로컬 빌드

JDK 17, Android SDK와 Gradle 8.11.1이 필요합니다.

```text
cd android
gradle testDebugUnitTest assembleDebug --no-daemon
```

공개 정식 APK는 GitHub Actions에서 공개판 전용 고정 서명키로 생성합니다. 개인 signing key는 저장소 파일로 보관하지 않습니다.
