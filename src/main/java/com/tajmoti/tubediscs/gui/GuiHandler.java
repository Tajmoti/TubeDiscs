package com.tajmoti.tubediscs.gui;

import com.tajmoti.tubediscs.item.TubeDisc;
import com.tajmoti.tubediscs.util.Util;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nullable;

@SideOnly(Side.CLIENT)
public class GuiHandler implements IGuiHandler {
    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        switch (ID) {
            case TubeDiscGui.ID:
                // Check both hands for the disc
                ItemStack disc = Util.findItemInHands(player, TubeDisc.class);
                // This should not happen, just to be sure
                if (disc == null) return null;
                // Open the actual GUI
                return new TubeDiscGui();
            default:
                return null;
        }
    }
}
