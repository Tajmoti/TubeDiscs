package com.tajmoti.tubediscs;

import com.tajmoti.tubediscs.audio.AudioTracker;
import com.tajmoti.tubediscs.audio.client.PositionedAudioPlayer;
import com.tajmoti.tubediscs.audio.client.SoundManagerRefHook;
import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;
import com.tajmoti.tubediscs.event.ClientJukeboxHandler;
import com.tajmoti.tubediscs.event.ClientSoundSystemHandler;
import com.tajmoti.tubediscs.event.RegistryHandler;
import com.tajmoti.tubediscs.event.ServerJukeboxHandler;
import com.tajmoti.tubediscs.gui.GuiHandler;
import com.tajmoti.tubediscs.net.TubePlayMessage;
import com.tajmoti.tubediscs.net.TubeSaveMessage;
import com.tajmoti.tubediscs.net.TubeStopMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.logging.log4j.Logger;
import paulscode.sound.SoundSystem;

@Mod(modid = ModInfo.MODID, name = ModInfo.NAME, version = ModInfo.VERSION)
public class TubeDiscs {
    @Mod.Instance
    private static TubeDiscs INSTANCE;

    /**
     * The mod logger instance, use this instead
     * of System.out or System.err!
     */
    private Logger logger;

    private SimpleNetworkWrapper network;

    @SideOnly(Side.CLIENT)
    private PositionedAudioPlayer audio;
    private AudioTracker<TimedAudioRequest> serverTracker;


    public static TubeDiscs getInstance() {
        return INSTANCE;
    }

    @SideOnly(Side.CLIENT)
    public PositionedAudioPlayer getAudio() {
        return audio;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        network = NetworkRegistry.INSTANCE.newSimpleChannel(ModInfo.MODID);
        serverTracker = new AudioTracker<>(logger);

        // Registers a custom version of the OpenAL library with a bug fixed
        if (event.getSide() == Side.CLIENT)
            MinecraftForge.EVENT_BUS.register(new ClientSoundSystemHandler());

        MinecraftForge.EVENT_BUS.register(new RegistryHandler(logger, network, serverTracker));
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Network messages

        network.registerMessage(TubePlayMessage.Handler.class, TubePlayMessage.class, 0, Side.CLIENT);
        network.registerMessage(TubeStopMessage.Handler.class, TubeStopMessage.class, 1, Side.CLIENT);
        network.registerMessage(TubeSaveMessage.Handler.class, TubeSaveMessage.class, 2, Side.SERVER);

        if (event.getSide() == Side.CLIENT) {
            // GUI
            NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler(logger, network));

            // Sound handlers
            SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
            SoundSystem system = new SoundManagerRefHook(logger, handler).getSoundSystem();

            audio = new PositionedAudioPlayer(logger, system);

            // Stops music on disconnect
            MinecraftForge.EVENT_BUS.register(new ClientJukeboxHandler(audio));
        }
        // Sends cancel play message on block destroy
        MinecraftForge.EVENT_BUS.register(new ServerJukeboxHandler(serverTracker, network));
    }

    @EventHandler
    public void serverStopping(FMLServerStoppingEvent event) {
        serverTracker.removeAllSounds();
    }
}
