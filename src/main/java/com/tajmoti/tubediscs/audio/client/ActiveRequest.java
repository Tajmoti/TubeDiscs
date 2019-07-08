package com.tajmoti.tubediscs.audio.client;

import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;

public class ActiveRequest extends TimedAudioRequest {
    /**
     * The {@link paulscode.sound.SoundSystem} sourcename.
     */
    public final String sourcename;
    /**
     * The time when the track has started in RTC ms.
     * {@link System#currentTimeMillis()}.
     */
    public final long timeStarted;
    /**
     * The duration of the track in ms,
     * or -1 if not yet known.
     */
    public volatile long duration;

    /**
     * If set to true, the playing should stop.
     */
    public volatile boolean isCanceled;


    public ActiveRequest(TimedAudioRequest request, String sourcename, long timeStarted) {
        super(request.dimen, request.pos, request.url, request.ticksStarted);
        this.sourcename = sourcename;
        this.timeStarted = timeStarted;
        this.duration = -1;
    }

    @Override
    public String toString() {
        return "ActiveRequest{" +
                "url='" + url + '\'' +
                ", dimen=" + dimen +
                ", pos=" + pos +
                ", sourcename='" + sourcename + '\'' +
                '}';
    }
}
