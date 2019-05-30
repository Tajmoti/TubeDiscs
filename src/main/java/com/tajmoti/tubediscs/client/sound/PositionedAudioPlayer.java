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
    private final Map<BlockPos, String> worldAudioMap;


    public PositionedAudioPlayer(SoundSystem ss) {
        this.soundSystem = ss;
        this.worldAudioMap = new HashMap<>();
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
        soundSystem.newStreamingSource(false, uid, url, fileName, false, pos.getX(), pos.getY(), pos.getZ(), attType, distOrRoll);
        soundSystem.play(uid);

        // Save the audio ref
        worldAudioMap.put(pos, uid);
    }

    public void stopAudioAtPos(BlockPos pos) {
        String existing = worldAudioMap.get(pos);
        if (existing != null) {
            soundSystem.stop(existing);
            worldAudioMap.remove(pos);
        }
    }

    public void stopAudio() {
        worldAudioMap.forEach((pos, s) -> soundSystem.stop(s));
        worldAudioMap.clear();
    }
}
