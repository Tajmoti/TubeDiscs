package com.tajmoti.tubediscs.net;

import com.tajmoti.tubediscs.TubeDiscs;
import com.tajmoti.tubediscs.audio.server.TimedAudioRequest;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TubePlayMessage implements IMessage {
    private TimedAudioRequest request;
    /**
     * We also need to send the current server time in order
     * to calculate the time offset in the track.
     * Could be done on the server and then send just the difference,
     * but that would require me to rewrite some of the mechanics.
     */
    private long serverTime;


    /**
     * Required empty constructor!
     */
    @SuppressWarnings("unused")
    public TubePlayMessage() {
    }

    /**
     * Actual initializing constructor.
     */
    public TubePlayMessage(TimedAudioRequest request, long serverTime) {
        this.request = request;
        this.serverTime = serverTime;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int dimen = buf.readInt();
        double x, y, z;
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        BlockPos pos = new BlockPos(x, y, z);
        long startTime = buf.readLong();
        this.serverTime = buf.readLong();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        String url = new String(bytes);
        this.request = new TimedAudioRequest(dimen, pos, url, startTime);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(request.dimen);
        buf.writeDouble(request.pos.getX());
        buf.writeDouble(request.pos.getY());
        buf.writeDouble(request.pos.getZ());
        buf.writeLong(request.ticksStarted);
        buf.writeLong(serverTime);
        buf.writeBytes(request.url.getBytes());
    }


    public static class Handler implements IMessageHandler<TubePlayMessage, IMessage> {
        @Override
        public IMessage onMessage(TubePlayMessage message, MessageContext ctx) {
            TubeDiscs mod = TubeDiscs.getInstance();
            mod.getAudio().playAudioAtPos(message.request, message.serverTime);

            String displayMsg = "Now playing " + message.request.url + " with offset " + (message.serverTime - message.request.ticksStarted) + " ticks";
            Minecraft.getMinecraft().ingameGUI.setOverlayMessage(displayMsg, true);
            return null;
        }
    }
}
