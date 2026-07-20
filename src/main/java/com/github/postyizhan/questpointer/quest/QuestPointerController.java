package com.github.postyizhan.questpointer.quest;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.github.postyizhan.questpointer.QuestPointerMod;

import noppes.npcs.CustomNpcs;
import noppes.npcs.controllers.QuestController;

/**
 * Owns the persistent map of questId -> list of {@link PointerEntry}. Storage is
 * intentionally kept separate from CustomNPC+'s own quest files: we only reference
 * quests by id and never touch CNPC+'s data on disk.
 */
public class QuestPointerController {

    public static final QuestPointerController INSTANCE = new QuestPointerController();

    private final Map<Integer, List<PointerEntry>> pointsByQuest = new HashMap<Integer, List<PointerEntry>>();

    private QuestPointerController() {}

    public void load() {
        pointsByQuest.clear();
        File file = getSaveFile();
        if (file == null || !file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);
            NBTTagList questList = root.getTagList("Quests", 10);
            for (int i = 0; i < questList.tagCount(); i++) {
                NBTTagCompound questTag = questList.getCompoundTagAt(i);
                int questId = questTag.getInteger("QuestId");
                NBTTagList pointList = questTag.getTagList("Points", 10);
                List<PointerEntry> entries = new ArrayList<PointerEntry>();
                for (int j = 0; j < pointList.tagCount(); j++) {
                    entries.add(PointerEntry.readFromNBT(pointList.getCompoundTagAt(j)));
                }
                pointsByQuest.put(questId, entries);
            }
        } catch (Exception e) {
            QuestPointerMod.LOG.error("Failed to load quest pointer data", e);
        }

        pruneOrphans();
    }

    public void save() {
        File file = getSaveFile();
        if (file == null) return;

        NBTTagCompound root = new NBTTagCompound();
        NBTTagList questList = new NBTTagList();
        for (Map.Entry<Integer, List<PointerEntry>> entry : pointsByQuest.entrySet()) {
            NBTTagCompound questTag = new NBTTagCompound();
            questTag.setInteger("QuestId", entry.getKey());
            NBTTagList pointList = new NBTTagList();
            for (PointerEntry point : entry.getValue()) {
                pointList.appendTag(point.writeToNBT());
            }
            questTag.setTag("Points", pointList);
            questList.appendTag(questTag);
        }
        root.setTag("Quests", questList);

        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) dir.mkdirs();
            File tmp = new File(file.getParentFile(), file.getName() + "_new");
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                CompressedStreamTools.writeCompressed(root, fos);
            }
            if (file.exists()) file.delete();
            tmp.renameTo(file);
        } catch (Exception e) {
            QuestPointerMod.LOG.error("Failed to save quest pointer data", e);
        }
    }

    /** Removes point lists for quests that no longer exist in CustomNPC+'s QuestController. */
    private void pruneOrphans() {
        boolean changed = false;
        java.util.Iterator<Integer> it = pointsByQuest.keySet()
            .iterator();
        while (it.hasNext()) {
            int questId = it.next();
            if (!QuestController.Instance.quests.containsKey(questId)) {
                it.remove();
                changed = true;
            }
        }
        if (changed) save();
    }

    public List<PointerEntry> getPoints(int questId) {
        List<PointerEntry> entries = pointsByQuest.get(questId);
        return entries == null ? new ArrayList<PointerEntry>() : new ArrayList<PointerEntry>(entries);
    }

    public boolean hasPoints(int questId) {
        List<PointerEntry> entries = pointsByQuest.get(questId);
        return entries != null && !entries.isEmpty();
    }

    public void setPoints(int questId, List<PointerEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            pointsByQuest.remove(questId);
        } else {
            pointsByQuest.put(questId, new ArrayList<PointerEntry>(entries));
        }
        save();
    }

    public void addPoint(int questId, PointerEntry entry) {
        List<PointerEntry> entries = pointsByQuest.get(questId);
        if (entries == null) {
            entries = new ArrayList<PointerEntry>();
            pointsByQuest.put(questId, entries);
        }
        entries.add(entry);
        save();
    }

    private File getSaveFile() {
        File dir = CustomNpcs.getWorldSaveDirectory();
        if (dir == null) return null;
        return new File(dir, "questpointer.dat");
    }
}
