package com.tajmoti.tubediscs.audio.client.soundlib;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulscode.sound.Channel;
import paulscode.sound.libraries.SourceLWJGLOpenAL;

import javax.sound.sampled.AudioFormat;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;

@SideOnly(Side.CLIENT)
public class SourceOpenALFixed extends SourceLWJGLOpenAL {
    private static Field fieldChannelOpenAL;

    static {
        try {
            fieldChannelOpenAL = SourceLWJGLOpenAL.class.getDeclaredField("channelOpenAL");
            fieldChannelOpenAL.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

    public SourceOpenALFixed(FloatBuffer listenerPosition, AudioFormat audioFormat, boolean priority, String sourcename, float x, float y, float z, int attModel, float distOrRoll) {
        super(listenerPosition, audioFormat, priority, sourcename, x, y, z, attModel, distOrRoll);
    }

    @Override
    public int feedRawAudioData(Channel c, byte[] buffer) {
        if (channel != c) {
            try {
                fieldChannelOpenAL.set(this, c);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return super.feedRawAudioData(c, buffer);
    }
}
