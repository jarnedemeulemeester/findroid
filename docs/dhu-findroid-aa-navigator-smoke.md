# DHU Smoke: Findroid AA Navigator Video

This smoke test verifies the Android Auto `Findroid Navigator` entry on a normal Android phone AVD.
It must show local Findroid offline video rendered inside DHU, not phone `PlayerActivity`, screen mirroring, or Fermata.

## Preconditions

- Use a normal Google Play phone AVD, not AAOS.
- Install `app/phone/build/outputs/apk/libre/debug/phone-libre-x86_64-debug.apk`.
- Grant all-files access for the debug package:

```bash
adb shell appops set dev.jdtech.jellyfin.debug MANAGE_EXTERNAL_STORAGE allow
```

- Verify at least one real Findroid-downloaded file exists:

```bash
adb shell 'ls -l "/sdcard/Movies/Findroid/Сериалы/История любви/1"'
```

Expected proof files are real MP4 files such as `E01 - Pilot.mp4` and `cover.jpg`.

## Start Android Auto DHU

Open Android Auto settings on the phone:

```bash
adb shell am start -a com.google.android.projection.gearhead.SETTINGS
```

From the overflow menu:

1. Enable Developer settings if needed.
2. Enable Unknown sources if needed.
3. Select `Start head unit server`.

Forward DHU:

```bash
adb forward tcp:5277 tcp:5277
```

Run DHU interactively at 720p. Do not run it as a closed-stdin background pipe; that can close the socket and leave DHU at `Waiting for phone...`.

```bash
DISPLAY=:1 "$ANDROID_HOME/extras/google/auto/desktop-head-unit" \
  --config="$ANDROID_HOME/extras/google/auto/config/default_720p.ini" \
  --adb=5277
```

Pass condition in the DHU log:

```text
Phone reported protocol version 1.7
SSL negotiation finished successfully
Verify returned: ok
```

Screenshot pass condition:

```bash
identify path/to/dhu-screenshot.png
```

The result must be `PNG 1280x720`. Smaller default DHU captures are not accepted for approval.

## Approval Matrix

This smoke is not approved unless all four evidence groups are captured from DHU:

1. **Media library online**
   - Open `Findroid Media`.
   - Expected: root screen shows `Offline` and `Online`.
   - Open `Online`.
   - Capture either `Movies` or `Series` with real Jellyfin rows loaded from the server.
2. **Media library offline**
   - Open `Findroid Media`.
   - Open `Offline`.
   - Open `Series`.
   - Expected: downloaded entries from `/sdcard/Movies/Findroid/...` are listed.
3. **Posters in media library**
   - Capture the offline media library where rows have large poster artwork.
   - The poster source must be the downloaded artwork asset, for example `cover.jpg` or private offline artwork.
4. **Hidden playback controls**
   - Open `Findroid Navigator`.
   - Start an offline video.
   - Expected: the video frame fills the DHU content area and no large pane/card remains over the movie.
   - Only small icon actions may remain visible while playing.

Phone-only screenshots, screen casting, synthetic media folders, or AAOS screenshots do not satisfy this smoke.

## Navigate

Use DHU commands from its prompt:

```text
tap 43 441
screenshot /tmp/dhu-appdrawer.png
```

Open `Findroid Navigator`, then choose `Offline` / `Series` / an offline item such as
`E01 - Pilot`.
Press `Play`.

Capture proof:

```text
screenshot /tmp/dhu-navigator-video.png
sleep 5
screenshot /tmp/dhu-navigator-video-progress.png
```

The two screenshots should show different frames from the same video.

## Playback Controls

The smoke must also cover the basic controls on the Navigator video surface:

1. Press `Play` from the item detail screen.
   - Expected: DHU shows the real offline video frame.
   - Expected: no large pane/card remains over the movie.
   - Expected: playback controls are limited to small icon actions.
2. Press `Pause`.
   - Expected: the same Navigator video surface stays open.
   - Expected: the status text changes to `Paused`.
   - Expected: the primary action changes back to `Play`.
3. Press `Back`.
   - Expected: playback is released.
   - Expected: DHU returns to the item detail screen with `Play`.

Reference evidence from the current 720p successful run:

- `17-dhu-media-root-720p.png`: Media library root with `Offline` and `Online`.
- `18-dhu-media-online-movies-720p.png`: Online media library with Jellyfin movie rows.
- `20-dhu-media-offline-series-with-poster-720p.png`: Offline series rows with poster artwork.
- `23-dhu-navigator-e02-detail-720p.png`: Navigator detail for a real Findroid-downloaded item.
- `24-dhu-navigator-e02-playing-hidden-controls-720p.png`: video is playing without the large control pane.
- `25-dhu-navigator-e02-paused-720p.png`: video is paused and `Play` is visible.
- `26-dhu-navigator-back-to-detail-720p.png`: returned to the item detail screen.
- `27-dumpsys-services-findroid.txt`, `28-dumpsys-media-session.txt`, `29-dumpsys-display.txt`, and `31-logcat-tail.txt`: runtime diagnostics from the same run.

## Collect Evidence

```bash
adb shell dumpsys activity services dev.jdtech.jellyfin.debug > services.txt
adb shell dumpsys display > display.txt
adb logcat -d -t 1500 > logcat.txt
```

Expected service evidence:

- `dev.jdtech.jellyfin.car.FindroidNavigationCarAppService` is bound by Gearhead.
- `TemplateNavigationService` owns the active Android Auto virtual display.
- No phone-only `PlayerActivity` is required for Navigator playback.
