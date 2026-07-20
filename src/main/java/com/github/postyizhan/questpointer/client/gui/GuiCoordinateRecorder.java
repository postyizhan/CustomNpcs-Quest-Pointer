package com.github.postyizhan.questpointer.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;

import com.github.postyizhan.questpointer.client.DimensionNameUtil;
import com.github.postyizhan.questpointer.network.NetworkHandler;
import com.github.postyizhan.questpointer.network.packet.RecorderAddPointPacket;
import com.github.postyizhan.questpointer.network.packet.RecorderRemovePointPacket;
import com.github.postyizhan.questpointer.network.packet.RecorderRenamePointPacket;
import com.github.postyizhan.questpointer.network.packet.RecorderRequestPointsPacket;
import com.github.postyizhan.questpointer.quest.PointerEntry;

import noppes.npcs.controllers.data.Quest;

/**
 * Point manager for a quest: shows the recorded position (from the block the
 * recorder was used on), lets the admin pick a quest, and lists/add/remove/rename
 * that quest's pointer entries. Reads the quest list straight from CustomNPC+'s
 * client-side {@code QuestController}, which is already kept in sync by CNPC+'s
 * own login/edit sync packets.
 */
public class GuiCoordinateRecorder extends GuiScreen {

    private static final int ROWS_VISIBLE = 6;
    private static final int ROW_HEIGHT = 20;

    // Vertical layout constants. Each band leaves a visible gap from the one
    // before it so nothing overlaps (this previously overlapped badly: the
    // status line, the note label, the note field and the add-point button
    // were all crammed into the same y=44-64 band).
    private static final int TITLE_Y = 12;
    private static final int QUEST_BUTTON_Y = 28;
    private static final int STATUS_Y = 54;
    private static final int NOTE_LABEL_Y = 72;
    private static final int NOTE_ROW_Y = 84;
    private static final int LIST_TOP = 112;

    private final int dimension;
    private final int blockX;
    private final int blockY;
    private final int blockZ;

    private Quest selectedQuest;
    private List<PointerEntry> points = new ArrayList<PointerEntry>();
    private int scrollOffset = 0;

    private GuiTextField labelField;
    private int renamingIndex = -1;
    private GuiTextField renameField;

    public GuiCoordinateRecorder(int dimension, int x, int y, int z) {
        this.dimension = dimension;
        this.blockX = x;
        this.blockY = y;
        this.blockZ = z;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int centerX = width / 2;

        buttonList.add(
            new GuiButton(
                0,
                centerX - 100,
                QUEST_BUTTON_Y,
                200,
                20,
                selectedQuest == null ? "选择任务..." : ("任务: " + selectedQuest.title)));

        labelField = new GuiTextField(fontRendererObj, centerX - 100, NOTE_ROW_Y, 140, 16);
        labelField.setMaxStringLength(64);

        buttonList.add(new GuiButton(1, centerX + 44, NOTE_ROW_Y - 2, 56, 20, "添加当前点"));

        for (int i = 0; i < ROWS_VISIBLE; i++) {
            int rowY = LIST_TOP + i * ROW_HEIGHT;
            // id scheme: 100+i remove, 200+i rename
            buttonList.add(new GuiButton(200 + i, centerX - 100, rowY, 140, ROW_HEIGHT - 2, ""));
            buttonList.add(new GuiButton(100 + i, centerX + 44, rowY, 56, ROW_HEIGHT - 2, "删除"));
        }

        buttonList.add(new GuiButton(10, centerX - 60, LIST_TOP + ROWS_VISIBLE * ROW_HEIGHT + 6, 20, 20, "<"));
        buttonList.add(new GuiButton(11, centerX + 40, LIST_TOP + ROWS_VISIBLE * ROW_HEIGHT + 6, 20, 20, ">"));

        buttonList.add(new GuiButton(99, centerX - 40, height - 30, 80, 20, "完成"));

        updateRowVisibility();
    }

    private void updateRowVisibility() {
        for (Object obj : buttonList) {
            GuiButton button = (GuiButton) obj;
            if (button.id >= 100 && button.id < 300) {
                int rowIndex = scrollOffset + (button.id % 100);
                button.visible = rowIndex < points.size();
            }
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        int centerX = width / 2;

        if (button.id == 0) {
            commitRename();
            mc.displayGuiScreen(new GuiQuestPicker(this));
            return;
        }
        if (button.id == 1) {
            commitRename();
            addCurrentPoint();
            return;
        }
        if (button.id == 10) {
            if (scrollOffset > 0) {
                commitRename();
                scrollOffset--;
                updateRowVisibility();
            }
            return;
        }
        if (button.id == 11) {
            if (scrollOffset + ROWS_VISIBLE < points.size()) {
                commitRename();
                scrollOffset++;
                updateRowVisibility();
            }
            return;
        }
        if (button.id == 99) {
            commitRename();
            mc.displayGuiScreen(null);
            return;
        }
        if (button.id >= 100 && button.id < 200) {
            int rowIndex = scrollOffset + (button.id - 100);
            if (rowIndex < points.size() && selectedQuest != null) {
                commitRename();
                NetworkHandler.CHANNEL.sendToServer(new RecorderRemovePointPacket(selectedQuest.id, rowIndex));
            }
            return;
        }
        if (button.id >= 200 && button.id < 300) {
            int rowIndex = scrollOffset + (button.id - 200);
            if (rowIndex < points.size()) {
                startRename(rowIndex);
            }
        }
    }

    private void startRename(int rowIndex) {
        if (renamingIndex >= 0) {
            commitRename();
        }
        renamingIndex = rowIndex;
        PointerEntry entry = points.get(rowIndex);
        int centerX = width / 2;
        int rowSlot = rowIndex - scrollOffset;
        int rowY = LIST_TOP + rowSlot * ROW_HEIGHT;
        renameField = new GuiTextField(fontRendererObj, centerX - 100, rowY, 140, ROW_HEIGHT - 2);
        renameField.setMaxStringLength(64);
        renameField.setText(entry.label);
        renameField.setFocused(true);
    }

    private void commitRename() {
        if (renamingIndex >= 0 && renameField != null && selectedQuest != null) {
            NetworkHandler.CHANNEL
                .sendToServer(new RecorderRenamePointPacket(selectedQuest.id, renamingIndex, renameField.getText()));
        }
        renamingIndex = -1;
        renameField = null;
    }

    private void addCurrentPoint() {
        if (selectedQuest == null) return;
        String label = labelField.getText();
        NetworkHandler.CHANNEL.sendToServer(
            new RecorderAddPointPacket(selectedQuest.id, dimension, blockX + 0.5, blockY, blockZ + 0.5, label));
        labelField.setText("");
    }

    /** Called by {@link GuiQuestPicker} when the admin selects a quest. */
    public void onQuestSelected(Quest quest) {
        renamingIndex = -1;
        renameField = null;
        this.selectedQuest = quest;
        this.points = new ArrayList<PointerEntry>();
        this.scrollOffset = 0;
        NetworkHandler.CHANNEL.sendToServer(new RecorderRequestPointsPacket(quest.id));
        initGui();
    }

    /** Called from {@link com.github.postyizhan.questpointer.network.packet.SyncPointerPointsPacket} handling. */
    public void onPointsReceived(int questId, List<PointerEntry> newPoints) {
        if (selectedQuest != null && selectedQuest.id == questId) {
            this.points = newPoints;
            if (renamingIndex >= points.size()) {
                renamingIndex = -1;
                renameField = null;
            }
            if (scrollOffset > 0 && scrollOffset >= points.size()) {
                scrollOffset = Math.max(0, points.size() - ROWS_VISIBLE);
            }
            updateRowVisibility();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = width / 2;

        drawCenteredString(fontRendererObj, "坐标记录仪", centerX, TITLE_Y, 0xFFFFFF);
        drawString(
            fontRendererObj,
            "位置: " + DimensionNameUtil.getName(dimension) + "  X:" + blockX + " Y:" + blockY + " Z:" + blockZ,
            centerX - 100,
            STATUS_Y,
            0xAAAAAA);

        if (selectedQuest == null) {
            drawCenteredString(fontRendererObj, "请先选择一个任务", centerX, LIST_TOP + 60, 0xFF5555);
        }

        drawString(fontRendererObj, "备注(可选):", centerX - 100, NOTE_LABEL_Y, 0xAAAAAA);

        if (points.isEmpty() && selectedQuest != null) {
            drawCenteredString(fontRendererObj, "该任务暂无坐标点", centerX, LIST_TOP + 30, 0x888888);
        }

        drawCenteredString(
            fontRendererObj,
            (scrollOffset + 1) + "-" + Math.min(scrollOffset + ROWS_VISIBLE, points.size()) + " / " + points.size(),
            centerX,
            LIST_TOP + ROWS_VISIBLE * ROW_HEIGHT + 12,
            0xAAAAAA);

        // super.drawScreen draws every GuiButton's own (opaque) background texture,
        // including the invisible-label row buttons (id 200+i) that we manually
        // annotate below. It must run BEFORE that manual text/text-field drawing,
        // otherwise the button backgrounds paint over and hide our labels.
        super.drawScreen(mouseX, mouseY, partialTicks);

        labelField.drawTextBox();

        for (int i = 0; i < ROWS_VISIBLE; i++) {
            int rowIndex = scrollOffset + i;
            if (rowIndex >= points.size()) continue;
            int rowY = LIST_TOP + i * ROW_HEIGHT;
            PointerEntry entry = points.get(rowIndex);

            if (renamingIndex == rowIndex && renameField != null) {
                renameField.drawTextBox();
            } else {
                String text = String.format(
                    "%s (%s %.0f,%.0f,%.0f)",
                    entry.label.isEmpty() ? ("坐标点 " + (rowIndex + 1)) : entry.label,
                    DimensionNameUtil.getName(entry.dimension),
                    entry.x,
                    entry.y,
                    entry.z);
                drawString(fontRendererObj, trimToWidth(text, 138), centerX - 98, rowY + 5, 0xFFFFFF);
            }
        }
    }

    private String trimToWidth(String text, int maxWidth) {
        while (fontRendererObj.getStringWidth(text) > maxWidth && text.length() > 1) {
            text = text.substring(0, text.length() - 2) + "…";
        }
        return text;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        labelField.mouseClicked(mouseX, mouseY, button);
        if (renameField != null) {
            boolean wasFocused = renameField.isFocused();
            renameField.mouseClicked(mouseX, mouseY, button);
            if (wasFocused && !renameField.isFocused()) {
                commitRename();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (renameField != null) {
            if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_ESCAPE) {
                commitRename();
                return;
            }
            renameField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        if (labelField.isFocused()) {
            if (keyCode == Keyboard.KEY_RETURN) {
                addCurrentPoint();
                return;
            }
            labelField.textboxKeyTyped(typedChar, keyCode);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        labelField.updateCursorCounter();
        if (renameField != null) renameField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (renamingIndex >= 0) {
            commitRename();
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
