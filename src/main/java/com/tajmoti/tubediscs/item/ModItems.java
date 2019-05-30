package com.tajmoti.tubediscs.item;

import com.tajmoti.tubediscs.ModInfo;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ModInfo.MODID)
public class ModItems {
    public static Item TUBEDISC;


    public static void init() {
        TUBEDISC = new TubeDisc();
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(TUBEDISC);
    }
}
