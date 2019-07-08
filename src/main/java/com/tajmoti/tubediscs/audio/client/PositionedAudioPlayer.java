package com.tajmoti.tubediscs.audio.client;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.tajmoti.tubediscs.audio.AudioTracker;
import com.tajmoti.tubediscs.audio.client.impl.LavaPlayerAudioProvider;
import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundSystem;

import javax.annotation.concurrent.GuardedBy;
import java.util.Iterator;
import java.util.UUID;

@SideOnly(Side.CLIENT)
public class PositionedAudioPlayer implements ITickable {
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
        String sourcename = UUID.randomUUID().toString();
        int attType = ISound.AttenuationType.LINEAR.getTypeInt();

        float distOrRoll = 16.0F * 4.0F;
        soundSystem.rawDataStream(IAudioProvider.MC_AUDIO_FORMAT, false, sourcename,
                request.pos.getX(), request.pos.getY(), request.pos.getZ(), attType, distOrRoll);
        logger.info("Submitting SoundSystem request to play " + request.toString());
        ActiveRequest active = new ActiveRequest(request, sourcename, System.currentTimeMillis(), worldTime);
        // Fill the buffers and play
        player.fetchAndPlayAsync(active);

        // Track the sound
        synchronized (tracker) {
            tracker.addSound(active);
        }
    }

    public void stopAudioAtPos(int dimen, BlockPos pos) {
        ActiveRequest activeRequest;
        synchronized (tracker) {
            activeRequest = tracker.removeSoundAtPos(dimen, pos);
        }
        if (activeRequest != null) {
            activeRequest.isCanceled = true;
            soundSystem.stop(activeRequest.sourcename);
            soundSystem.removeSource(activeRequest.sourcename);
            logger.warn("{} {} stopped stopAudioAtPos()", dimen, pos.toString());
        } else {
            logger.warn("{} {} not found in stopAudioAtPos()", dimen, pos.toString());
        }
    }

    private void stopAudioAtPosIfValid(int dimen, BlockPos pos, String sourcename) {
        ActiveRequest activeRequest;
        synchronized (tracker) {
            activeRequest = tracker.findExistingRequest(dimen, pos);
        }
        if (activeRequest != null && activeRequest.isMatchingRequest(dimen, pos) && activeRequest.sourcename.equals(sourcename)) {
            activeRequest.isCanceled = true;
            soundSystem.stop(activeRequest.sourcename);
            soundSystem.removeSource(sourcename);
            synchronized (tracker) {
                tracker.removeSound(activeRequest);
            }
        } else {
            logger.warn("{} {} {} not found in stopAudioAtPosIfValid()", dimen, pos.toString(), sourcename);
        }
    }

    public void stopAllAudio() {
        synchronized (tracker) {
            for (ActiveRequest request : tracker.getAllSounds()) {
                request.isCanceled = true;
                soundSystem.stop(request.sourcename);
                soundSystem.removeSource(request.sourcename);
                logger.info("{} removed in stopAllAudio()", request.sourcename);
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
                    logger.info("{} is over, removing it", r.sourcename);
                    soundSystem.stop(r.sourcename);
                    soundSystem.removeSource(r.sourcename);
                    it.remove();
                    continue;
                }

                // If we are in the dimension,
                if (r.dimen == dimension) {
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
        private long ticksNow;
        /**
         * The duration of the track in ms,
         * or -1 if not yet known.
         */
        public volatile long duration;
        /**
         * If set to true, the playing should stop.
         */
        private volatile boolean isCanceled;
        /**
         * How many bytes, including the skipped ones,
         * have been fed into this request.
         */
        private int processedBytes;


        private ActiveRequest(TimedAudioRequest request, String sourcename, long timeStarted, long ticksNow) {
            super(request.dimen, request.pos, request.url, request.ticksStarted);
            this.sourcename = sourcename;
            this.timeStarted = timeStarted;
            this.duration = -1;
            this.ticksNow = ticksNow;
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

        /**
         * Consumes the bytes and if we did not yet reach the seek point,
         * trims the required bytes to seek to the right point.
         * If we do not yet have enough bytes, the buffer might be skipped entirely.
         * <p>
         * Returns true if we need more bytes, false if bytes will no longer be accepted.
         */
        public boolean trimAndFeedBytes(byte[] buffer) {
            if (isCanceled)
                return false;

            // Calculate the offset
            long skipTicks = ticksNow - ticksStarted;
            long skipMillis = (skipTicks / 20) * 1000 + (System.currentTimeMillis() - timeStarted);
            int skipBytes = IAudioProvider.millisToBytes(skipMillis);
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
            soundSystem.feedRawAudioData(sourcename, buffer);
        }

        public void notifyFailed() {
            isCanceled = true;
            stopAudioAtPosIfValid(dimen, pos, sourcename);
        }
    }
}