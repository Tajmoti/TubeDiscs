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

import javax.sound.sampled.AudioFormat;
import java.util.HashMap;
import java.util.Map;
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
     * The value is the "sourcename" variable (as in MC code).
     */
    private final Map<BlockPos, String> worldAudioMap;

    private final AudioFormat mcAudioFormat;
    private final AudioPlayerManager manager;


    public PositionedAudioPlayer(Logger logger, SoundSystem ss) {
        this.logger = logger;
        this.soundSystem = ss;
        this.worldAudioMap = new HashMap<>();
        this.mcAudioFormat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100f, 16, 2, 4, 44100f, false);
        this.manager = new DefaultAudioPlayerManager();
        this.manager.getConfiguration().setOutputFormat(StandardAudioDataFormats.COMMON_PCM_S16_LE);
        this.manager.registerSourceManager(new YoutubeAudioSourceManager());
    }

    public synchronized void playAudioAtPos(String url, BlockPos pos, int offsetMillis) {
        // Stop the old audio play at the position
        stopAudioAtPos(pos);

        // Sound parameters
        String sourcename = UUID.randomUUID().toString();
        int attType = ISound.AttenuationType.LINEAR.getTypeInt();

        float distOrRoll = 16.0F;
        soundSystem.rawDataStream(mcAudioFormat, false, sourcename, pos.getX(), pos.getY(), pos.getZ(), attType, distOrRoll);
        logger.info("Submitting SoundSystem request to play [" + url + ";" + sourcename + ";" + offsetMillis);

        // Save the audio ref
        worldAudioMap.put(pos, sourcename);
        // Fill the buffers and play
        loadPlayFillBuffers(url, pos, sourcename);
    }

    private void loadPlayFillBuffers(String url, BlockPos pos, String sourcename) {
        manager.loadItem(url, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                fillBuffers(sourcename, track, pos);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                logger.warn("playlistLoaded()");
                stopAudioAtPosIfValid(pos, sourcename);
            }

            @Override
            public void noMatches() {
                logger.warn("noMatches()");
                stopAudioAtPosIfValid(pos, sourcename);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                logger.warn("loadFailed()");
                stopAudioAtPosIfValid(pos, sourcename);
            }
        });
    }

    private void fillBuffers(String sourcename, AudioTrack track, BlockPos pos) {
        AudioPlayer player = manager.createPlayer();
        player.playTrack(track);

        AudioFrame frame;
        while (true) {
            try {
                frame = player.provide(8, TimeUnit.SECONDS);
                if (frame instanceof TerminatorAudioFrame || frame == null)
                    break;
                synchronized (PositionedAudioPlayer.this) {
                    if (!sourcename.equals(worldAudioMap.get(pos)))
                        break;
                }
                soundSystem.feedRawAudioData(sourcename, frame.getData());
            } catch (TimeoutException | InterruptedException e) {
                logger.warn(e);
                break;
            }
        }
    }

    public synchronized void stopAudioAtPos(BlockPos pos) {
        String sourcename = worldAudioMap.get(pos);
        if (sourcename != null) {
            logger.info("Stopping audio at pos " + pos.toString());
            soundSystem.stop(sourcename);
            soundSystem.removeSource(sourcename);
            worldAudioMap.remove(pos);
        }
    }

    private synchronized void stopAudioAtPosIfValid(BlockPos pos, String sourcename) {
        String mapSourcename = worldAudioMap.get(pos);
        if (sourcename.equals(mapSourcename)) {
            logger.info("Stopping audio (matching sourcename) at pos " + pos.toString());
            soundSystem.stop(sourcename);
            soundSystem.removeSource(sourcename);
            worldAudioMap.remove(pos);
        }
    }

    public synchronized void stopAllAudio() {
        logger.info("Stopping all audio");
        worldAudioMap.forEach((pos, s) -> {
            soundSystem.stop(s);
            soundSystem.removeSource(s);
        });
        worldAudioMap.clear();
    }
}
