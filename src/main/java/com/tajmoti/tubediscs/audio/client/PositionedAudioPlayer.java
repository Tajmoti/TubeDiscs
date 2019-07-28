package com.tajmoti.tubediscs.audio.client;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.tajmoti.tubediscs.audio.AudioTracker;
import com.tajmoti.tubediscs.audio.client.impl.LavaPlayerAudioProvider;
import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundSystem;
import paulscode.sound.SoundSystemConfig;

import javax.annotation.concurrent.GuardedBy;
import javax.sound.sampled.AudioFormat;
import java.util.Iterator;
import java.util.UUID;

@SideOnly(Side.CLIENT)
public class PositionedAudioPlayer implements ITickable {
    private static final AudioFormat FORMAT_IN = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
    private static final AudioFormat FORMAT_OUT = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, false);

    private final Logger logger;
    private final Minecraft mc;
    private final SoundSystem soundSystem;

    @GuardedBy("tracker")
    private final AudioTracker<ActiveRequest> tracker;
    private final IAudioProvider player;


    public PositionedAudioPlayer(Logger logger, Minecraft minecraft, SoundSystem ss) {
        this.logger = logger;
        this.mc = minecraft;
        this.soundSystem = ss;
        this.tracker = new AudioTracker<>();
        this.player = new LavaPlayerAudioProvider(logger, StandardAudioDataFormats.COMMON_PCM_S16_LE);
    }


    public void playAudioAtPos(TimedAudioRequest request, long worldTime) {
        // Stop the old audio play at the position
        stopAudioAtPos(request.dimen, request.pos);

        // Sound parameters
        ActiveRequest active = new ActiveRequest(request, UUID.randomUUID().toString(), System.currentTimeMillis(), worldTime);
        // Track the sound
        synchronized (tracker) {
            tracker.addSound(active);
        }

        logger.debug("Submitting SoundSystem request to play " + request.toString());
        soundSystem.rawDataStream(
                FORMAT_OUT,
                false,
                active.sourcename,
                request.pos.getX(),
                request.pos.getY(),
                request.pos.getZ(),
                SoundSystemConfig.ATTENUATION_LINEAR,
                64.0f
        );

        // Fill the buffers and play
        player.fetchAndPlayAsync(active);
    }

    public void stopAudioAtPos(int dimen, BlockPos pos) {
        ActiveRequest activeRequest;
        synchronized (tracker) {
            activeRequest = tracker.removeSoundAtPos(dimen, pos);
        }
        if (activeRequest != null) {
            activeRequest.isStopped = true;
            soundSystem.stop(activeRequest.sourcename);
            soundSystem.removeSource(activeRequest.sourcename);
            logger.debug("{} {} stopped stopAudioAtPos()", dimen, pos.toString());
        } else {
            logger.debug("{} {} not found in stopAudioAtPos()", dimen, pos.toString());
        }
    }

    private void stopAudioAtPosIfValid(int dimen, BlockPos pos, String sourcename) {
        ActiveRequest activeRequest;
        synchronized (tracker) {
            activeRequest = tracker.findExistingRequest(dimen, pos);
        }
        if (activeRequest != null && activeRequest.isMatchingRequest(dimen, pos) && activeRequest.sourcename.equals(sourcename)) {
            activeRequest.isStopped = true;
            soundSystem.stop(activeRequest.sourcename);
            soundSystem.removeSource(sourcename);
            synchronized (tracker) {
                tracker.removeSound(activeRequest);
            }
        } else {
            logger.debug("{} {} {} not found in stopAudioAtPosIfValid()", dimen, pos.toString(), sourcename);
        }
    }

    public void stopAllAudio() {
        synchronized (tracker) {
            for (ActiveRequest request : tracker.getAllSounds()) {
                request.isStopped = true;
                soundSystem.stop(request.sourcename);
                soundSystem.removeSource(request.sourcename);
                logger.debug("{} removed in stopAllAudio()", request.sourcename);
            }
            tracker.removeAllSounds();
        }
    }


    @Override
    public void update() {
        EntityPlayerSP player = mc.player;
        if (player == null) return;

        int dimension = player.dimension;
        synchronized (tracker) {
            // The current volume
            float volume = mc.gameSettings.getSoundLevel(SoundCategory.RECORDS);
            // The current time
            long now = System.currentTimeMillis();

            Iterator<ActiveRequest> it = tracker.getAllSounds().iterator();
            while (it.hasNext()) {
                ActiveRequest r = it.next();

                // If already over, remove it
                if (r.duration != -1 && now > (r.timeStarted + r.duration)) {
                    logger.debug("{} is over, removing it", r.sourcename);
                    r.isStopped = true;
                    soundSystem.stop(r.sourcename);
                    soundSystem.removeSource(r.sourcename);
                    it.remove();
                } else if (r.dimen == dimension) {
                    soundSystem.setVolume(r.sourcename, volume);
                } else {
                    soundSystem.setVolume(r.sourcename, 0.0f);
                }
            }
        }
    }

    public class ActiveRequest extends TimedAudioRequest {
        /**
         * The {@link SoundSystem} sourcename.
         */
        private final String sourcename;
        /**
         * The time in RTC when the client has received the request to start playing this track.
         * Can be well after the track has started playing on the server.
         * {@link System#currentTimeMillis()}.
         */
        private final long timeStarted;
        /**
         * The number of server ticks when the server sent a request
         * to this client to start the track.
         */
        private final long ticksNow;
        /**
         * The duration of the track in ms,
         * or -1 if not yet known.
         */
        public volatile long duration;
        /**
         * If set to true, the playing has stopped,
         * either because the track is over or it was canceled.
         */
        private volatile boolean isStopped;
        /**
         * How many bytes, including the skipped ones,
         * have been fed into this request.
         */
        private int processedBytes;


        private ActiveRequest(TimedAudioRequest request, String sourcename, long timeStarted, long ticksNow) {
            super(request.dimen, request.pos, request.url, request.ticksStarted);
            this.sourcename = sourcename;
            this.timeStarted = timeStarted;
            this.ticksNow = ticksNow;
            this.duration = -1;
            this.isStopped = false;
            this.processedBytes = 0;
        }

        /**
         * Consumes the bytes and if we did not yet reach the seek point,
         * trims the required bytes to seek to the right point.
         * If we do not yet have enough bytes, the buffer might be skipped entirely.
         * <p>
         * Returns true if we need more bytes, false if bytes will no longer be accepted.
         */
        public boolean trimAndFeedBytes(byte[] buffer) {
            if (isStopped)
                return false;

            // Calculate the offset
            long skipTicks = ticksNow - ticksStarted;
            long skipMillis = (skipTicks / 20) * 1000 + (System.currentTimeMillis() - timeStarted);
            int skipBytes = millisToBytes(skipMillis);
            int receivedBytes = buffer.length;
            if (processedBytes > skipBytes) {
                // We are already behind the cut-off point, continue loading normally
                feedBytes(buffer);
            } else if (skipBytes <= (receivedBytes + processedBytes)) {
                // This portion contains the cut-off point
                skipBytes = skipBytes - processedBytes;

                int portionLength = receivedBytes - skipBytes;
                byte[] portion = new byte[portionLength];
                System.arraycopy(buffer, skipBytes, portion, 0, portionLength);

                feedBytes(portion);
            }
            processedBytes += receivedBytes;
            return true;
        }

        private void feedBytes(byte[] buffer) {
            soundSystem.feedRawAudioData(sourcename, stereoToMono(buffer));
            soundSystem.CommandQueue(null);
        }

        public void notifyFailed() {
            isStopped = true;
            stopAudioAtPosIfValid(dimen, pos, sourcename);
        }
    }

    /**
     * Calculates the byte offset into the PCM sound
     * from the milliseconds to skip.
     */
    private static int millisToBytes(long totalOffsetMillis) {
        int frameSize = FORMAT_IN.getFrameSize();
        float totalOffsetBytes = (totalOffsetMillis * FORMAT_IN.getFrameRate() / 1000) * frameSize;
        // Offset must be multiple of a frame!
        return (int) totalOffsetBytes / frameSize * frameSize;
    }

    /**
     * Averages two samples and combines them into one.
     */
    private static byte[] stereoToMono(byte[] buffer) {
        byte[] mono = new byte[buffer.length / 2];
        for (int i = 0; i < buffer.length / 4; i++) {
            short sampleOne = (short) (((buffer[i * 4 + 1] & 0xFF) << 8) | (buffer[i * 4] & 0xFF));
            short sampleTwo = (short) (((buffer[i * 4 + 3] & 0xFF) << 8) | (buffer[i * 4 + 2] & 0xFF));

            int sampleMono = (sampleOne + sampleTwo) / 2;

            mono[i * 2] = (byte) sampleMono;
            mono[i * 2 + 1] = (byte) (sampleMono >> 8);
        }
        return mono;
    }
}