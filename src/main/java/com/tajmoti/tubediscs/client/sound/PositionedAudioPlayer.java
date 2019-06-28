package com.tajmoti.tubediscs.client.sound;

import net.minecraft.client.audio.ISound;
import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PositionedAudioPlayer {
    private final Logger logger;
    private final SoundSystem soundSystem;
    private int nexSourceId = 0;
    /**
     * The offset for each "sourcename" is remembered here.
     * LibraryLWJGLOpenALSeekable uses it to play the sound from a certain position.
     */
    private final OffsetTracker offsetTracker;
    /**
     * What sound is played where. Added on playback start,
     * removed when the playback is canceled.
     * The value is the "sourcename" variable (as in MC code).
     */
    private final Map<BlockPos, String> worldAudioMap;


    public PositionedAudioPlayer(Logger logger, SoundSystem ss, OffsetTracker offsetTracker) {
        this.logger = logger;
        this.offsetTracker = offsetTracker;
        this.soundSystem = ss;
        this.worldAudioMap = new HashMap<>();
    }

    public void playAudioAtPos(BlockPos pos, File file, int offsetMillis) throws IOException {
        // Stop the old audio play at the position
        stopAudioAtPos(pos);

        // Sound parameters
        URL url = file.toURI().toURL();
        String fileName = file.getName();
        String sourcename = fileName + nexSourceId++;
        int attType = ISound.AttenuationType.LINEAR.getTypeInt();

        float distOrRoll = 16.0F;
        // The "String identifier" parameter is actually the file name.
        // "String sourcename" parameter is our UID, to which we pass "$filename-$nextId" where nextId is an incrementing number.
        soundSystem.newSource(false, sourcename, url, fileName, false, pos.getX(), pos.getY(), pos.getZ(), attType, distOrRoll);
        // Register the offset in the offsetTracker for this UID,
        // LibraryLWJGLOpenALSeekable will find it there under the UID.
        offsetTracker.setOffset(sourcename, offsetMillis);
        logger.info("Submitting SoundSystem request to play [" + fileName + ";" + sourcename + ";" + offsetMillis);
        soundSystem.play(sourcename);
        // Remove the sound from the cache immediately because we can not re-use it, it is cut up.
        // We will load, modify it and uncache it each time we want to play it.
        soundSystem.unloadSound(fileName);

        // Save the audio ref
        worldAudioMap.put(pos, sourcename);
    }

    public void stopAudioAtPos(BlockPos pos) {
        String sourcename = worldAudioMap.get(pos);
        if (sourcename != null) {
            soundSystem.stop(sourcename);
            soundSystem.removeSource(sourcename);
            worldAudioMap.remove(pos);
        }
    }

    public void stopAudio() {
        worldAudioMap.forEach((pos, s) -> {
            soundSystem.stop(s);
            soundSystem.removeSource(s);
        });
        worldAudioMap.clear();
    }
}
