package com.github.postyizhan.questpointer.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;

import org.lwjgl.input.Keyboard;

import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;

/**
 * Simple category -> quest browser for the recorder GUI. Reads directly from
 * CustomNPC+'s client-side {@code QuestController}, which is already populated
 * by CNPC+'s own login sync (and kept current by its own edit-sync packets).
 */
public class GuiQuestPicker extends GuiScreen {

    private static final int ROWS_VISIBLE = 8;
    private static final int ROW_HEIGHT = 18;

    // Vertical layout constants, with enough gap between bands that the search
    // field (which has its own opaque background) never overlaps the heading
    // text drawn above it.
    private static final int TITLE_Y = 16;
    private static final int HEADING_Y = 32;
    private static final int SEARCH_Y = 46;
    private static final int LIST_TOP = 68;

    private final GuiCoordinateRecorder parent;

    private QuestCategory selectedCategory;
    private List<QuestCategory> categories = new ArrayList<QuestCategory>();
    private List<Quest> quests = new ArrayList<Quest>();

    private int catScroll = 0;
    private int questScroll = 0;

    private GuiTextField searchField;
    private String search = "";

    public GuiQuestPicker(GuiCoordinateRecorder parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        categories = new ArrayList<QuestCategory>(QuestController.Instance.categories.values());
        Collections.sort(categories, new Comparator<QuestCategory>() {

            @Override
            public int compare(QuestCategory a, QuestCategory b) {
                return a.title.compareToIgnoreCase(b.title);
            }
        });

        int leftX = width / 2 - 180;
        int rightX = width / 2 + 4;

        searchField = new GuiTextField(fontRendererObj, rightX, SEARCH_Y, 176, 16);
        searchField.setText(search);

        for (int i = 0; i < ROWS_VISIBLE; i++) {
            buttonList.add(new GuiButton(10 + i, leftX, LIST_TOP + i * ROW_HEIGHT, 176, ROW_HEIGHT - 2, ""));
        }
        buttonList.add(new GuiButton(1, leftX, LIST_TOP + ROWS_VISIBLE * ROW_HEIGHT + 4, 20, 20, "<"));
        buttonList.add(new GuiButton(2, leftX + 156, LIST_TOP + ROWS_VISIBLE * ROW_HEIGHT + 4, 20, 20, ">"));

        for (int i = 0; i < ROWS_VISIBLE; i++) {
            buttonList.add(new GuiButton(50 + i, rightX, LIST_TOP + i * ROW_HEIGHT, 176, ROW_HEIGHT - 2, ""));
        }
        buttonList.add(new GuiButton(3, rightX, LIST_TOP + ROWS_VISIBLE * ROW_HEIGHT + 4, 20, 20, "<"));
        buttonList.add(new GuiButton(4, rightX + 156, LIST_TOP + ROWS_VISIBLE * ROW_HEIGHT + 4, 20, 20, ">"));

        buttonList.add(new GuiButton(99, width / 2 - 40, height - 30, 80, 20, "取消"));

        refreshQuestList();
        updateRowLabels();
    }

    private void refreshQuestList() {
        quests = new ArrayList<Quest>();
        if (selectedCategory != null) {
            quests.addAll(selectedCategory.quests.values());
        }
        if (!search.isEmpty()) {
            List<Quest> filtered = new ArrayList<Quest>();
            for (Quest quest : quests) {
                if (quest.title.toLowerCase()
                    .contains(search.toLowerCase())) filtered.add(quest);
            }
            quests = filtered;
        }
        Collections.sort(quests, new Comparator<Quest>() {

            @Override
            public int compare(Quest a, Quest b) {
                return a.title.compareToIgnoreCase(b.title);
            }
        });
        questScroll = 0;
    }

    private void updateRowLabels() {
        for (int i = 0; i < ROWS_VISIBLE; i++) {
            GuiButton catButton = getButton(10 + i);
            int catIndex = catScroll + i;
            if (catIndex < categories.size()) {
                catButton.visible = true;
                catButton.displayString = categories.get(catIndex).title;
            } else {
                catButton.visible = false;
            }

            GuiButton questButton = getButton(50 + i);
            int questIndex = questScroll + i;
            if (questIndex < quests.size()) {
                questButton.visible = true;
                questButton.displayString = quests.get(questIndex).title;
            } else {
                questButton.visible = false;
            }
        }
    }

    private GuiButton getButton(int id) {
        for (Object obj : buttonList) {
            GuiButton button = (GuiButton) obj;
            if (button.id == id) return button;
        }
        return null;
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.id == 99) {
            mc.displayGuiScreen(parent);
            return;
        }
        if (button.id == 1) {
            if (catScroll > 0) {
                catScroll--;
                updateRowLabels();
            }
            return;
        }
        if (button.id == 2) {
            if (catScroll + ROWS_VISIBLE < categories.size()) {
                catScroll++;
                updateRowLabels();
            }
            return;
        }
        if (button.id == 3) {
            if (questScroll > 0) {
                questScroll--;
                updateRowLabels();
            }
            return;
        }
        if (button.id == 4) {
            if (questScroll + ROWS_VISIBLE < quests.size()) {
                questScroll++;
                updateRowLabels();
            }
            return;
        }
        if (button.id >= 10 && button.id < 10 + ROWS_VISIBLE) {
            int catIndex = catScroll + (button.id - 10);
            if (catIndex < categories.size()) {
                selectedCategory = categories.get(catIndex);
                refreshQuestList();
                updateRowLabels();
            }
            return;
        }
        if (button.id >= 50 && button.id < 50 + ROWS_VISIBLE) {
            int questIndex = questScroll + (button.id - 50);
            if (questIndex < quests.size()) {
                Quest quest = quests.get(questIndex);
                parent.onQuestSelected(quest);
                mc.displayGuiScreen(parent);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int leftX = width / 2 - 180;
        int rightX = width / 2 + 4;

        drawCenteredString(fontRendererObj, "选择任务", width / 2, TITLE_Y, 0xFFFFFF);
        drawString(fontRendererObj, "分类", leftX, HEADING_Y, 0xAAAAAA);
        drawString(fontRendererObj, "任务 (可搜索)", rightX, HEADING_Y, 0xAAAAAA);
        searchField.drawTextBox();

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        super.mouseClicked(mouseX, mouseY, button);
        searchField.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) {
        if (searchField.isFocused()) {
            if (keyCode == Keyboard.KEY_ESCAPE) {
                mc.displayGuiScreen(parent);
                return;
            }
            searchField.textboxKeyTyped(typedChar, keyCode);
            if (!search.equals(searchField.getText())) {
                search = searchField.getText();
                refreshQuestList();
                updateRowLabels();
            }
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        searchField.updateCursorCounter();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
