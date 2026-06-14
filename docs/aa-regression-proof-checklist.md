# Findroid AA Regression Proof Checklist

## Scope

Acceptance checklist for Findroid Android Auto library/player regressions on branch
`aa-offline-player`.

This checklist is for proof artifacts, not just build success. Every relevant
player/library change should leave screenshots, logs, and file/DB checks that show
the actual behavior in DHU.

## Environment

- Use a normal Android phone AVD or normal Android device.
- Do not use AAOS/Automotive emulator as proof for phone-projection behavior.
- Launch DHU in large/720p mode.
- Capture DHU screenshots at `1280x720` when possible.
- Install the current APK built from the current commit.
- Use real Findroid-downloaded files under `/sdcard/Movies/Findroid/...`.
- Do not use synthetic media folders or invented proof paths.

## Build Gate

- `:app:phone:ktfmtFormat`
- `:app:phone:ktfmtCheck`
- `:app:phone:assembleLibreDebug`
- Release build when the user asks for a release APK.
- Record commit hash, APK path, APK size, and timestamp.

## Data Gate

- Confirm current server/login state.
- Confirm downloaded files exist under the exact Jellyfin-projected path.
- For series proof, verify at least 2 real MP4 episodes plus `cover.jpg`.
- Verify MP4 duration/codecs with `ffprobe` or Android metadata when pulled locally.
- If checking Room, copy `servers`, `servers-wal`, and `servers-shm` together.

## Android Auto Library Proof

Required screenshots:

- Home/root with main entries.
- Offline entry.
- Online entry.
- Offline Movies tab with poster rows when movies exist.
- Offline Series tab showing series rows, not flat episode rows.
- Offline Series -> selected series -> season list.
- Offline Series -> season -> episode list.
- Online Movies tab with poster rows.
- Online Series tab showing series rows.
- Online Series -> selected series -> season list.
- Online Series -> season -> episode list.

Pass criteria:

- No generic `Findroid error` screen.
- Rows have posters when artwork exists.
- Rows do not exceed Android Auto template text constraints.
- Series are not flattened into mixed episode dumps.
- Watched/in-progress/unwatched state is visible where available.

## Player Proof

Required screenshots:

- Episode/movie detail screen with poster.
- Video playing cleanly with controls hidden by default.
- Tap-to-show controls.
- Rewind control visible.
- Play/pause control visible.
- Forward control visible.
- Back control visible.
- Visible timeline/progress line while controls are shown.
- Rewind proof with before/after playback position.
- Forward proof with before/after playback position.
- Video playing after controls auto-hide: clean video, no persistent panel.
- Aspect-ratio proof for at least one 16:9 video and one non-16:9 video.
- Pause state.
- Resume/play state.
- Back returns to the previous list/detail screen.

Required behavior:

- Play starts actual video rendering on DHU, not phone-only UI.
- Pause stops playback.
- Resume continues playback.
- Back releases video surface and does not leave stale frames behind library UI.
- Controls are hidden by default while playing.
- Controls auto-hide after the configured delay.
- Tap shows controls without accidentally pausing playback.
- Rewind seeks backward and clamps at `0`.
- Forward seeks forward and clamps at duration when known.
- Timeline/progress line reflects current playback position and updates after seek.
- Seek proof must include either screenshots with visible time/progress change or logs/dumpsys
  showing current playback position before and after the action.
- Video must preserve source aspect ratio; no horizontal/vertical stretching. Letterboxing or
  pillarboxing is acceptable, cropping requires an explicit product decision and proof.
- Audio focus pauses or ducks the previous media source according to Android focus behavior.

## Online Playback Proof

- Online movie opens a Jellyfin stream in the AA player.
- Online episode opens a Jellyfin stream in the AA player.
- Online series routes through `Series -> Season -> Episode`, not a dead-end container.
- Server errors show a clear message and do not crash the host.

## Offline Playback Proof

- Offline movie opens local public MP4 path.
- Offline episode opens local public MP4 path.
- Offline series routes through `Series -> Season -> Episode`.
- Public `cover.jpg` is shown when present.
- Offline playback works without needing the Jellyfin server for the selected local file.

## Series Behavior Proof

- Series has season folders.
- Episodes inside a season are sorted by episode index.
- Watched/in-progress state is visible.
- Auto-next starts the next episode for online series playback.
- Auto-next starts the next episode for offline series playback.
- Last episode stops cleanly without looping unexpectedly.

## Continue Watching Proof

- Main screen includes `Continue watching` / history entry.
- History shows last 10 resumable items.
- A movie resumes from saved progress.
- A series episode resumes from saved progress.
- Resuming a series still has the same auto-next behavior.
- Progress is saved on pause/back/end.

## Artifact Layout

Use a fresh folder per proof run:

```text
output_to_user/screenshots/<run-name>/
  REPORT.md
  01-...
  02-...
  logcat-...
  dumpsys-...
  db-...
  files-...
```

`REPORT.md` must state:

- commit hash
- APK path
- DHU config
- AVD/device name
- what passed
- what failed
- exact screenshot list
- exact blockers, if any

## Non-Acceptable Proof

- AAOS screenshots for phone-projection behavior.
- Phone-only screenshots for Android Auto behavior.
- Generic media/audio UI presented as video proof.
- Screen casting/mirroring presented as native Findroid/Fermata proof.
- Synthetic folders or copied fake media unless explicitly labeled as demo-only.
- Build success without DHU proof for UI/player changes.
