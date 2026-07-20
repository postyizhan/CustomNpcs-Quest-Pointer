package com.github.postyizhan.questpointer.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.github.postyizhan.questpointer.QuestPointerPermissions;
import com.github.postyizhan.questpointer.network.NetworkHandler;
import com.github.postyizhan.questpointer.quest.PointerEntry;
import com.github.postyizhan.questpointer.quest.QuestPointerController;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;
import noppes.npcs.controllers.QuestController;

/** Client -> Server: add a point (the block position the recorder was used on) to a quest. */
public class RecorderAddPointPacket implements IMessage {

    public int questId;
    public int dimension;
    public double x, y, z;
    public String label;

    public RecorderAddPointPacket() {}

    public RecorderAddPointPacket(int questId, int dimension, double x, double y, double z, String label) {
        this.questId = questId;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.label = label == null ? "" : label;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(questId);
        buf.writeInt(dimension);
        buf.writeDouble(x);
        buf.writeDouble(y);
        buf.writeDouble(z);
        ByteBufUtils.writeUTF8String(buf, label);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        questId = buf.readInt();
        dimension = buf.readInt();
        x = buf.readDouble();
        y = buf.readDouble();
        z = buf.readDouble();
        label = ByteBufUtils.readUTF8String(buf);
    }

    public static class Handler implements IMessageHandler<RecorderAddPointPacket, IMessage> {

        @Override
        public IMessage onMessage(RecorderAddPointPacket message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;
            if (!QuestPointerPermissions.canUseRecorder(player)) return null;
            if (!QuestController.Instance.quests.containsKey(message.questId)) return null;

            QuestPointerController.INSTANCE.addPoint(
                message.questId,
                new PointerEntry(message.dimension, message.x, message.y, message.z, message.label));

            NetworkHandler.CHANNEL.sendTo(
                new SyncPointerPointsPacket(
                    message.questId,
                    QuestPointerController.INSTANCE.getPoints(message.questId)),
                (EntityPlayerMP) player);
            com.github.postyizhan.questpointer.quest.QuestTrackingTicker.notifyPointsChanged(message.questId);
            return null;
        }
    }
}
