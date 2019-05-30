package com.tajmoti.tubediscs;

import com.tajmoti.tubediscs.client.converter.OnlineAudioPlayer;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@SuppressWarnings("unused")
public class JukeboxHandler {
    private final OnlineAudioPlayer player;


    public JukeboxHandler(OnlineAudioPlayer player) {
        this.player = player;
    }

    @SubscribeEvent
    public void onBreakEvent(BlockEvent.BreakEvent event) {
        World w = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState s = w.getBlockState(pos);
        if (s.getBlock() instanceof BlockJukebox) {
            player.cancel(pos);
        }
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void onClick(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (world.getBlockState(pos).getBlock() instanceof BlockJukebox) {
            player.cancel(pos);
        }
        event.setResult(Event.Result.ALLOW);
    }
}
