package com.tajmoti.tubediscs.client.sound;

import io.netty.util.internal.ThreadLocalRandom;
import net.minecraft.client.audio.ISound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import paulscode.sound.SoundSystem;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class PositionedAudioPlayer {
    private final SoundSystem soundSystem;
    private final Map<BlockPos, PlayInfo> worldAudioMap;
    private final Map<String, Integer> fileNameRefs;


    public PositionedAudioPlayer(SoundSystem ss) {
        this.soundSystem = ss;
        this.worldAudioMap = new HashMap<>();
        this.fileNameRefs = new HashMap<>();
    }

    public void playAudioAtPos(BlockPos pos, File file) throws IOException {
        // Stop the old audio play at the position
        stopAudioAtPos(pos);

        // Sound parameters
        URL url = file.toURI().toURL();
        String uid = MathHelper.getRandomUUID(ThreadLocalRandom.current()).toString();
        String fileName = file.getName();
        int attType = ISound.AttenuationType.LINEAR.getTypeInt();

        // Actually play the sound
        float distOrRoll = 16.0F;
        soundSystem.newSource(false, uid, url, fileName, false, pos.getX(), pos.getY(), pos.getZ(), attType, distOrRoll);
        soundSystem.play(uid);

        // Save the audio ref
        worldAudioMap.put(pos, new PlayInfo(uid, fileName));

        // Ref counter
        Integer refCount = fileNameRefs.get(fileName);
        if (refCount == null) refCount = 0;
        fileNameRefs.put(fileName, ++refCount);
    }

    public void stopAudioAtPos(BlockPos pos) {
        PlayInfo info = worldAudioMap.get(pos);
        if (info != null) {
            soundSystem.stop(info.uid);
            soundSystem.removeSource(info.uid);
            worldAudioMap.remove(pos);

            // Ref counter, guaranteed to be here
            int refs = fileNameRefs.get(info.fileName) - 1;
            if (refs == 0) soundSystem.unloadSound(info.fileName);
            fileNameRefs.put(info.fileName, refs);
        }
    }

    public void stopAudio() {
        worldAudioMap.forEach((pos, s) -> {
            soundSystem.stop(s.uid);
            soundSystem.removeSource(s.uid);
        });
        worldAudioMap.clear();
        // Ref counter
        fileNameRefs.forEach((fileName, refs) -> {
            if (refs > 0) soundSystem.unloadSound(fileName);
        });
        fileNameRefs.clear();
    }


    private static class PlayInfo {
        private final String uid;
        private final String fileName;

        public PlayInfo(String uid, String fileName) {
            this.uid = uid;
            this.fileName = fileName;
        }
    }
}
