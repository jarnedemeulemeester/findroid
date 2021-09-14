package dev.jdtech.jellyfin.mpv;

import androidx.annotation.StringDef;

@StringDef({
        TrackType.VIDEO,
        TrackType.AUDIO,
        TrackType.SUBTITLE,
})
public @interface TrackType {
    String VIDEO = "video";
    String AUDIO = "audio";
    String SUBTITLE = "sub";
}
