package com.tajmoti.tubediscs.net;

import com.tajmoti.tubediscs.TubeDiscs;
import com.tajmoti.tubediscs.client.sound.PositionedAudioPlayer;
import io.netty.buffer.ByteBuf;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class TubePlayMessage implements IMessage {
    private int dimen;
    private BlockPos pos;
    private int offset;
    private String url;


    /**
     * Required empty constructor!
     */
    @SuppressWarnings("unused")
    public TubePlayMessage() {
    }

    /**
     * Actual initializing constructor.
     */
    public TubePlayMessage(int dimen, BlockPos pos, String url, int offset) {
        this.dimen = dimen;
        this.pos = pos;
        this.offset = offset;
        this.url = url;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimen = buf.readInt();
        double x, y, z;
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        pos = new BlockPos(x, y, z);
        offset = buf.readInt();
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        url = new String(bytes);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimen);
        buf.writeDouble(pos.getX());
        buf.writeDouble(pos.getY());
        buf.writeDouble(pos.getZ());
        buf.writeInt(offset);
        buf.writeBytes(url.getBytes());
    }


    public static class Handler implements IMessageHandler<TubePlayMessage, IMessage> {
        @Override
        public IMessage onMessage(TubePlayMessage message, MessageContext ctx) {
            TubeDiscs mod = TubeDiscs.getInstance();

            PositionedAudioPlayer.Request request = new PositionedAudioPlayer.Request(message.dimen, message.pos, message.url);
            int offset = message.offset;

            mod.getAudio().playAudioAtPos(request, offset);
            return null;
        }
    }
}
