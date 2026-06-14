# DHU smoke for Findroid AA offline player

This smoke proves that the Findroid APK exposes its own Android Auto Car App
Library entry points and that the car UI browses the manifest-backed offline
catalog. It is separate from the Fermata smoke.

## Scope

Valid proof for this stage:

- ordinary Android phone AVD or real Android phone, not AAOS;
- Findroid APK installed with `FindroidMediaCarAppService` and
  `FindroidNavigationCarAppService`;
- Android Auto DHU shows two Findroid entries: `Findroid Media` and
  `Findroid Navigator`;
- `Findroid Media` opens through `androidx.car.app.category.MEDIA`;
- `Findroid Navigator` opens through `androidx.car.app.category.NAVIGATION`;
- tabs are visible: `Offline`, `Movies`, `Episodes`;
- rows come from READY `offlineItemSnapshots` plus READY public video assets;
- item detail opens from a row and exposes `Play`;
- `Play` starts the existing Findroid `PlayerActivity` against the local source.

Not valid proof:

- AAOS screenshots;
- phone-only screenshots without DHU;
- synthetic files or alternate proof folders;
- Fermata screenshots;
- Android Auto media now-playing UI presented as Findroid video rendering.

This stage does not prove a custom Findroid video surface inside DHU. The next
stage must add and verify the actual car video-rendering surface.

## Build

```bash
export ANDROID_HOME=/home/vadv/Android/Sdk
export ANDROID_SDK_ROOT=/home/vadv/Android/Sdk
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk

./gradlew \
  :data:ktfmtFormat \
  :core:ktfmtFormat \
  :app:phone:ktfmtFormat \
  :core:testLibreDebugUnitTest \
  :data:testDebugUnitTest \
  :app:phone:assembleLibreDebug
```

APK:

```text
app/phone/build/outputs/apk/libre/debug/phone-libre-x86_64-debug.apk
```

## Static APK checks

```bash
APK=app/phone/build/outputs/apk/libre/debug/phone-libre-x86_64-debug.apk
aapt dump xmltree "$APK" AndroidManifest.xml | rg \
  'Findroid(Media|Navigation)CarAppService|androidx.car.app.CarAppService|androidx.car.app.category.(MEDIA|NAVIGATION)|com.google.android.gms.car.application|androidx.car.app.minCarApiLevel'
unzip -p "$APK" res/xml/automotive_app_desc.xml | strings
```

Expected manifest facts:

- service: `dev.jdtech.jellyfin.car.FindroidMediaCarAppService`;
- service: `dev.jdtech.jellyfin.car.FindroidNavigationCarAppService`;
- both services use action: `androidx.car.app.CarAppService`;
- media service category: `androidx.car.app.category.MEDIA`;
- navigator service category: `androidx.car.app.category.NAVIGATION`;
- metadata: `com.google.android.gms.car.application`;
- metadata: `androidx.car.app.minCarApiLevel=6`;
- descriptor uses `media` and `template`.

## Device setup

Use the same device that has real Findroid offline downloads.

```bash
ADB="$ANDROID_HOME/platform-tools/adb"
DEVICE=emulator-5554
APK=app/phone/build/outputs/apk/libre/debug/phone-libre-x86_64-debug.apk

$ADB -s "$DEVICE" install -r "$APK"
$ADB -s "$DEVICE" shell appops set dev.jdtech.jellyfin.debug MANAGE_EXTERNAL_STORAGE allow
$ADB -s "$DEVICE" shell appops get dev.jdtech.jellyfin.debug MANAGE_EXTERNAL_STORAGE
```

Verify real offline data exists:

```bash
$ADB -s "$DEVICE" shell 'ls -la "/sdcard/Movies/Findroid/Сериалы/История любви/1"'
```

## Start DHU

Enable Android Auto developer mode, Unknown sources, and Start head unit server
from the phone UI.

```bash
$ADB -s "$DEVICE" forward tcp:5277 tcp:5277
DISPLAY=:95 "$ANDROID_HOME/extras/google/auto/desktop-head-unit"
```

## Runtime proof

Capture:

1. DHU app drawer with `Findroid Media` and `Findroid Navigator`.
2. `Findroid Media` opened from DHU.
3. `Findroid Navigator` opened from DHU.
4. Findroid `Offline` tab with downloaded rows.
5. Findroid `Episodes` tab with episode rows in season order.
6. Item detail after selecting a row.
7. Phone/player screenshot after pressing `Play`, until a native car video
   surface exists.

Useful dumps:

```bash
$ADB -s "$DEVICE" shell dumpsys activity services > services.txt
$ADB -s "$DEVICE" shell dumpsys window > window.txt
$ADB -s "$DEVICE" shell dumpsys media_session > media-session.txt
```

Expected component facts:

- `services.txt` contains `dev.jdtech.jellyfin.car.FindroidMediaCarAppService`
  after opening `Findroid Media`;
- `services.txt` contains
  `dev.jdtech.jellyfin.car.FindroidNavigationCarAppService` after opening
  `Findroid Navigator`;
- DHU/window dumps show Android Auto host rendering Findroid templates;
- after `Play`, the phone side starts `dev.jdtech.jellyfin.PlayerActivity`;
- local playback uses the READY offline source fallback from the manifest.

## Artifact checklist

Save into one artifact folder:

- APK path, size, and mtime;
- static manifest/descriptor dump;
- exact `/sdcard/Movies/Findroid/...` listing;
- DHU app drawer screenshot;
- Findroid tabs screenshots;
- item detail screenshot;
- player handoff screenshot;
- `services.txt`, `window.txt`, `media-session.txt`;
- short `REPORT.md` with pass/fail and current limitations.
