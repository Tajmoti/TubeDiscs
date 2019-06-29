package com.tajmoti.tubediscs.audio.client;

import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;

public class ActiveRequest extends TimedAudioRequest {
    public final String sourcename;


    public ActiveRequest(TimedAudioRequest request, String sourcename) {
        super(request.dimen, request.pos, request.url, request.timeStarted);
        this.sourcename = sourcename;
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
