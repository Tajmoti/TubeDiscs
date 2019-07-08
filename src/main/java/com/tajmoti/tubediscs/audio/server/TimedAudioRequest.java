package com.tajmoti.tubediscs.audio.server;

import com.tajmoti.tubediscs.audio.AudioRequest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class TimedAudioRequest extends AudioRequest {
    /**
     * The time when the track was started in ticks.
     * These are the server ticks!
     * {@link World#getTotalWorldTime()}.
     */
    public final long ticksStarted;


    public TimedAudioRequest(int dimen, BlockPos pos, String url, long startTime) {
        super(dimen, pos, url);
        this.ticksStarted = startTime;
    }
}
