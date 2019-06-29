package com.tajmoti.tubediscs.client.sound;

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
import com.sedmelluq.discord.lavaplayer.track.playback.TerminatorAudioFrame;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundSystem;

import javax.annotation.Nullable;
import javax.sound.sampled.AudioFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@SideOnly(Side.CLIENT)
public class PositionedAudioPlayer {
    private final Logger logger;
    private final SoundSystem soundSystem;
    /**
     * What sound is played where. Added on playback start,
     * removed when the playback is canceled.
     */
    private final List<ActiveRequest> playingSounds;

    private final AudioFormat mcAudioFormat;
    private final AudioPlayerManager manager;


    public PositionedAudioPlayer(Logger logger, SoundSystem ss) {
        this.logger = logger;
        this.soundSystem = ss;
        this.playingSounds = new ArrayList<>();
        this.mcAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 4, 44100f, false);
        this.manager = new DefaultAudioPlayerManager();
        this.manager.getConfiguration().setOutputFormat(StandardAudioDataFormats.COMMON_PCM_S16_LE);
        this.manager.registerSourceManager(new YoutubeAudioSourceManager());
        this.manager.setUseSeekGhosting(false);
    }

    public synchronized void playAudioAtPos(Request request, int offsetMillis) {
        // Track how long it takes to buffer up and skip the lost time
        long timeReceived = System.currentTimeMillis();
        // Stop the old audio play at the position
        stopAudioAtPos(request.dimen, request.pos);

        // Sound parameters
        String sourcename = UUID.randomUUID().toString();
        int attType = ISound.AttenuationType.LINEAR.getTypeInt();

        float distOrRoll = 16.0F;
        soundSystem.rawDataStream(mcAudioFormat, false, sourcename, request.pos.getX(), request.pos.getY(), request.pos.getZ(), attType, distOrRoll);
        logger.info("Submitting SoundSystem request to play " + request.toString());

        ActiveRequest active = new ActiveRequest(request, sourcename);
        // Save the audio ref
        playingSounds.add(active);
        // Fill the buffers and play
        loadPlayFillBuffers(active, sourcename, offsetMillis, timeReceived);
    }

    public synchronized void stopAudioAtPos(int dimen, BlockPos pos) {
        ActiveRequest activeRequest = findExistingRequest(dimen, pos);
        if (activeRequest != null) {
            logger.info("Stopping audio at pos " + pos.toString());
            soundSystem.stop(activeRequest.sourcename);
            soundSystem.removeSource(activeRequest.sourcename);
            playingSounds.remove(activeRequest);
        }
    }

    private synchronized void stopAudioAtPosIfValid(int dimen, BlockPos pos, String sourcename) {
        ActiveRequest activeRequest = findExistingRequest(dimen, pos);
        if (activeRequest != null && activeRequest.isMatchingRequest(dimen, pos) && activeRequest.sourcename.equals(sourcename)) {
            logger.info("Stopping audio (matching sourcename) at pos " + pos.toString());
            soundSystem.stop(sourcename);
            soundSystem.removeSource(sourcename);
            playingSounds.remove(activeRequest);
        }
    }

    public synchronized void stopAllAudio() {
        logger.info("Stopping all audio");
        playingSounds.forEach((req) -> {
            soundSystem.stop(req.sourcename);
            soundSystem.removeSource(req.sourcename);
        });
        playingSounds.clear();
    }


    @Nullable
    private ActiveRequest findExistingRequest(int dimen, BlockPos pos) {
        for (ActiveRequest request : playingSounds)
            if (request.isMatchingRequest(dimen, pos))
                return request;
        return null;
    }

    private void loadPlayFillBuffers(ActiveRequest request, String sourcename, int offset, long received) {
        manager.loadItem(request.url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                fillBuffers(request, track, offset, received);
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

    private void fillBuffers(ActiveRequest request, AudioTrack track, int offset, long received) {
        AudioPlayer player = manager.createPlayer();
        player.playTrack(track);

        int processedBytes = 0;
        AudioFrame frame;
        while (true) {
            // Load the next frame or quit the loop on no data
            try {
                frame = player.provide(8, TimeUnit.SECONDS);
                if (frame instanceof TerminatorAudioFrame || frame == null)
                    break;
            } catch (TimeoutException | InterruptedException e) {
                logger.warn(e);
                break;
            }
            // Clear up any queued commands
            soundSystem.CommandQueue(null);
            // Check if the sound should still be playing
            synchronized (PositionedAudioPlayer.this) {
                ActiveRequest fromList = findExistingRequest(request.dimen, request.pos);
                if (fromList == null || !fromList.sourcename.equals(request.sourcename)) {
                    logger.warn("Playing of {} canceled, breaking from feed loop", request);
                    player.destroy();
                    break;
                }
            }

            long skipMillis = System.currentTimeMillis() - received + offset;
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

    public static class Request {
        protected final int dimen;
        protected final BlockPos pos;
        protected final String url;

        public Request(int dimen, BlockPos pos, String url) {
            this.dimen = dimen;
            this.pos = pos;
            this.url = url;
        }

        public boolean isMatchingRequest(int dimen, BlockPos pos) {
            return this.dimen == dimen && this.pos.equals(pos);
        }

        @Override
        public String toString() {
            return "Request{" +
                    "url='" + url + '\'' +
                    ", dimen=" + dimen +
                    ", pos=" + pos +
                    '}';
        }
    }

    private static class ActiveRequest extends Request {
        private final String sourcename;

        public ActiveRequest(Request request, String sourcename) {
            super(request.dimen, request.pos, request.url);
            this.sourcename = sourcename;
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
    }
}
