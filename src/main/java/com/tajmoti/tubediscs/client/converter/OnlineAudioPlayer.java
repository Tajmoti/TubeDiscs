package com.tajmoti.tubediscs.client.converter;

import com.tajmoti.tubediscs.client.sound.PositionedAudioPlayer;
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

@SideOnly(Side.CLIENT)
public class OnlineAudioPlayer {
    private final Logger logger;
    private final PositionedAudioPlayer player;

    private final ExecutorService executor;
    private final Map<BlockPos, AudioCacheProcessor> processorMap;


    public OnlineAudioPlayer(Logger logger, PositionedAudioPlayer player) {
        this.logger = logger;
        this.player = player;
        this.executor = Executors.newFixedThreadPool(4);
        this.processorMap = new HashMap<>();
    }

    public void playVideoAtPos(URL video, BlockPos pos) {
        player.stopAudioAtPos(pos);

        AudioCacheProcessor processor = new AudioCacheProcessor(video);
        executor.submit(() -> {
            try {
                File audio = processor.obtainAudio();
                player.playAudioAtPos(pos, audio);
            } catch (EncoderException | IOException e) {
                e.printStackTrace();
            }
        });
        processorMap.put(pos, processor);
    }

    public void cancel(BlockPos pos) {
        AudioCacheProcessor existing = processorMap.get(pos);
        if (existing != null)
            existing.cancel();
        player.stopAudioAtPos(pos);
    }
}
