package com.github.postyizhan.questpointer.client.gui;

import java.util.List;
import java.util.Set;

/** Pure state helpers kept independent from Minecraft rendering classes. */
final class QuestPickerLogic {

    private QuestPickerLogic() {}

    static int clampOffset(int offset, int size, int visibleRows) {
        return Math.min(Math.max(0, offset), Math.max(0, size - visibleRows));
    }

    static boolean isFinishedOnly(int questId, Set<Integer> activeIds, Set<Integer> finishedIds) {
        return finishedIds.contains(questId) && !activeIds.contains(questId);
    }

    static int findIdIndex(List<Integer> ids, int selectedId) {
        if (selectedId < 0) return -1;
        for (int i = 0; i < ids.size(); i++) {
            if (ids.get(i)
                .intValue() == selectedId) return i;
        }
        return -1;
    }
}
