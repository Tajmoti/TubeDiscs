package com.tajmoti.tubediscs.event;

import com.tajmoti.tubediscs.client.sound.library.LibraryLWJGLOpenALSeekable;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;


@SuppressWarnings("unused")
@SideOnly(Side.CLIENT)
public class ClientSoundLibReplacer {
    private final Logger logger;

    public ClientSoundLibReplacer(Logger logger) {
        this.logger = logger;
    }

    @SubscribeEvent
    public void onSoundSetup(SoundSetupEvent event) {
        try {
            SoundSystemConfig.removeLibrary(LibraryLWJGLOpenAL.class);
            SoundSystemConfig.addLibrary(LibraryLWJGLOpenALSeekable.class);
            logger.info("Registered custom codec and library");
        } catch (SoundSystemException e) {
            logger.error(e);
        }
    }
}
