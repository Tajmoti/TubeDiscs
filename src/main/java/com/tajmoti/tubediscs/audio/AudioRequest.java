package com.tajmoti.tubediscs.audio;

import net.minecraft.util.math.BlockPos;

public class AudioRequest {
    public final int dimen;
    public final BlockPos pos;
    public final String url;

    public AudioRequest(int dimen, BlockPos pos, String url) {
        this.dimen = dimen;
        this.pos = pos;
        this.url = url;
    }

    public boolean isMatchingRequest(int dimen, BlockPos pos) {
        return this.dimen == dimen && this.pos.equals(pos);
    }

    @Override
    public String toString() {
        return "AudioRequest{" +
                "url='" + url + '\'' +
                ", dimen=" + dimen +
                ", pos=" + pos +
                '}';
    }
}