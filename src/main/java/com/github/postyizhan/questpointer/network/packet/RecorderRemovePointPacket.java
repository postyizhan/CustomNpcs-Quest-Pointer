package com.github.postyizhan.questpointer.network.packet;

import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.github.postyizhan.questpointer.QuestPointerPermissions;
import com.github.postyizhan.questpointer.network.NetworkHandler;
import com.github.postyizhan.questpointer.quest.PointerEntry;
import com.github.postyizhan.questpointer.quest.QuestPointerController;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Client -> Server: remove the point at the given index of a quest's point list. */
public class RecorderRemovePointPacket implements IMessage {

    public int questId;
    public int index;

    public RecorderRemovePointPacket() {}

    public RecorderRemovePointPacket(int questId, int index) {
        this.questId = questId;
        this.index = index;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(questId);
        buf.writeInt(index);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        questId = buf.readInt();
        index = buf.readInt();
    }

    public static class Handler implements IMessageHandler<RecorderRemovePointPacket, IMessage> {

        @Override
        public IMessage onMessage(RecorderRemovePointPacket message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;
            if (!QuestPointerPermissions.canUseRecorder(player)) return null;

            List<PointerEntry> points = QuestPointerController.INSTANCE.getPoints(message.questId);
            if (message.index < 0 || message.index >= points.size()) return null;
            points.remove(message.index);
            QuestPointerController.INSTANCE.setPoints(message.questId, points);

            NetworkHandler.CHANNEL
                .sendTo(new SyncPointerPointsPacket(message.questId, points), (EntityPlayerMP) player);
            com.github.postyizhan.questpointer.quest.QuestTrackingTicker.notifyPointsChanged(message.questId);
            return null;
        }
    }
}
