package com.tajmoti.tubediscs.event;

import com.tajmoti.tubediscs.audio.AudioTracker;
import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;
import com.tajmoti.tubediscs.item.TubeDisc;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import org.apache.logging.log4j.Logger;

public class RegistryHandler {
    private final Logger logger;
    private final SimpleNetworkWrapper network;
    private final AudioTracker<TimedAudioRequest> audio;


    public RegistryHandler(Logger logger, SimpleNetworkWrapper network, AudioTracker<TimedAudioRequest> audio) {
        this.logger = logger;
        this.network = network;
        this.audio = audio;
    }

    @SuppressWarnings("unused")
    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new TubeDisc(logger, network, audio));
    }
}
