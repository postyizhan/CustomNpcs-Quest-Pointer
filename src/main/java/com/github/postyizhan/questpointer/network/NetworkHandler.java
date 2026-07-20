package com.github.postyizhan.questpointer.network;

import com.github.postyizhan.questpointer.QuestPointerMod;
import com.github.postyizhan.questpointer.network.packet.OpenRecorderGuiPacket;
import com.github.postyizhan.questpointer.network.packet.RecorderAddPointPacket;
import com.github.postyizhan.questpointer.network.packet.RecorderRemovePointPacket;
import com.github.postyizhan.questpointer.network.packet.RecorderRenamePointPacket;
import com.github.postyizhan.questpointer.network.packet.RecorderRequestPointsPacket;
import com.github.postyizhan.questpointer.network.packet.SyncPointerPointsPacket;
import com.github.postyizhan.questpointer.network.packet.SyncTrackedQuestPacket;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public class NetworkHandler {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(QuestPointerMod.MODID);

    public static void init() {
        int id = 0;
        CHANNEL.registerMessage(OpenRecorderGuiPacket.Handler.class, OpenRecorderGuiPacket.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(
            RecorderRequestPointsPacket.Handler.class,
            RecorderRequestPointsPacket.class,
            id++,
            Side.SERVER);
        CHANNEL.registerMessage(RecorderAddPointPacket.Handler.class, RecorderAddPointPacket.class, id++, Side.SERVER);
        CHANNEL.registerMessage(
            RecorderRemovePointPacket.Handler.class,
            RecorderRemovePointPacket.class,
            id++,
            Side.SERVER);
        CHANNEL.registerMessage(
            RecorderRenamePointPacket.Handler.class,
            RecorderRenamePointPacket.class,
            id++,
            Side.SERVER);
        CHANNEL
            .registerMessage(SyncPointerPointsPacket.Handler.class, SyncPointerPointsPacket.class, id++, Side.CLIENT);
        CHANNEL.registerMessage(SyncTrackedQuestPacket.Handler.class, SyncTrackedQuestPacket.class, id++, Side.CLIENT);
    }
}
