package com.github.postyizhan.questpointer.network.packet;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;

import com.github.postyizhan.questpointer.QuestPointerPermissions;
import com.github.postyizhan.questpointer.network.NetworkHandler;
import com.github.postyizhan.questpointer.quest.QuestPointerController;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/** Client -> Server: admin GUI asks for the current point list of a quest. */
public class RecorderRequestPointsPacket implements IMessage {

    public int questId;

    public RecorderRequestPointsPacket() {}

    public RecorderRequestPointsPacket(int questId) {
        this.questId = questId;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(questId);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        questId = buf.readInt();
    }

    public static class Handler implements IMessageHandler<RecorderRequestPointsPacket, IMessage> {

        @Override
        public IMessage onMessage(RecorderRequestPointsPacket message, MessageContext ctx) {
            EntityPlayer player = ctx.getServerHandler().playerEntity;
            if (!QuestPointerPermissions.canUseRecorder(player)) return null;

            NetworkHandler.CHANNEL.sendTo(
                new SyncPointerPointsPacket(
                    message.questId,
                    QuestPointerController.INSTANCE.getPoints(message.questId)),
                (EntityPlayerMP) player);
            return null;
        }
    }
}
