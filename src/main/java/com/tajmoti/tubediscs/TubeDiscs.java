package com.tajmoti.tubediscs;

import com.tajmoti.tubediscs.client.sound.PositionedAudioPlayer;
import com.tajmoti.tubediscs.client.sound.SoundManagerRefHook;
import com.tajmoti.tubediscs.client.gui.GuiHandler;
import com.tajmoti.tubediscs.client.converter.OnlineAudioPlayer;
import com.tajmoti.tubediscs.item.ModItems;
import com.tajmoti.tubediscs.net.TubePlayMessage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
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
    private OnlineAudioPlayer audio;


    public static TubeDiscs getInstance() {
        return INSTANCE;
    }

    public Logger getLogger() {
        return logger;
    }

    public SimpleNetworkWrapper getNetwork() {
        return network;
    }

    public OnlineAudioPlayer getAudio() {
        return audio;
    }

    @EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        logger = event.getModLog();
        ModItems.init();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // Network messages
        network = NetworkRegistry.INSTANCE.newSimpleChannel(ModInfo.MODID);
        network.registerMessage(TubePlayMessage.Handler.class, TubePlayMessage.class, 0, Side.CLIENT);

        // GUI
        NetworkRegistry.INSTANCE.registerGuiHandler(this, new GuiHandler());

        // Sound handlers
        SoundHandler handler = Minecraft.getMinecraft().getSoundHandler();
        SoundSystem system = new SoundManagerRefHook(handler).getSoundSystem();

        PositionedAudioPlayer pp = new PositionedAudioPlayer(system);
        audio = new OnlineAudioPlayer(logger, pp);

        // Block handler
        MinecraftForge.EVENT_BUS.register(new BlockHandler(audio));
    }
}
