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
    private final IAudioProvider.Callback callback = new IAudioProvider.Callback() {
        @Override
        public void feedBytes(ActiveRequest request, byte[] buffer) {
            if (!request.isCanceled)
                soundSystem.feedRawAudioData(request.sourcename, buffer);
        }

        @Override
        public void notifyFailed(ActiveRequest request) {
            request.isCanceled = true;
            stopAudioAtPosIfValid(request.dimen, request.pos, request.sourcename);
        }
    };

    public PositionedAudioPlayer(Logger logger, Minecraft minecraft, SoundSystem ss) {
        this.logger = logger;
        this.mc = minecraft;
        this.soundSystem = ss;
        this.tracker = new AudioTracker<>(logger);
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
        ActiveRequest active = new ActiveRequest(request, sourcename, System.currentTimeMillis());
        // Fill the buffers and play
        player.fetchAndPlayAsync(active, worldTime, callback);

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
            soundSystem.stop(activeRequest.sourcename);
            soundSystem.removeSource(activeRequest.sourcename);
        }
    }

    private void stopAudioAtPosIfValid(int dimen, BlockPos pos, String sourcename) {
        ActiveRequest activeRequest;
        synchronized (tracker) {
            activeRequest = tracker.findExistingRequest(dimen, pos);
        }
        if (activeRequest != null && activeRequest.isMatchingRequest(dimen, pos) && activeRequest.sourcename.equals(sourcename)) {
            soundSystem.stop(sourcename);
            soundSystem.removeSource(sourcename);
            synchronized (tracker) {
                tracker.removeSound(activeRequest);
            }
        }
    }

    public void stopAllAudio() {
        synchronized (tracker) {
            for (ActiveRequest request : tracker.getAllSounds()) {
                soundSystem.stop(request.sourcename);
                soundSystem.removeSource(request.sourcename);
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
}