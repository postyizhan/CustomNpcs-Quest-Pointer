package com.github.postyizhan.questpointer.quest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.postyizhan.questpointer.network.NetworkHandler;
import com.github.postyizhan.questpointer.network.packet.SyncTrackedQuestPacket;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.controllers.data.PlayerData;

/**
 * CustomNPC+ has no event for "player changed tracked quest", so we poll each
 * online player's {@code PlayerData.questData.getTrackedQuest()} periodically
 * and push an update only when it actually changes.
 *
 * Trigger design (this is intentional, not a bug): the overlay shows only
 * while a quest is tracked via CustomNPC+'s own quest log "track" button, and
 * disappears as soon as the player untracks it OR the quest is completed.
 * Quest completion already clears CNPC+'s trackedQuest for us (CNPC+'s own
 * per-tick handler untracks a quest once it's no longer in activeQuests), so
 * we don't need extra completion-handling logic here - just react to
 * getTrackedQuest() going null.
 */
public class QuestTrackingTicker {

    // A quarter second feels responsive without meaningfully adding load: this
    // only does a getter call per online player between polls.
    private static final int POLL_INTERVAL_TICKS = 5;

    /** Last tracked quest id seen per player, -1 for "not tracking". */
    private final Map<UUID, Integer> lastTrackedQuestId = new HashMap<UUID, Integer>();

    private int ticks = 0;

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        if (ticks < POLL_INTERVAL_TICKS) return;
        ticks = 0;

        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;

        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            checkPlayer(player);
        }
    }

    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        lastTrackedQuestId.remove(event.player.getUniqueID());
    }

    private void checkPlayer(EntityPlayerMP player) {
        PlayerData data = PlayerData.get(player);
        if (data == null) return;

        IQuest tracked = data.questData.getTrackedQuest();
        int trackedId = tracked == null ? -1 : tracked.getId();

        UUID uuid = player.getUniqueID();
        Integer previous = lastTrackedQuestId.get(uuid);
        if (previous != null && previous == trackedId) return;

        lastTrackedQuestId.put(uuid, trackedId);
        sendTrackedState(player, trackedId);
    }

    private void sendTrackedState(EntityPlayerMP player, int trackedId) {
        if (trackedId < 0) {
            NetworkHandler.CHANNEL.sendTo(new SyncTrackedQuestPacket(-1, null), player);
        } else {
            List<PointerEntry> points = QuestPointerController.INSTANCE.getPoints(trackedId);
            NetworkHandler.CHANNEL.sendTo(new SyncTrackedQuestPacket(trackedId, points), player);
            // The overlay's trigger (tracking a quest via CNPC+'s own quest log) isn't
            // obvious from this mod alone, and an untracked quest with no recorded
            // points would otherwise show no overlay with no explanation. Give a
            // one-line hint the first time tracking starts for this session.
            if (points.isEmpty()) {
                player
                    .addChatMessage(new ChatComponentText(EnumChatFormatting.GRAY + "[任务指引] 当前追踪的任务没有记录坐标点，不会显示方向指示。"));
            }
        }
    }

    /**
     * Called by the recorder packets when a quest's point list is edited, so that
     * any online players currently tracking that quest get their overlay refreshed
     * immediately instead of waiting for the next poll (which wouldn't even fire,
     * since the tracked quest id itself hasn't changed).
     */
    public static void notifyPointsChanged(int questId) {
        MinecraftServer server = MinecraftServer.getServer();
        if (server == null) return;

        List<EntityPlayerMP> players = server.getConfigurationManager().playerEntityList;
        for (EntityPlayerMP player : players) {
            PlayerData data = PlayerData.get(player);
            if (data == null) continue;
            IQuest tracked = data.questData.getTrackedQuest();
            if (tracked != null && tracked.getId() == questId) {
                NetworkHandler.CHANNEL.sendTo(
                    new SyncTrackedQuestPacket(questId, QuestPointerController.INSTANCE.getPoints(questId)),
                    player);
            }
        }
    }
}
