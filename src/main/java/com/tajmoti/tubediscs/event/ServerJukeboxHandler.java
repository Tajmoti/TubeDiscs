package com.tajmoti.tubediscs.event;

import com.tajmoti.tubediscs.audio.AudioTracker;
import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;
import com.tajmoti.tubediscs.net.TubePlayMessage;
import com.tajmoti.tubediscs.net.TubeStopMessage;
import net.minecraft.block.BlockJukebox;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

@SuppressWarnings("unused")
public class ServerJukeboxHandler {
    private final AudioTracker<TimedAudioRequest> tracker;
    private final SimpleNetworkWrapper network;


    public ServerJukeboxHandler(AudioTracker<TimedAudioRequest> tracker, SimpleNetworkWrapper network) {
        this.tracker = tracker;
        this.network = network;
    }

    @SubscribeEvent
    public void onBreakEvent(BlockEvent.BreakEvent event) {
        World w = event.getWorld();
        BlockPos pos = event.getPos();
        IBlockState s = w.getBlockState(pos);
        if (s.getBlock() instanceof BlockJukebox) {
            int dimen = event.getPlayer().dimension;
            network.sendToAll(new TubeStopMessage(event.getPlayer().dimension, pos));
            tracker.removeSoundAtPos(dimen, pos);
        }
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void onClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() == Side.SERVER) {
            World world = event.getWorld();
            BlockPos pos = event.getPos();
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() instanceof BlockJukebox && state.getValue(BlockJukebox.HAS_RECORD)) {
                int dimen = event.getEntityPlayer().dimension;
                network.sendToAll(new TubeStopMessage(dimen, pos));
                tracker.removeSoundAtPos(dimen, pos);
                event.setResult(Event.Result.ALLOW);
            } else {
                event.setResult(Event.Result.DEFAULT);
            }
        } else {
            event.setResult(Event.Result.DEFAULT);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        for (TimedAudioRequest request : tracker.getSoundsByDim(event.player.dimension)) {
            network.sendTo(new TubePlayMessage(request, event.player.world.getTotalWorldTime()), (EntityPlayerMP) event.player);
        }
    }
}
