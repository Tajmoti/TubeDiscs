package com.tajmoti.tubediscs.event;

import com.tajmoti.tubediscs.client.converter.OnlineAudioPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@SuppressWarnings("unused")
public class ClientJukeboxHandler {
    private final OnlineAudioPlayer player;


    public ClientJukeboxHandler(OnlineAudioPlayer player) {
        this.player = player;
    }

    @SubscribeEvent
    public void onPlayerDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        player.cancelAll();
        event.setResult(Event.Result.ALLOW);
    }
}
