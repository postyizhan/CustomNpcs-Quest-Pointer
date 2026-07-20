package com.github.postyizhan.questpointer.network.packet;

import net.minecraft.client.Minecraft;

import com.github.postyizhan.questpointer.client.gui.GuiCoordinateRecorder;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client: tells the client to open the recorder GUI for the block the
 * player right-clicked with the coordinate recorder item.
 */
public class OpenRecorderGuiPacket implements IMessage {

    public int dimension;
    public int x;
    public int y;
    public int z;

    public OpenRecorderGuiPacket() {}

    public OpenRecorderGuiPacket(int dimension, int x, int y, int z) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(dimension);
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        dimension = buf.readInt();
        x = buf.readInt();
        y = buf.readInt();
        z = buf.readInt();
    }

    public static class Handler implements IMessageHandler<OpenRecorderGuiPacket, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(OpenRecorderGuiPacket message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .displayGuiScreen(new GuiCoordinateRecorder(message.dimension, message.x, message.y, message.z));
            return null;
        }
    }
}
