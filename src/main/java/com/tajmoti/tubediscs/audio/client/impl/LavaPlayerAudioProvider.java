package com.tajmoti.tubediscs.audio.client.impl;

import com.sedmelluq.discord.lavaplayer.format.AudioDataFormat;
import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame;
import com.tajmoti.tubediscs.audio.client.ActiveRequest;
import com.tajmoti.tubediscs.audio.client.IAudioProvider;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LavaPlayerAudioProvider implements IAudioProvider {
    private final ExecutorService feeders;
    private final Logger logger;
    private final AudioPlayerManager manager;


    public LavaPlayerAudioProvider(Logger logger, AudioDataFormat outputFormat) {
        this.logger = logger;
        this.feeders = Executors.newCachedThreadPool();
        this.manager = new DefaultAudioPlayerManager();
        this.manager.getConfiguration().setOutputFormat(outputFormat);
        this.manager.registerSourceManager(new YoutubeAudioSourceManager());
        this.manager.setUseSeekGhosting(false);
    }

    @Override
    public void fetchAndPlayAsync(ActiveRequest request, long serverTicks, Callback callback) {
        long loadingStart = System.currentTimeMillis();
        manager.loadItem(request.url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                feeders.submit(() -> {
                    request.duration = track.getDuration();
                    fillBuffers(request, track, serverTicks, loadingStart, callback);
                });
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                logger.warn("playlistLoaded()");
                callback.notifyFailed(request);
            }

            @Override
            public void noMatches() {
                logger.warn("noMatches()");
                callback.notifyFailed(request);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                logger.warn("loadFailed()");
                callback.notifyFailed(request);
            }
        });
    }

    private void fillBuffers(ActiveRequest request, AudioTrack track, long ticksNow, long loadingStart, Callback callback) {
        AudioPlayer player = manager.createPlayer();
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
            // Check if the sound should still be playing
            if (request.isCanceled) {
                logger.warn("Playing of {} canceled, breaking from feed loop", request);
                player.destroy();
                break;
            }

            // Calculate the offset
            long skipTicks = ticksNow - request.ticksStarted;
            long skipMillis = (skipTicks / 20) * 1000 + (System.currentTimeMillis() - loadingStart);
            int skipBytes = IAudioProvider.millisToBytes(skipMillis);
            int receivedBytes = frame.getDataLength();
            if (processedBytes > skipBytes) {
                // We are already behind the cut-off point, continue loading normally
                callback.feedBytes(request, frame.getData());
            } else if (skipBytes <= (receivedBytes + processedBytes)) {
                // This portion contains the cut-off point
                skipBytes = skipBytes - processedBytes;

                int portionLength = receivedBytes - skipBytes;
                byte[] portion = new byte[portionLength];
                System.arraycopy(frame.getData(), skipBytes, portion, 0, portionLength);

                callback.feedBytes(request, portion);
            }
            processedBytes += receivedBytes;
        }
    }
}
