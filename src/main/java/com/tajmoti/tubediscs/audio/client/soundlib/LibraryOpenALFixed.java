package com.tajmoti.tubediscs.audio.client.soundlib;

import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystemException;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

import javax.sound.sampled.AudioFormat;
import java.lang.reflect.Field;
import java.nio.FloatBuffer;

@SideOnly(Side.CLIENT)
public class LibraryOpenALFixed extends LibraryLWJGLOpenAL {
    private static Field fieldListenerPositionAL;

    static {
        try {
            fieldListenerPositionAL = LibraryLWJGLOpenAL.class.getDeclaredField("listenerPositionAL");
            fieldListenerPositionAL.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
    }


    public LibraryOpenALFixed() throws SoundSystemException {
        super();
    }

    @Override
    public void rawDataStream(AudioFormat audioFormat, boolean priority, String sourcename, float x, float y, float z, int attModel, float distOrRoll) {
        FloatBuffer listenerPositionAL = null;
        try {
            listenerPositionAL = (FloatBuffer) fieldListenerPositionAL.get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        sourceMap.put(sourcename,
                new SourceOpenALFixed(listenerPositionAL, audioFormat,
                        priority, sourcename, x, y, z,
                        attModel, distOrRoll));
    }
}
