package com.tajmoti.tubediscs.audio.client;

import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.audio.SoundManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundSystem;

import java.lang.reflect.Field;

@SideOnly(Side.CLIENT)
public class SoundManagerRefHook {
    private final Logger logger;
    private final Field sndManagerField;
    private final Field sndSystemField;
    private final SoundHandler handler;


    public SoundManagerRefHook(Logger logger, SoundHandler handler) {
        this.logger = logger;
        this.handler = handler;
        this.sndManagerField = ObfuscationReflectionHelper.findField(SoundHandler.class, "field_147694_f");
        this.sndSystemField = ObfuscationReflectionHelper.findField(SoundManager.class, "field_148620_e");
        this.sndManagerField.setAccessible(true);
        this.sndSystemField.setAccessible(true);
    }

    public SoundSystem getSoundSystem() {
        SoundManager manager;
        try {
            manager = (SoundManager) sndManagerField.get(handler);
        } catch (IllegalAccessException e) {
            logger.error(e);
            return null;
        }

        try {
            return (SoundSystem) sndSystemField.get(manager);
        } catch (IllegalAccessException e) {
            logger.error(e);
        }
        return null;
    }
}
