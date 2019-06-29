package com.tajmoti.tubediscs.audio.server;

import com.tajmoti.tubediscs.audio.AudioRequest;
import net.minecraft.util.math.BlockPos;

public class TimedAudioRequest extends AudioRequest {
    public final long timeStarted;

    public TimedAudioRequest(int dimen, BlockPos pos, String url, long startTime) {
        super(dimen, pos, url);
        this.timeStarted = startTime;
    }
}
