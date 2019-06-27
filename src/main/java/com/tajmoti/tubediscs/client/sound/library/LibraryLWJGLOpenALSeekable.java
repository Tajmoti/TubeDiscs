package com.tajmoti.tubediscs.client.sound.library;

import com.tajmoti.tubediscs.TubeDiscs;
import com.tajmoti.tubediscs.client.sound.codec.CodecJOrbisSeekable;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import paulscode.sound.*;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

import javax.sound.sampled.AudioFormat;
import java.lang.reflect.Field;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

public class LibraryLWJGLOpenALSeekable extends LibraryLWJGLOpenAL {
    private final Logger logger;
    private final SeekAmountGetter seekInfo;
    private final LibraryLWJGLOpenALFieldAccessor accessor;


    public LibraryLWJGLOpenALSeekable() throws SoundSystemException {
        super();
        TubeDiscs mod = TubeDiscs.getInstance();
        this.logger = mod.getLogger();
        this.seekInfo = mod.getSeekTracker();
        try {
            this.accessor = new LibraryLWJGLOpenALFieldAccessor(logger, this);
        } catch (NoSuchFieldException e) {
            throw (SoundSystemException) new SoundSystemException("Failed to create library instance").initCause(e);
        }
    }

    @Override
    public boolean loadSound(FilenameURL filenameURL) {
        HashMap<String, IntBuffer> ALBufferMap = accessor.getBufferMap();

        // Make sure the buffer map exists:
        if (bufferMap == null) {
            bufferMap = new HashMap<String, SoundBuffer>();
            importantMessage("Buffer Map was null in method 'loadSound'");
        }
        // Make sure the OpenAL buffer map exists:
        if (ALBufferMap == null) {
            ALBufferMap = new HashMap<String, IntBuffer>();
            accessor.setBufferMap(ALBufferMap);
            importantMessage("Open AL Buffer Map was null in method" +
                    "'loadSound'");
        }

        // make sure they gave us a filename:
        if (errorCheck(filenameURL == null,
                "Filename/URL not specified in method 'loadSound'"))
            return false;

        // check if it is already loaded:
        if (bufferMap.get(filenameURL.getFilename()) != null)
            return true;

        ICodec codec = SoundSystemConfig.getCodec(filenameURL.getFilename());
        if (codec instanceof CodecJOrbisSeekable) {
            ((CodecJOrbisSeekable) codec).setOffset(seekInfo.getAmountToSeek(filenameURL));
        }

        if (errorCheck(codec == null, "No codec found for file '" +
                filenameURL.getFilename() +
                "' in method 'loadSound'"))
            return false;
        codec.reverseByteOrder(true);

        URL url = filenameURL.getURL();
        if (errorCheck(url == null, "Unable to open file '" +
                filenameURL.getFilename() +
                "' in method 'loadSound'"))
            return false;

        codec.initialize(url);
        SoundBuffer buffer = codec.readAll();
        codec.cleanup();
        codec = null;
        if (errorCheck(buffer == null,
                "Sound buffer null in method 'loadSound'"))
            return false;

        bufferMap.put(filenameURL.getFilename(), buffer);

        AudioFormat audioFormat = buffer.audioFormat;
        int soundFormat = 0;
        if (audioFormat.getChannels() == 1) {
            if (audioFormat.getSampleSizeInBits() == 8) {
                soundFormat = AL10.AL_FORMAT_MONO8;
            } else if (audioFormat.getSampleSizeInBits() == 16) {
                soundFormat = AL10.AL_FORMAT_MONO16;
            } else {
                errorMessage("Illegal sample size in method 'loadSound'");
                return false;
            }
        } else if (audioFormat.getChannels() == 2) {
            if (audioFormat.getSampleSizeInBits() == 8) {
                soundFormat = AL10.AL_FORMAT_STEREO8;
            } else if (audioFormat.getSampleSizeInBits() == 16) {
                soundFormat = AL10.AL_FORMAT_STEREO16;
            } else {
                errorMessage("Illegal sample size in method 'loadSound'");
                return false;
            }
        } else {
            errorMessage("File neither mono nor stereo in method " +
                    "'loadSound'");
            return false;
        }

        IntBuffer intBuffer = BufferUtils.createIntBuffer(1);
        AL10.alGenBuffers(intBuffer);
        if (errorCheck(AL10.alGetError() != AL10.AL_NO_ERROR,
                "alGenBuffers error when loading " +
                        filenameURL.getFilename()))
            return false;

//        AL10.alBufferData( intBuffer.get( 0 ), soundFormat,
//                           ByteBuffer.wrap( buffer.audioData ),
//                           (int) audioFormat.getSampleRate() );
        AL10.alBufferData(intBuffer.get(0), soundFormat,
                (ByteBuffer) BufferUtils.createByteBuffer(
                        buffer.audioData.length).put(
                        buffer.audioData).flip(),
                (int) audioFormat.getSampleRate());

        if (errorCheck(AL10.alGetError() != AL10.AL_NO_ERROR,
                "alBufferData error when loading " +
                        filenameURL.getFilename()))


            if (errorCheck(intBuffer == null,
                    "Sound buffer was not created for " +
                            filenameURL.getFilename()))
                return false;

        ALBufferMap.put(filenameURL.getFilename(), intBuffer);

        return true;
    }

    public interface SeekAmountGetter {
        /**
         * Return >= 0 if we want to skip the bytes, -1 if not.
         */
        int getAmountToSeek(FilenameURL filenameURL);
    }
}
