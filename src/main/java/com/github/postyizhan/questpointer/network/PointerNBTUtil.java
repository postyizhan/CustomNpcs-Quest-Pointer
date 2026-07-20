package com.github.postyizhan.questpointer.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import com.github.postyizhan.questpointer.quest.PointerEntry;

import io.netty.buffer.ByteBuf;

/** Helpers to move {@link PointerEntry} lists over the wire via NBT, reusing FML's NBT ByteBuf codec. */
public class PointerNBTUtil {

    public static void writePoints(ByteBuf buf, List<PointerEntry> points) {
        NBTTagCompound wrapper = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (PointerEntry entry : points) {
            list.appendTag(entry.writeToNBT());
        }
        wrapper.setTag("Points", list);
        cpw.mods.fml.common.network.ByteBufUtils.writeTag(buf, wrapper);
    }

    public static List<PointerEntry> readPoints(ByteBuf buf) {
        NBTTagCompound wrapper = cpw.mods.fml.common.network.ByteBufUtils.readTag(buf);
        List<PointerEntry> points = new ArrayList<PointerEntry>();
        if (wrapper == null) return points;
        NBTTagList list = wrapper.getTagList("Points", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            points.add(PointerEntry.readFromNBT(list.getCompoundTagAt(i)));
        }
        return points;
    }
}
