package com.tajmoti.tubediscs.client.converter;

import com.sapher.youtubedl.YoutubeDL;
import com.tajmoti.tubediscs.client.sound.PositionedAudioPlayer;
import com.tajmoti.tubediscs.util.AudioUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import ws.schild.jave.EncoderException;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@SideOnly(Side.CLIENT)
public class OnlineAudioPlayer {
    private static final File AUDIO_CACHE = new File("tubediscs/audio");
    private static final File VIDEO_CACHE = new File("tubediscs/video");

    private final Logger logger;
    private final PositionedAudioPlayer audioPlayer;

    /**
     * Implementation for a video downloader,
     * downloads a single video at a time.
     */
    private final IVideoDownloader downloader;
    /**
     * Executor service scheduling multiple
     * possible downloads onto threads.
     */
    private final ExecutorService executor;
    private final Map<BlockPos, Future> processorMap;


    public OnlineAudioPlayer(Logger logger, PositionedAudioPlayer audioPlayer) {
        this.logger = logger;
        this.audioPlayer = audioPlayer;
        this.executor = Executors.newFixedThreadPool(4);
        this.processorMap = new HashMap<>();
        YoutubeDL.setExecutablePath("C:\\bin\\youtube-dl.exe");
        this.downloader = new YoutubeDlVideoDownloader();
    }

    public void playVideoAtPos(URL url, BlockPos pos) {
        audioPlayer.stopAudioAtPos(pos);

        mkdirs();
        Future f = executor.submit(() -> {
            try {
                String id = downloader.extractVideoId(url);
                File audio = new File(AUDIO_CACHE, id + ".ogg");
                if (!audio.exists()) {
                    File video = downloader.downloadVideo(url, VIDEO_CACHE);
                    AudioUtil.convertToOgg(video, audio);
                    video.delete();
                }
                // Play it
                if (!Thread.interrupted())
                    audioPlayer.playAudioAtPos(pos, audio);
            } catch (EncoderException | IOException e) {
                logger.error(e);
            }
        });
        processorMap.put(pos, f);
    }

    public void cancel(BlockPos pos) {
        audioPlayer.stopAudioAtPos(pos);

        Future task = processorMap.get(pos);
        if (task != null)
            task.cancel(true);
    }

    public void cancelAll() {
        audioPlayer.stopAudio();
    }

    private static void mkdirs() {
        AUDIO_CACHE.mkdirs();
        VIDEO_CACHE.mkdirs();
    }
}
