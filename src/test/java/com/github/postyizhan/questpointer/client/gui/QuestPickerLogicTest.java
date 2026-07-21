package com.github.postyizhan.questpointer.client.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class QuestPickerLogicTest {

    @Test
    public void finishedViewExcludesCurrentlyActiveQuest() {
        Set<Integer> activeIds = new HashSet<Integer>(Arrays.asList(7));
        Set<Integer> finishedIds = new HashSet<Integer>(Arrays.asList(7, 8));

        assertFalse(QuestPickerLogic.isFinishedOnly(7, activeIds, finishedIds));
        assertTrue(QuestPickerLogic.isFinishedOnly(8, activeIds, finishedIds));
    }

    @Test
    public void categorySelectionFindsReplacementPositionByStableId() {
        assertEquals(1, QuestPickerLogic.findIdIndex(Arrays.asList(4, 12, 20), 12));
        assertEquals(-1, QuestPickerLogic.findIdIndex(Arrays.asList(4, 12, 20), 99));
    }

    @Test
    public void offsetIsClampedWhenListShrinksOrViewportChanges() {
        assertEquals(0, QuestPickerLogic.clampOffset(10, 5, 8));
        assertEquals(2, QuestPickerLogic.clampOffset(10, 5, 3));
        assertEquals(0, QuestPickerLogic.clampOffset(-4, 20, 8));
        assertEquals(5, QuestPickerLogic.clampOffset(5, 20, 8));
    }
}
