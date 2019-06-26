package com.tajmoti.tubediscs.net;

import com.tajmoti.tubediscs.item.TubeDisc;
import com.tajmoti.tubediscs.util.Util;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;

import java.net.MalformedURLException;
import java.net.URL;

public class TubeSaveMessage implements IMessage {
    private URL url;


    /**
     * Required empty constructor!
     */
    @SuppressWarnings("unused")
    public TubeSaveMessage() {
    }

    /**
     * Actual initializing constructor.
     */
    public TubeSaveMessage(URL url) {
        this.url = url;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // URL
        byte[] urlStrBytes = new byte[buf.readableBytes()];
        buf.readBytes(urlStrBytes);
        try {
            url = new URL(new String(urlStrBytes));
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        String url = this.url.toString();
        buf.writeBytes(url.getBytes());
    }

    public static class Handler implements IMessageHandler<TubeSaveMessage, IMessage> {
        @Override
        public IMessage onMessage(TubeSaveMessage message, MessageContext ctx) {
            if (ctx.side == Side.SERVER) {
                ItemStack tubeDisc = Util.findItemInHands(ctx.getServerHandler().player, TubeDisc.class);
                if (tubeDisc != null)
                    TubeDisc.setUrl(tubeDisc, message.url);
            }
            return null;
        }
    }
}
