package com.github.postyizhan.questpointer.client.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.StatCollector;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import noppes.npcs.api.handler.data.IQuest;
import noppes.npcs.controllers.QuestController;
import noppes.npcs.controllers.data.PlayerData;
import noppes.npcs.controllers.data.Quest;
import noppes.npcs.controllers.data.QuestCategory;

/**
 * Category and quest browser for the recorder GUI. Quest status is read from
 * CustomNPC+'s existing client-side player data cache and is only used to
 * filter this display.
 */
@SideOnly(Side.CLIENT)
public class GuiQuestPicker extends GuiScreen {

    private static final int MAX_ROWS_VISIBLE = 8;
    private static final int ROW_HEIGHT = 18;

    private static final int TITLE_Y = 6;
    private static final int TAB_Y = 18;
    private static final int TAB_HEIGHT = 20;
    private static final int HEADING_Y = 42;
    private static final int SEARCH_Y = 54;
    private static final int LIST_TOP = 76;

    private static final int TAB_ALL = 1000;
    private static final int TAB_ACTIVE = 1001;
    private static final int TAB_FINISHED = 1002;

    private final GuiCoordinateRecorder parent;

    private QuestView selectedView = QuestView.ALL;
    private QuestCategory selectedCategory;
    private List<QuestCategory> categories = new ArrayList<QuestCategory>();
    private List<Quest> quests = new ArrayList<Quest>();
    private final Set<Integer> activeQuestIds = new HashSet<Integer>();
    private final Set<Integer> finishedQuestIds = new HashSet<Integer>();

    private int catScroll;
    private int questScroll;
    private int rowsVisible = MAX_ROWS_VISIBLE;

    private GuiTextField searchField;
    private String search = "";

    private enum QuestView {
        ALL,
        ACTIVE,
        FINISHED
    }

    public GuiQuestPicker(GuiCoordinateRecorder parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int selectedCategoryId = getSelectedCategoryId();
        int leftX = width / 2 - 180;
        int rightX = width / 2 + 4;
        int pagerY = height - 54;
        int availableListHeight = pagerY - LIST_TOP - 4;
        rowsVisible = Math.max(1, Math.min(MAX_ROWS_VISIBLE, availableListHeight / ROW_HEIGHT));

        searchField = new GuiTextField(fontRendererObj, rightX, SEARCH_Y, 176, 16);
        searchField.setText(search);

        int tabX = width / 2 - 180;
        int tabWidth = 120;
        buttonList.add(new GuiButton(TAB_ALL, tabX, TAB_Y, tabWidth, TAB_HEIGHT, translate("view.all")));
        buttonList
            .add(new GuiButton(TAB_ACTIVE, tabX + tabWidth, TAB_Y, tabWidth, TAB_HEIGHT, translate("view.active")));
        buttonList.add(
            new GuiButton(TAB_FINISHED, tabX + tabWidth * 2, TAB_Y, tabWidth, TAB_HEIGHT, translate("view.finished")));

        for (int i = 0; i < rowsVisible; i++) {
            buttonList.add(new GuiButton(10 + i, leftX, LIST_TOP + i * ROW_HEIGHT, 176, ROW_HEIGHT - 2, ""));
        }
        buttonList.add(new GuiButton(1, leftX, pagerY, 20, 20, "<"));
        buttonList.add(new GuiButton(2, leftX + 156, pagerY, 20, 20, ">"));

        for (int i = 0; i < rowsVisible; i++) {
            buttonList.add(new GuiButton(50 + i, rightX, LIST_TOP + i * ROW_HEIGHT, 176, ROW_HEIGHT - 2, ""));
        }
        buttonList.add(new GuiButton(3, rightX, pagerY, 20, 20, "<"));
        buttonList.add(new GuiButton(4, rightX + 156, pagerY, 20, 20, ">"));

        buttonList.add(new GuiButton(99, width / 2 - 40, height - 30, 80, 20, translate("cancel")));

        refreshPlayerQuestState();
        refreshCategoryList(selectedCategoryId);
        refreshQuestList(false);
        updateButtons();
    }

    private void refreshPlayerQuestState() {
        activeQuestIds.clear();
        finishedQuestIds.clear();
        if (mc == null || mc.thePlayer == null) return;

        PlayerData playerData = PlayerData.get(mc.thePlayer);
        if (playerData == null || playerData.questData == null) return;

        addQuestIds(activeQuestIds, playerData.questData.getActiveQuests());
        addQuestIds(finishedQuestIds, playerData.questData.getFinishedQuests());
    }

    private static void addQuestIds(Set<Integer> destination, IQuest[] source) {
        if (source == null) return;
        for (IQuest quest : source) {
            if (quest != null) destination.add(quest.getId());
        }
    }

    private void refreshCategoryList(int selectedCategoryId) {
        categories = new ArrayList<QuestCategory>();
        for (QuestCategory category : QuestController.Instance.categories.values()) {
            if (selectedView == QuestView.ALL || categoryHasMatchingQuest(category)) categories.add(category);
        }
        Collections.sort(categories, new Comparator<QuestCategory>() {

            @Override
            public int compare(QuestCategory a, QuestCategory b) {
                return a.title.compareToIgnoreCase(b.title);
            }
        });

        selectedCategory = findCategoryById(categories, selectedCategoryId);
        catScroll = QuestPickerLogic.clampOffset(catScroll, categories.size(), rowsVisible);
    }

    private boolean categoryHasMatchingQuest(QuestCategory category) {
        for (Quest quest : category.quests.values()) {
            if (matchesSelectedView(quest)) return true;
        }
        return false;
    }

    private boolean matchesSelectedView(Quest quest) {
        if (selectedView == QuestView.ACTIVE) return activeQuestIds.contains(quest.id);
        if (selectedView == QuestView.FINISHED) {
            return QuestPickerLogic.isFinishedOnly(quest.id, activeQuestIds, finishedQuestIds);
        }
        return true;
    }

    private void refreshQuestList(boolean resetScroll) {
        quests = new ArrayList<Quest>();
        if (selectedCategory != null) {
            String normalizedSearch = search.toLowerCase(Locale.ROOT);
            for (Quest quest : selectedCategory.quests.values()) {
                if (matchesSelectedView(quest) && (normalizedSearch.isEmpty() || quest.title.toLowerCase(Locale.ROOT)
                    .contains(normalizedSearch))) {
                    quests.add(quest);
                }
            }
        }
        Collections.sort(quests, new Comparator<Quest>() {

            @Override
            public int compare(Quest a, Quest b) {
                return a.title.compareToIgnoreCase(b.title);
            }
        });
        questScroll = resetScroll ? 0 : QuestPickerLogic.clampOffset(questScroll, quests.size(), rowsVisible);
    }

    static QuestCategory findCategoryById(List<QuestCategory> availableCategories, int categoryId) {
        List<Integer> categoryIds = new ArrayList<Integer>();
        for (QuestCategory category : availableCategories) {
            categoryIds.add(category.id);
        }
        int categoryIndex = QuestPickerLogic.findIdIndex(categoryIds, categoryId);
        return categoryIndex < 0 ? null : availableCategories.get(categoryIndex);
    }

    private int getSelectedCategoryId() {
        return selectedCategory == null ? -1 : selectedCategory.id;
    }

    private void updateButtons() {
        for (int i = 0; i < rowsVisible; i++) {
            GuiButton catButton = getButton(10 + i);
            int catIndex = catScroll + i;
            if (catIndex < categories.size()) {
                catButton.visible = true;
                catButton.displayString = trimToButtonWidth(categories.get(catIndex).title, catButton.getButtonWidth());
            } else {
                catButton.visible = false;
            }

            GuiButton questButton = getButton(50 + i);
            int questIndex = questScroll + i;
            if (questIndex < quests.size()) {
                questButton.visible = true;
                questButton.displayString = trimToButtonWidth(
                    quests.get(questIndex).title,
                    questButton.getButtonWidth());
            } else {
                questButton.visible = false;
            }
        }

        getButton(1).enabled = catScroll > 0;
        getButton(2).enabled = catScroll < Math.max(0, categories.size() - rowsVisible);
        getButton(3).enabled = questScroll > 0;
        getButton(4).enabled = questScroll < Math.max(0, quests.size() - rowsVisible);
        getButton(TAB_ALL).enabled = selectedView != QuestView.ALL;
        getButton(TAB_ACTIVE).enabled = selectedView != QuestView.ACTIVE;
        getButton(TAB_FINISHED).enabled = selectedView != QuestView.FINISHED;
    }

    /** Truncates long row labels because GuiButton does not clamp its text. */
    private String trimToButtonWidth(String text, int buttonWidth) {
        int maxWidth = buttonWidth - 6;
        if (fontRendererObj.getStringWidth(text) <= maxWidth) return text;
        while (text.length() > 1 && fontRendererObj.getStringWidth(text + "…") > maxWidth) {
            text = text.substring(0, text.length() - 1);
        }
        return text + "…";
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
        if (button.id == TAB_ALL || button.id == TAB_ACTIVE || button.id == TAB_FINISHED) {
            QuestView requestedView = button.id == TAB_ACTIVE ? QuestView.ACTIVE
                : button.id == TAB_FINISHED ? QuestView.FINISHED : QuestView.ALL;
            if (requestedView != selectedView) {
                int selectedCategoryId = getSelectedCategoryId();
                selectedView = requestedView;
                if (selectedView != QuestView.ALL) refreshPlayerQuestState();
                refreshCategoryList(selectedCategoryId);
                refreshQuestList(false);
                updateButtons();
            }
            return;
        }
        if (button.id == 1 || button.id == 2) {
            catScroll += button.id == 1 ? -1 : 1;
            catScroll = QuestPickerLogic.clampOffset(catScroll, categories.size(), rowsVisible);
            updateButtons();
            return;
        }
        if (button.id == 3 || button.id == 4) {
            questScroll += button.id == 3 ? -1 : 1;
            questScroll = QuestPickerLogic.clampOffset(questScroll, quests.size(), rowsVisible);
            updateButtons();
            return;
        }
        if (button.id >= 10 && button.id < 10 + rowsVisible) {
            int catIndex = catScroll + (button.id - 10);
            if (catIndex < categories.size()) {
                selectedCategory = categories.get(catIndex);
                refreshQuestList(true);
                updateButtons();
            }
            return;
        }
        if (button.id >= 50 && button.id < 50 + rowsVisible) {
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

        drawCenteredString(fontRendererObj, translate("title"), width / 2, TITLE_Y, 0xFFFFFF);
        drawString(fontRendererObj, translate("category"), leftX, HEADING_Y, 0xAAAAAA);
        drawString(fontRendererObj, translate("search"), rightX, HEADING_Y, 0xAAAAAA);
        searchField.drawTextBox();
        drawEmptyState(rightX);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawEmptyState(int rightX) {
        String message = null;
        if (categories.isEmpty()) {
            if (selectedView == QuestView.ACTIVE) message = translate("empty.active");
            else if (selectedView == QuestView.FINISHED) message = translate("empty.finished");
            else message = translate("empty.general");
        } else if (selectedCategory == null) {
            message = translate("selectCategory");
        } else if (quests.isEmpty() && !search.isEmpty()) {
            message = translate("empty.search");
        } else if (quests.isEmpty()) {
            message = translate("empty.general");
        }
        if (message != null) {
            drawCenteredString(fontRendererObj, message, rightX + 88, LIST_TOP + 4, 0xAAAAAA);
        }
    }

    private static String translate(String suffix) {
        return StatCollector.translateToLocal("questpointer.gui.questPicker." + suffix);
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
                refreshQuestList(true);
                updateButtons();
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
