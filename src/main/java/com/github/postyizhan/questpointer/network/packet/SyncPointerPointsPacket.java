package com.github.postyizhan.questpointer.network.packet;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;

import com.github.postyizhan.questpointer.client.gui.GuiCoordinateRecorder;
import com.github.postyizhan.questpointer.network.PointerNBTUtil;
import com.github.postyizhan.questpointer.quest.PointerEntry;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client: full point list for a quest, sent either in response to
 * {@link RecorderRequestPointsPacket} (to refresh the admin GUI) or to update a
 * tracking player's overlay data when their tracked quest's points change.
 */
public class SyncPointerPointsPacket implements IMessage {

    public int questId;
    public List<PointerEntry> points;

    public SyncPointerPointsPacket() {}

    public SyncPointerPointsPacket(int questId, List<PointerEntry> points) {
        this.questId = questId;
        this.points = points;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(questId);
        PointerNBTUtil.writePoints(buf, points);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        questId = buf.readInt();
        points = PointerNBTUtil.readPoints(buf);
    }

    public static class Handler implements IMessageHandler<SyncPointerPointsPacket, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SyncPointerPointsPacket message, MessageContext ctx) {
            // Feed the admin GUI, if it's the one open and editing this quest.
            GuiScreen current = Minecraft.getMinecraft().currentScreen;
            if (current instanceof GuiCoordinateRecorder) {
                ((GuiCoordinateRecorder) current).onPointsReceived(message.questId, message.points);
            }
            // Feed the overlay's tracked-quest point cache regardless of which GUI is open.
            com.github.postyizhan.questpointer.client.QuestPointerClientData.setPoints(message.questId, message.points);
            return null;
        }
    }
}
