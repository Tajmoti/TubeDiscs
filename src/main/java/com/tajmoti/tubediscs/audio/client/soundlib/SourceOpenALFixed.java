package com.tajmoti.tubediscs.audio.client.soundlib;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.openal.AL10;
import paulscode.sound.Channel;
import paulscode.sound.libraries.ChannelLWJGLOpenAL;
import paulscode.sound.libraries.SourceLWJGLOpenAL;

import javax.sound.sampled.AudioFormat;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.FloatBuffer;

@SideOnly(Side.CLIENT)
public class SourceOpenALFixed extends SourceLWJGLOpenAL {
    private static Field fieldChannelOpenAL;
    private static Field fieldSourcePosition;
    private static Method methodResetAllInformation;
    private static Method methodCheckALError;

    static {
        try {
            fieldChannelOpenAL = SourceLWJGLOpenAL.class.getDeclaredField("channelOpenAL");
            fieldSourcePosition = SourceLWJGLOpenAL.class.getDeclaredField("sourcePosition");
            methodResetAllInformation = SourceLWJGLOpenAL.class.getDeclaredMethod("resetALInformation");
            methodCheckALError = SourceLWJGLOpenAL.class.getDeclaredMethod("checkALError");
            fieldChannelOpenAL.setAccessible(true);
            fieldSourcePosition.setAccessible(true);
            methodResetAllInformation.setAccessible(true);
            methodCheckALError.setAccessible(true);
        } catch (NoSuchFieldException | NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public SourceOpenALFixed(FloatBuffer listenerPosition, AudioFormat audioFormat, boolean priority, String sourcename, float x, float y, float z, int attModel, float distOrRoll) {
        super(listenerPosition, audioFormat, priority, sourcename, x, y, z, attModel, distOrRoll);
    }

    @Override
    public int feedRawAudioData(Channel c, byte[] buffer) {
        positionChanged();
        if (channel != c) {
            try {
                fieldChannelOpenAL.set(this, c);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        return super.feedRawAudioData(c, buffer);
    }

    @Override
    public void setPosition(float x, float y, float z) {
        // Instead of super.setPosition(x, y, z);
        // from here
        position.x = x;
        position.y = y;
        position.z = z;
        // to here

        FloatBuffer sourcePosition = null;
        try {
            sourcePosition = (FloatBuffer) fieldSourcePosition.get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        // Make sure OpenAL information has been created
        if (sourcePosition == null) {
            try {
                methodResetAllInformation.invoke(this);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        } else {
            // put the new position information into the buffer:
            sourcePosition.put(0, x);
            sourcePosition.put(1, y);
            sourcePosition.put(2, z);

            positionChanged();
        }
    }

    @Override
    public void positionChanged() {
        super.positionChanged();
        FloatBuffer sourcePosition = null;
        ChannelLWJGLOpenAL channelOpenAL = null;
        try {
            sourcePosition = (FloatBuffer) fieldSourcePosition.get(this);
            channelOpenAL = (ChannelLWJGLOpenAL) fieldChannelOpenAL.get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        // make sure we are assigned to a channel:
        if (channel != null && channel.attachedSource == this &&
                channelOpenAL != null && channelOpenAL.ALSource != null) {
            // move the source:
            AL10.alSource(channelOpenAL.ALSource.get(0), AL10.AL_POSITION,
                    sourcePosition);
            try {
                methodCheckALError.invoke(this);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }
}
