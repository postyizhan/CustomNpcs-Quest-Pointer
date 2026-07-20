package com.github.postyizhan.questpointer.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.postyizhan.questpointer.quest.PointerEntry;

/**
 * Client-side cache of the local player's currently tracked quest and its
 * pointer entries, used by the overlay renderer. Updated from network packets.
 */
public class QuestPointerClientData {

    private static volatile int trackedQuestId = -1;
    private static volatile List<PointerEntry> trackedPoints = Collections.emptyList();

    private QuestPointerClientData() {}

    public static void setTracked(int questId, List<PointerEntry> points) {
        trackedQuestId = questId;
        trackedPoints = points == null ? Collections.<PointerEntry>emptyList() : new ArrayList<PointerEntry>(points);
    }

    public static void clearTracked() {
        trackedQuestId = -1;
        trackedPoints = Collections.emptyList();
    }

    /** Called when a quest's points were edited server-side; only applies if it's the tracked quest. */
    public static void setPoints(int questId, List<PointerEntry> points) {
        if (questId == trackedQuestId) {
            trackedPoints = points == null ? Collections.<PointerEntry>emptyList()
                : new ArrayList<PointerEntry>(points);
        }
    }

    public static boolean isTracking() {
        return trackedQuestId >= 0;
    }

    public static int getTrackedQuestId() {
        return trackedQuestId;
    }

    public static List<PointerEntry> getTrackedPoints() {
        return trackedPoints;
    }
}
