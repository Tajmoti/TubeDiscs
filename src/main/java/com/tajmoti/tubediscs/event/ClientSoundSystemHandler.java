package com.tajmoti.tubediscs.event;

import com.tajmoti.tubediscs.audio.client.soundlib.LibraryOpenALFixed;
import net.minecraftforge.client.event.sound.SoundSetupEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import paulscode.sound.SoundSystemConfig;
import paulscode.sound.SoundSystemException;
import paulscode.sound.libraries.LibraryLWJGLOpenAL;

@SuppressWarnings("unused")
@SideOnly(Side.CLIENT)
public class ClientSoundSystemHandler {

    @SubscribeEvent
    public void onSystemLoad(SoundSetupEvent event) {
        try {
            SoundSystemConfig.removeLibrary(LibraryLWJGLOpenAL.class);
            SoundSystemConfig.addLibrary(LibraryOpenALFixed.class);
        } catch (SoundSystemException e) {
            e.printStackTrace();
        }
    }
}
