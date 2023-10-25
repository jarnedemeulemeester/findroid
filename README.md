![Findroid banner](images/findroid-banner.png)

# Findroid

Findroid is third-party Android application for Jellyfin that provides a native user interface to browse and play movies and series.

I am developing this application in my spare time.

**This project is in its early stages so expect bugs.**

<a href='https://play.google.com/store/apps/details?id=dev.jdtech.jellyfin'><img alt='Get it on Google Play' src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png' height="80"/></a><a href='http://www.amazon.com/gp/product/B0BTWC8DNZ'><img alt='Available at Amazon Appstore' src='https://user-images.githubusercontent.com/32322857/219019331-027a6775-7362-44bb-a026-281f71e9b37b.png' height="80"/></a><a href='https://appgallery.huawei.com/app/C107646987'><img alt='Explore it on Huawei AppGallery' src='https://user-images.githubusercontent.com/32322857/219013519-f0a11e17-c32c-42fd-8009-ab77fe2c23e7.png' height="80"/></a><a href='https://apt.izzysoft.de/fdroid/index/apk/dev.jdtech.jellyfin'><img alt='Get it on IzzyOnDroid' src='https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png' height="80"/></a>

## Screenshots
| Home                                | Library                             | Movie                           | Season                            | Episode                             |
|-------------------------------------|-------------------------------------|---------------------------------|-----------------------------------|-------------------------------------|
| ![Home](images/home-light-dark.png) | ![Library](images/library-dark.png) | ![Movie](images/movie-dark.png) | ![Season](images/season-dark.png) | ![Episode](images/episode-dark.png) |

## Features
- Completely native interface
- Supported media items: movies, series, seasons, episodes 
  - Direct play only, (no transcoding)
- Offline playback / downloads
- ExoPlayer
  - Video codecs: H.263, H.264, H.265, VP8, VP9, AV1 
    - Support depends on Android device
  - Audio codecs: Vorbis, Opus, FLAC, ALAC, PCM, MP3, AMR-NB, AMR-WB, AAC, AC-3, E-AC-3, DTS, DTS-HD, TrueHD 
    - Support provided by ExoPlayer FFmpeg extension
  - Subtitle codecs: SRT, VTT, SSA/ASS, PGSSUB
    - SSA/ASS has limited styling support see [this issue](https://github.com/google/ExoPlayer/issues/8435)
- mpv
  - Container formats: mkv, mov, mp4, avi
  - Video codecs: H.264, H.265, VP8, VP9, AV1
  - Audio codecs: Opus, FLAC, MP3, AAC, AC-3, E-AC-3, TrueHD, DTS, DTS-HD
  - Subtitle codecs: SRT, VTT, SSA/ASS, DVDSUB
  - Optionally force software decoding when hardware decoding has issues.
- Picture-in-picture mode

## Planned features
- Android TV
- Websocket connection (Syncplay)
- Chromecast support

## Translating
[JDTech Weblate](https://weblate.jdtech.dev) is a selfhosted instance of Weblate where you can translate this project and future projects of mine.

## Questions?
[![](https://dcbadge.vercel.app/api/server/tg5VvTFwTV)](https://discord.gg/tg5VvTFwTV)\
We have a Discord server to discuss future development or ask general questions.

## License
This project is licensed under [GPLv3](LICENSE).

The logo is a combination of the Jellyfin logo and the Android robot.

The Android robot is reproduced or modified from work created and shared by Google and used according to terms described in the Creative Commons 3.0 Attribution License.

Android is a trademark of Google LLC.

Google Play and the Google Play logo are trademarks of Google LLC.
