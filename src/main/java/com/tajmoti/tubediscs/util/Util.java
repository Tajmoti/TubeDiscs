package com.tajmoti.tubediscs.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public class Util {
    @Nullable
    public static <T extends Item> ItemStack findItemInHands(EntityPlayer player, Class<T> itemClass) {
        for (ItemStack is : player.getHeldEquipment()) {
            Item item = is.getItem();
            if (itemClass.isAssignableFrom(item.getClass()))
                return is;
        }
        return null;
    }
}
