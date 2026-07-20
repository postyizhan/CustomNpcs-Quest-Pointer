package com.github.postyizhan.questpointer;

import net.minecraft.entity.player.EntityPlayer;

/**
 * Access control for the coordinate recorder: any player in creative mode may
 * use it. This intentionally does not go through CustomNPC+'s permission system,
 * since the recorder is meant to be usable by any builder/admin with creative
 * access, not gated behind a separate permission node.
 */
public class QuestPointerPermissions {

    private QuestPointerPermissions() {}

    public static boolean canUseRecorder(EntityPlayer player) {
        return player != null && player.capabilities.isCreativeMode;
    }
}
