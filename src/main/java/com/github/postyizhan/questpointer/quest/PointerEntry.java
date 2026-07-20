package com.github.postyizhan.questpointer.quest;

import net.minecraft.nbt.NBTTagCompound;

/**
 * A single waypoint attached to a quest: a position in a dimension plus an
 * optional admin-facing label (e.g. "Entrance", "Boss room").
 */
public class PointerEntry {

    public int dimension;
    public double x;
    public double y;
    public double z;
    public String label;

    public PointerEntry() {
        this(0, 0, 0, 0, "");
    }

    public PointerEntry(int dimension, double x, double y, double z, String label) {
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.label = label == null ? "" : label;
    }

    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("Dimension", dimension);
        tag.setDouble("X", x);
        tag.setDouble("Y", y);
        tag.setDouble("Z", z);
        tag.setString("Label", label);
        return tag;
    }

    public static PointerEntry readFromNBT(NBTTagCompound tag) {
        PointerEntry entry = new PointerEntry();
        entry.dimension = tag.getInteger("Dimension");
        entry.x = tag.getDouble("X");
        entry.y = tag.getDouble("Y");
        entry.z = tag.getDouble("Z");
        entry.label = tag.getString("Label");
        return entry;
    }
}
