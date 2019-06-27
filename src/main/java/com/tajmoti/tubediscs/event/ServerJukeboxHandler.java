package com.tajmoti.tubediscs.event;

import com.tajmoti.tubediscs.net.TubeStopMessage;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;

@SuppressWarnings("unused")
public class ServerJukeboxHandler {
    private final SimpleNetworkWrapper network;


    public ServerJukeboxHandler(SimpleNetworkWrapper network) {
        this.network = network;
    }

    @SubscribeEvent
    public void onBreakEvent(BlockEvent.BreakEvent event) {
        World w = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState s = w.getBlockState(pos);
        if (s.getBlock() instanceof BlockJukebox)
            network.sendToAll(new TubeStopMessage(pos));
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void onClick(PlayerInteractEvent.RightClickBlock event) {
        World world = event.getWorld();
        BlockPos pos = event.getPos();
        if (world.getBlockState(pos).getBlock() instanceof BlockJukebox)
            network.sendToAll(new TubeStopMessage(pos));
        event.setResult(Event.Result.ALLOW);
    }
}
