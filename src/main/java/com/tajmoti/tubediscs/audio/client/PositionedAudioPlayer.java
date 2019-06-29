package com.tajmoti.tubediscs.audio.client;

import com.sedmelluq.discord.lavaplayer.format.StandardAudioDataFormats;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.tajmoti.tubediscs.audio.AudioTracker;
import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundSystem;

import javax.sound.sampled.AudioFormat;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SideOnly(Side.CLIENT)
public class PositionedAudioPlayer {
    private final Logger logger;
    private final SoundSystem soundSystem;
    private final AudioTracker<ActiveRequest> tracker;

    private final AudioFormat mcAudioFormat;
    private final AudioPlayerManager manager;


    public PositionedAudioPlayer(Logger logger, SoundSystem ss) {
        super();
        this.logger = logger;
        this.soundSystem = ss;
        this.tracker = new AudioTracker<>(logger);
        this.mcAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 4, 44100f, false);
        this.manager = new DefaultAudioPlayerManager();
        this.manager.getConfiguration().setOutputFormat(StandardAudioDataFormats.COMMON_PCM_S16_LE);
        this.manager.registerSourceManager(new YoutubeAudioSourceManager());
        this.manager.setUseSeekGhosting(false);
    }

    public synchronized void playAudioAtPos(TimedAudioRequest request, long worldTime) {
        // Stop the old audio play at the position
        stopAudioAtPos(request.dimen, request.pos);

        // Sound parameters
        String sourcename = UUID.randomUUID().toString();
        int attType = ISound.AttenuationType.LINEAR.getTypeInt();

        float distOrRoll = 16.0F;
        soundSystem.rawDataStream(mcAudioFormat, false, sourcename, request.pos.getX(), request.pos.getY(), request.pos.getZ(), attType, distOrRoll);
        logger.info("Submitting SoundSystem request to play " + request.toString());
        ActiveRequest active = new ActiveRequest(request, sourcename);
        // Fill the buffers and play
        loadPlayFillBuffers(active, sourcename, worldTime);
        tracker.addSound(active);
    }

    public synchronized void stopAudioAtPos(int dimen, BlockPos pos) {
        ActiveRequest activeRequest = tracker.removeSoundAtPos(dimen, pos);
        if (activeRequest != null) {
            soundSystem.stop(activeRequest.sourcename);
            soundSystem.removeSource(activeRequest.sourcename);
        }
    }

    private synchronized void stopAudioAtPosIfValid(int dimen, BlockPos pos, String sourcename) {
        ActiveRequest activeRequest = tracker.findExistingRequest(dimen, pos);
        if (activeRequest != null && activeRequest.isMatchingRequest(dimen, pos) && activeRequest.sourcename.equals(sourcename)) {
            soundSystem.stop(sourcename);
            soundSystem.removeSource(sourcename);
            tracker.removeSound(activeRequest);
        }
    }

    public void stopAllAudio() {
        for (ActiveRequest request : tracker.getAllSounds()) {
            soundSystem.stop(request.sourcename);
            soundSystem.removeSource(request.sourcename);
        }
        tracker.removeAllSounds();
    }

    private void loadPlayFillBuffers(ActiveRequest request, String sourcename, long worldTicksNow) {
        long loadingStart = System.currentTimeMillis();
        manager.loadItem(request.url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                fillBuffers(request, track, worldTicksNow, System.currentTimeMillis() - loadingStart);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                logger.warn("playlistLoaded()");
                stopAudioAtPosIfValid(request.dimen, request.pos, sourcename);
            }

            @Override
            public void noMatches() {
                logger.warn("noMatches()");
                stopAudioAtPosIfValid(request.dimen, request.pos, sourcename);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                logger.warn("loadFailed()");
                stopAudioAtPosIfValid(request.dimen, request.pos, sourcename);
            }
        });
    }

    private void fillBuffers(ActiveRequest request, AudioTrack track, long ticksNow, long millisWastedLoading) {
        AudioPlayer player = manager.createPlayer();
        player.setVolume((int) (Minecraft.getMinecraft().gameSettings.getSoundLevel(SoundCategory.RECORDS) * 100.0f));
        player.playTrack(track);

        int processedBytes = 0;
        AudioFrame frame;
        while (true) {
            // Load the next frame or quit the loop on no data
            try {
                frame = player.provide(8, TimeUnit.SECONDS);
                if (frame == null || frame.isTerminator())
                    break;
            } catch (TimeoutException | InterruptedException e) {
                logger.warn(e);
                break;
            }
            // Clear up any queued commands
            soundSystem.CommandQueue(null);
            // Check if the sound should still be playing
            synchronized (PositionedAudioPlayer.this) {
                ActiveRequest fromList = tracker.findExistingRequest(request.dimen, request.pos);
                if (fromList == null || !fromList.sourcename.equals(request.sourcename)) {
                    logger.warn("Playing of {} canceled, breaking from feed loop", request);
                    player.destroy();
                    break;
                }
            }
            long skipTicks = ticksNow - request.timeStarted;
            long skipMillis = (skipTicks / 20) * 1000 + millisWastedLoading;
            int skipBytes = millisToBytes(skipMillis);
            int receivedBytes = frame.getDataLength();
            if (processedBytes > skipBytes) {
                // We are already behind the cut-off point, continue loading normally
                soundSystem.feedRawAudioData(request.sourcename, frame.getData());
            } else if (skipBytes <= (receivedBytes + processedBytes)) {
                // This portion contains the cut-off point
                skipBytes = skipBytes - processedBytes;

                int portionLength = receivedBytes - skipBytes;
                byte[] portion = new byte[portionLength];
                System.arraycopy(frame.getData(), skipBytes, portion, 0, portionLength);

                soundSystem.feedRawAudioData(request.sourcename, portion);
            }
            processedBytes += receivedBytes;
        }
    }

    private int millisToBytes(long totalOffsetMillis) {
        int frameSize = mcAudioFormat.getFrameSize();
        float totalOffsetBytes = (totalOffsetMillis * mcAudioFormat.getFrameRate() / 1000) * frameSize;
        // Offset must be multiple of a frame!
        return (int) totalOffsetBytes / frameSize * frameSize;
    }
}