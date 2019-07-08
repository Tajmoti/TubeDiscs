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
import com.tajmoti.tubediscs.audio.client.IAudioProvider;
import com.tajmoti.tubediscs.audio.client.PositionedAudioPlayer;
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
    public void fetchAndPlayAsync(PositionedAudioPlayer.ActiveRequest request) {
        manager.loadItem(request.url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                feeders.submit(() -> {
                    request.duration = track.getDuration();
                    fillBuffers(request, track);
                });
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                logger.warn("playlistLoaded()");
                request.notifyFailed();
            }

            @Override
            public void noMatches() {
                logger.warn("noMatches()");
                request.notifyFailed();
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                logger.warn("loadFailed()");
                request.notifyFailed();
            }
        });
    }

    private void fillBuffers(PositionedAudioPlayer.ActiveRequest request, AudioTrack track) {
        AudioPlayer player = manager.createPlayer();
        player.playTrack(track);

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
            if (!request.trimAndFeedBytes(frame.getData())) {
                logger.warn("Playing of {} canceled, breaking from feed loop", request);
                player.destroy();
                break;
            }
        }
    }
}
