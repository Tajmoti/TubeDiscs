package com.tajmoti.tubediscs.net;

import com.tajmoti.tubediscs.TubeDiscs;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

public class TubeStopMessage implements IMessage {
    private int dimen;
    private BlockPos pos;


    /**
     * Required empty constructor!
     */
    @SuppressWarnings("unused")
    public TubeStopMessage() {
    }

    /**
     * Actual initializing constructor.
     */
    public TubeStopMessage(int dimen, BlockPos pos) {
        this.dimen = dimen;
        this.pos = pos;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimen = buf.readInt();
        double x, y, z;
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        pos = new BlockPos(x, y, z);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimen);
        buf.writeDouble(pos.getX());
        buf.writeDouble(pos.getY());
        buf.writeDouble(pos.getZ());
    }


    public static class Handler implements IMessageHandler<TubeStopMessage, IMessage> {
        @Override
        public IMessage onMessage(TubeStopMessage message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> {
                    TubeDiscs.getInstance().getAudio().stopAudioAtPos(message.dimen, message.pos);
                });
            }
            return null;
        }
    }
}
