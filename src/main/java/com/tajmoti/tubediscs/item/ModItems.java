package com.tajmoti.tubediscs.item;

import com.tajmoti.tubediscs.ModInfo;
import com.tajmoti.tubediscs.TubeDiscs;
import net.minecraft.item.Item;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Mod.EventBusSubscriber(modid = ModInfo.MODID)
public class ModItems {
    public static Item TUBEDISC;


    public static void init() {
        TUBEDISC = new TubeDisc(TubeDiscs.getInstance().getLogger());
    }

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().registerAll(TUBEDISC);
    }
}
