package com.tajmoti.tubediscs.event;

import com.tajmoti.tubediscs.client.sound.PositionedAudioPlayer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
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
}
