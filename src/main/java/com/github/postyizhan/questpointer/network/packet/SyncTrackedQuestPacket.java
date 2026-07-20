package com.github.postyizhan.questpointer.network.packet;

import java.util.List;

import com.github.postyizhan.questpointer.client.QuestPointerClientData;
import com.github.postyizhan.questpointer.network.PointerNBTUtil;
import com.github.postyizhan.questpointer.quest.PointerEntry;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client: tells the client which quest (if any) is currently tracked
 * and the pointer entries attached to it. Sent whenever the player's tracked
 * quest changes, and whenever the points of the currently tracked quest are edited.
 * An empty/absent quest (questId == -1) means "stop showing the overlay".
 */
public class SyncTrackedQuestPacket implements IMessage {

    public int questId = -1;
    public List<PointerEntry> points;

    public SyncTrackedQuestPacket() {}

    public SyncTrackedQuestPacket(int questId, List<PointerEntry> points) {
        this.questId = questId;
        this.points = points;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(questId);
        PointerNBTUtil.writePoints(buf, points == null ? java.util.Collections.<PointerEntry>emptyList() : points);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        questId = buf.readInt();
        points = PointerNBTUtil.readPoints(buf);
    }

    public static class Handler implements IMessageHandler<SyncTrackedQuestPacket, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(SyncTrackedQuestPacket message, MessageContext ctx) {
            if (message.questId < 0) {
                QuestPointerClientData.clearTracked();
            } else {
                QuestPointerClientData.setTracked(message.questId, message.points);
            }
            return null;
        }
    }
}
