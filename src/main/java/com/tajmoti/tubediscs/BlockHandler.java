package com.tajmoti.tubediscs;

import com.tajmoti.tubediscs.client.converter.OnlineAudioPlayer;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class BlockHandler {
    private final OnlineAudioPlayer player;


    public BlockHandler(OnlineAudioPlayer player) {
        this.player = player;
    }

    @SubscribeEvent
    public void onBreakEvent(BlockEvent.BreakEvent event) {
        World w = event.getWorld();
        if (w.isRemote) {
            BlockPos pos = event.getPos();
            IBlockState s = w.getBlockState(pos);
            if (s.getBlock() instanceof BlockJukebox) {
                player.cancel(pos);
            }
            event.setResult(Event.Result.ALLOW);
        }
    }
}
