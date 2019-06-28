package com.tajmoti.tubediscs.client.converter;

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
    private static final int DOWNLOAD_TRY_COUNT = 5;

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


    public OnlineAudioPlayer(Logger logger, IVideoDownloader downloader, PositionedAudioPlayer audioPlayer) {
        this.logger = logger;
        this.audioPlayer = audioPlayer;
        this.executor = Executors.newCachedThreadPool();
        this.processorMap = new HashMap<>();
        this.downloader = downloader;
    }

    public synchronized void playVideoAtPos(URL url, BlockPos pos, int offsetMillis) {
        long timeReceived = System.currentTimeMillis();
        audioPlayer.stopAudioAtPos(pos);

        mkdirs();
        Future f = executor.submit(() -> {
            try {
                String id = IVideoDownloader.extractVideoId(url);
                File audio = new File(AUDIO_CACHE, id + ".ogg");
                if (!audio.exists()) {
                    logger.info(url + " does not exist, downloading…");

                    File video = null;
                    for (int i = 0; i < DOWNLOAD_TRY_COUNT; i++) {
                        try {
                            video = downloader.downloadVideo(url, VIDEO_CACHE);
                            break;
                        } catch (IOException e) {
                            logger.info("Download failed, retrying…");
                            if (i == (DOWNLOAD_TRY_COUNT - 1) || Thread.interrupted())
                                throw e;
                        }
                    }

                    logger.info(url + " downloaded, converting…");
                    AudioUtil.convertToOgg(video, audio);
                    logger.info(url + " converted, deleting video file…");
                    if (!video.delete())
                        logger.warn("Failed to delete " + video.toString());
                }
                // Play it
                if (!Thread.interrupted()) {
                    logger.info("Playing " + audio.toString());
                    int finalOffsetMillis = (int) (System.currentTimeMillis() - timeReceived) + offsetMillis;
                    audioPlayer.playAudioAtPos(pos, audio, finalOffsetMillis);
                } else {
                    logger.info("Playback of " + audio.toString() + " canceled!");
                }
            } catch (EncoderException | IOException e) {
                logger.error(e);
            }
        });
        processorMap.put(pos, f);
    }

    public synchronized void cancel(BlockPos pos) {
        audioPlayer.stopAudioAtPos(pos);

        Future task = processorMap.get(pos);
        if (task != null)
            task.cancel(true);
    }

    public synchronized void cancelAll() {
        audioPlayer.stopAudio();
    }

    public static void mkdirs() {
        AUDIO_CACHE.mkdirs();
        VIDEO_CACHE.mkdirs();
    }
}
