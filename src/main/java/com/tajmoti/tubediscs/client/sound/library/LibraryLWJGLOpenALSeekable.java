package com.tajmoti.tubediscs.client.sound.library;

import com.tajmoti.tubediscs.TubeDiscs;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL10;
import paulscode.sound.*;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;
import paulscode.sound.libraries.SourceLWJGLOpenAL;

import javax.sound.sampled.AudioFormat;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;

@SideOnly(Side.CLIENT)
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

    /**
     * Replacement for loadSound(FilenameURL), this one also has the sourcename
     * parameter so we can find the delay by it.
     */
    private boolean loadSoundWithIdentifier(FilenameURL filenameURL, String sourcename) {
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
        int totalOffsetMillis = seekInfo.getSeekMillis(sourcename);
        buffer.audioData = trimToSeek(buffer, totalOffsetMillis);

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

    @Override
    public void newSource(boolean priority, boolean toStream, boolean toLoop, String sourcename, FilenameURL filenameURL, float x, float y, float z, int attModel, float distOrRoll) {
        HashMap<String, IntBuffer> ALBufferMap = accessor.getBufferMap();
        IntBuffer myBuffer = null;
        if (!toStream) {
            // Grab the sound buffer for this file:
            myBuffer = ALBufferMap.get(filenameURL.getFilename());

            // if not found, try loading it:
            if (myBuffer == null) {
                if (!loadSoundWithIdentifier(filenameURL, sourcename)) {
                    errorMessage("Source '" + sourcename + "' was not created "
                            + "because an error occurred while loading "
                            + filenameURL.getFilename());
                    return;
                }
            }

            // try and grab the sound buffer again:
            myBuffer = ALBufferMap.get(filenameURL.getFilename());
            // see if it was there this time:
            if (myBuffer == null) {
                errorMessage("Source '" + sourcename + "' was not created "
                        + "because a sound buffer was not found for "
                        + filenameURL.getFilename());
                return;
            }
        }
        SoundBuffer buffer = null;

        if (!toStream) {
            // Grab the audio data for this file:
            buffer = bufferMap.get(filenameURL.getFilename());
            // if not found, try loading it:
            if (buffer == null) {
                if (!loadSoundWithIdentifier(filenameURL, sourcename)) {
                    errorMessage("Source '" + sourcename + "' was not created "
                            + "because an error occurred while loading "
                            + filenameURL.getFilename());
                    return;
                }
            }
            // try and grab the sound buffer again:
            buffer = bufferMap.get(filenameURL.getFilename());
            // see if it was there this time:
            if (buffer == null) {
                errorMessage("Source '" + sourcename + "' was not created "
                        + "because audio data was not found for "
                        + filenameURL.getFilename());
                return;
            }
        }

        sourceMap.put(sourcename,
                new SourceLWJGLOpenAL(accessor.getListenerPosition(), myBuffer,
                        priority, toStream, toLoop,
                        sourcename, filenameURL, buffer, x,
                        y, z, attModel, distOrRoll,
                        false));
    }

    private byte[] trimToSeek(SoundBuffer buffer, int totalOffsetMillis) {
        AudioFormat audioFormat = buffer.audioFormat;
        byte[] audioData = buffer.audioData;

        // Trim the data
        if (totalOffsetMillis <= 0) return audioData;

        int frameSize = audioFormat.getFrameSize();
        float totalOffsetBytes = (totalOffsetMillis * audioFormat.getFrameRate() / 1000) * frameSize;
        // Offset must be multiple of a frame!
        int offset = (int) totalOffsetBytes / frameSize * frameSize;

        // Final length
        int dataLen = audioData.length;
        int retLen = dataLen - offset;
        // If the offset is larger than the data,
        // the audio is already over. Do not play anything.
        if (retLen < 0) {
            logger.info("Offset [" + offset + "] larger than data [" + dataLen + "]!");
            return new byte[0];
        }

        byte[] newData = new byte[retLen];
        System.arraycopy(audioData, offset, newData, 0, retLen);
        return newData;
    }

    public interface SeekAmountGetter {
        /**
         * Return >= 0 if we want to skip the bytes, -1 if not.
         * Find it by the track UID assigned in PositionedAudiPlayer.
         */
        int getSeekMillis(String sourcename);
    }
}
