# 스마트 QR — Android WebView 앱

[스마트 QR 생성기](https://smj1740.github.io/dddff/) 사이트를 전체화면으로 보여주는 Android 앱입니다.

- 사이트: https://smj1740.github.io/dddff/
- 원본 웹: https://github.com/smj1740/dddff

## 기능

- 전체화면 WebView (시스템 바 숨김, 스와이프로 일시 표시)
- JavaScript / 로컬 스토리지 지원 (갤러리 저장)
- 외부 링크(예: 대학 홈페이지)는 기본 브라우저에서 열림
- QR 이미지 다운로드 지원

## Android Studio 없이 APK 받기 (GitHub Actions)

1. 이 저장소를 GitHub에 푸시합니다.
2. **Actions** 탭에서 `Build Android APK` 워크플로가 실행되는지 확인합니다.
3. 완료되면 해당 실행(run)을 열고 **Artifacts**에서 다음을 다운로드합니다.
   - `smartqr-debug` — 바로 설치 가능한 디버그 APK
   - `smartqr-release` — 릴리스 APK (디버그 키로 서명됨, 테스트용)

수동 실행: Actions → `Build Android APK` → **Run workflow**

태그로 릴리스하려면:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## 로컬 빌드 (선택)

JDK 17과 Android SDK가 있다면:

```bash
./gradlew assembleDebug
```

APK 경로: `app/build/outputs/apk/debug/app-debug.apk`

## 설치

Android 기기에서 다운로드한 APK를 열고 설치합니다.  
처음이면 **알 수 없는 앱 설치** 허용이 필요할 수 있습니다.

## 아이콘

사이트 톤(KMOU 블루 `#1C3FAA`, 밝은 배경)에 맞춘 QR 테마 런처 아이콘이 포함되어 있습니다.  
원본 마스터 이미지는 `icon-source/app-icon-master.png` 에 있습니다.
