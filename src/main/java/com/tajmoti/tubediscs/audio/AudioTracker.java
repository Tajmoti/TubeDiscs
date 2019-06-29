package com.tajmoti.tubediscs.audio;

import net.minecraft.util.math.BlockPos;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AudioTracker<R extends AudioRequest> {
    private Logger logger;
    /**
     * What sound is played where. Added on playback start,
     * removed when the playback is canceled.
     */
    private final List<R> playingSounds;


    public AudioTracker(Logger logger) {
        this.logger = logger;
        this.playingSounds = new ArrayList<>();
    }


    public void addSound(@Nonnull R sound) {
        playingSounds.add(sound);
    }

    public void removeSound(@Nonnull R sound) {
        playingSounds.remove(sound);
    }

    @Nullable
    public R removeSoundAtPos(int dimen, @Nonnull BlockPos pos) {
        R audio = findExistingRequest(dimen, pos);
        if (audio != null) {
            logger.info("Stopping audio at pos " + pos.toString());
            playingSounds.remove(audio);
        }
        return audio;
    }

    public void removeAllSounds() {
        logger.info("Stopping all audio");
        playingSounds.clear();
    }

    @Nonnull
    public List<R> getAllSounds() {
        return playingSounds;
    }

    @Nonnull
    public List<R> getSoundsByDim(int dim) {
        return playingSounds.stream()
                .filter(r -> r.dimen == dim)
                .collect(Collectors.toList());
    }

    @Nullable
    public R findExistingRequest(int dimen, BlockPos pos) {
        for (R request : playingSounds)
            if (request.isMatchingRequest(dimen, pos))
                return request;
        return null;
    }
}
