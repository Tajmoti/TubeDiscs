package com.tajmoti.tubediscs.event;

import com.tajmoti.tubediscs.item.TubeDisc;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

public class RegistryHandler {
    private final Logger logger;

    public RegistryHandler(Logger logger) {
        this.logger = logger;
    }


    @SuppressWarnings("unused")
    @SubscribeEvent
    public void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(new TubeDisc(logger));
    }
}
