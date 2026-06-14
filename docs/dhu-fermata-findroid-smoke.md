# DHU + Fermata smoke for Findroid public downloads

This smoke proves that Findroid-downloaded public files are visible to Fermata
inside Android Auto DHU, and that Fermata can render a real video frame from
those files.

## Scope

Valid proof:

- ordinary phone AVD, not AAOS;
- Findroid creates files under the real projected path;
- no manual `adb push`, `cp`, synthetic media, or alternate proof folders;
- Fermata opens the exact Findroid folder in DHU;
- screenshots show both folder listing and a real video frame;
- component dumps confirm Fermata `CarService` / `MainCarActivity`;
- `MirrorService` and `MirrorServiceFS` are not active.

Not valid proof:

- phone-only Fermata screenshots;
- AAOS screenshots;
- composed images;
- `FermataMediaService` / Android Auto Now Playing alone;
- mirror, FS mirror, screen casting, or any seeded test folder.

## Environment

Known working setup:

- AVD: ordinary Google Play phone image with `x86_64,arm64-v8a`;
- Android Auto / Gearhead: full APK, not only the SDK stub;
- DHU from `$ANDROID_HOME/extras/google/auto/desktop-head-unit`;
- Fermata Auto official release APK;
- Findroid-AA debug APK installed with all-files access.

Use the same device for Findroid download and DHU proof.

## Prepare device

```bash
export ANDROID_HOME=/home/vadv/Android/Sdk
export ANDROID_AVD_HOME=/home/vadv/.config/.android/avd
export ADB="$ANDROID_HOME/platform-tools/adb"
export DEVICE=emulator-5554

$ADB -s "$DEVICE" devices
$ADB -s "$DEVICE" shell appops set dev.jdtech.jellyfin.debug MANAGE_EXTERNAL_STORAGE allow
$ADB -s "$DEVICE" shell appops get dev.jdtech.jellyfin.debug MANAGE_EXTERNAL_STORAGE
```

## Download through Findroid

Use the Findroid UI. Do not seed files manually.

Target item for this smoke:

```text
Сериалы / История любви / Сезон 1
```

Expected projected path:

```text
/sdcard/Movies/Findroid/Сериалы/История любви/1
```

Verify exact files before launching DHU:

```bash
$ADB -s "$DEVICE" shell 'ls -la "/sdcard/Movies/Findroid/Сериалы/История любви/1"'
$ADB -s "$DEVICE" shell 'df -h /sdcard'
```

For a full-season smoke the folder should contain `E01` ... `E09` plus
`cover.jpg`. A partial smoke is acceptable only when the report says so
explicitly, for example `5/9 due to full AVD storage`.

Verify durations by pulling read-only copies, or by another deterministic media
probe. Do not rely only on Android Auto row duration, because Fermata can show
`00:00` for some rows before playback.

```bash
mkdir -p /tmp/findroid-dhu-proof
$ADB -s "$DEVICE" pull \
  "/sdcard/Movies/Findroid/Сериалы/История любви/1/E01 - Pilot.mp4" \
  /tmp/findroid-dhu-proof/E01.mp4
ffprobe -v error -show_entries format=duration,size \
  -show_streams -of json /tmp/findroid-dhu-proof/E01.mp4
```

## Start Android Auto DHU

Enable Android Auto developer mode, Unknown sources, and Start head unit server
from the phone UI.

```bash
$ADB -s "$DEVICE" forward tcp:5277 tcp:5277
DISPLAY=:95 "$ANDROID_HOME/extras/google/auto/desktop-head-unit"
```

Keep DHU attached to a live terminal. Do not start it as a detached process with
closed stdin.

## Fermata proof steps

1. Open the Android Auto app drawer in DHU.
2. Select the blue native Fermata entry.
3. In Fermata, add/open the exact Findroid folder through the folder/content
   picker.
4. Capture the folder list showing real Findroid files.
5. Open one real episode.
6. Capture a DHU screenshot with a video frame.
7. Capture component dumps immediately after the frame.

Useful dumps:

```bash
$ADB -s "$DEVICE" shell dumpsys activity services > services.txt
$ADB -s "$DEVICE" shell dumpsys window > window.txt
$ADB -s "$DEVICE" shell dumpsys display > display.txt
$ADB -s "$DEVICE" shell dumpsys media_session > media-session.txt
```

Proof conditions:

- `services.txt` contains `me.aap.fermata.auto.CarService`;
- `display.txt` or `window.txt` contains
  `me.aap.fermata.auto.MainCarActivity`;
- `media-session.txt` contains `FermataMediaService` with `state=PLAYING`;
- `services.txt` does not show active `MirrorService` or `MirrorServiceFS`;
- screenshots show the exact Findroid files and a real video frame.

## Storage failure check

If Findroid pauses with storage errors, verify disk state:

```bash
$ADB -s "$DEVICE" shell 'df -h /sdcard'
$ADB -s "$DEVICE" shell 'du -ah /sdcard/Movies/Findroid | sort -h | tail -40'
```

Expected user-facing error for full storage is:

```text
Not enough free storage for this download.
```

`Movies/Findroid storage is unavailable` means the app classified a full-storage
failure incorrectly as `StorageRootUnavailable`.

## Artifact checklist

Save these into one artifact folder:

- exact path `ls`;
- `df -h /sdcard`;
- ffprobe JSON for downloaded files;
- Findroid UI screenshot after download;
- DHU connected screenshot;
- Fermata folder listing screenshot;
- video-frame screenshot;
- `services.txt`;
- `window.txt`;
- `display.txt`;
- `media-session.txt`;
- short `REPORT.md` with what is proven and what is not.
