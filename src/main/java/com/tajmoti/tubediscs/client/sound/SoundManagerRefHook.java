package com.tajmoti.tubediscs.client.sound;

import com.tajmoti.tubediscs.TubeDiscs;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import paulscode.sound.SoundSystem;

import java.lang.reflect.Field;

public class SoundManagerRefHook {
    private final Field sndManagerField;
    private final Field sndSystemField;
    private final SoundHandler handler;


    public SoundManagerRefHook(SoundHandler handler) {
        this.handler = handler;
        this.sndManagerField = ObfuscationReflectionHelper.findField(SoundHandler.class, "sndManager");
        this.sndSystemField = ObfuscationReflectionHelper.findField(SoundManager.class, "sndSystem");
        this.sndManagerField.setAccessible(true);
        this.sndSystemField.setAccessible(true);
    }

    public SoundSystem getSoundSystem() {
        SoundManager manager;
        try {
            manager = (SoundManager) sndManagerField.get(handler);
        } catch (IllegalAccessException e) {
            TubeDiscs.getInstance().getLogger().error(e);
            return null;
        }

        try {
            return (SoundSystem) sndSystemField.get(manager);
        } catch (IllegalAccessException e) {
            TubeDiscs.getInstance().getLogger().error(e);
        }
        return null;
    }
}
