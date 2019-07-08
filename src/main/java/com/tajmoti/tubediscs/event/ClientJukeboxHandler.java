package com.tajmoti.tubediscs.event;

import com.tajmoti.tubediscs.audio.client.PositionedAudioPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@SuppressWarnings("unused")
public class ClientJukeboxHandler {
    private final PositionedAudioPlayer player;


    public ClientJukeboxHandler(PositionedAudioPlayer player) {
        this.player = player;
    }

    @SubscribeEvent
    public void onPlayerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        player.stopAllAudio();
        event.setResult(Event.Result.ALLOW);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END)
            player.update();
    }
}
