package com.tajmoti.tubediscs.net;

import com.tajmoti.tubediscs.TubeDiscs;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.net.MalformedURLException;
import java.net.URL;

public class TubePlayMessage implements IMessage {
    private BlockPos pos;
    private String url;


    /**
     * Required empty constructor!
     */
    public TubePlayMessage() {
    }

    /**
     * Actual initializing constructor.
     */
    public TubePlayMessage(BlockPos pos, String url) {
        this.pos = pos;
        this.url = url;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        double x, y, z;
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        pos = new BlockPos(x, y, z);
        byte[] bytes = new byte[buf.readableBytes()];
        buf.readBytes(bytes);
        url = new String(bytes);
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeDouble(pos.getX());
        buf.writeDouble(pos.getY());
        buf.writeDouble(pos.getZ());
        buf.writeBytes(url.getBytes());
    }


    public static class Handler implements IMessageHandler<TubePlayMessage, IMessage> {
        @Override
        public IMessage onMessage(TubePlayMessage message, MessageContext ctx) {
            if (ctx.side == Side.CLIENT) {
                Minecraft.getMinecraft().addScheduledTask(() -> playOnClient(message.url, message.pos));
            }
            return null;
        }

        @SideOnly(Side.CLIENT)
        private void playOnClient(String urls, BlockPos pos) {
            TubeDiscs mod = TubeDiscs.getInstance();
            try {
                URL url = new URL(urls);
                mod.getAudio().playVideoAtPos(url, pos);
            } catch (MalformedURLException e) {
                mod.getLogger().error(e);
            }
        }
    }
}
