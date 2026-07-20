package com.github.postyizhan.questpointer.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import com.github.postyizhan.questpointer.QuestPointerPermissions;
import com.github.postyizhan.questpointer.network.NetworkHandler;
import com.github.postyizhan.questpointer.network.packet.OpenRecorderGuiPacket;

/**
 * Admin tool: right-click a block to open the recorder GUI for that position.
 * Any player in creative mode may use it; the same check is re-applied
 * server-side in every packet handler that mutates data.
 */
public class ItemCoordinateRecorder extends Item {

    public static final ItemCoordinateRecorder INSTANCE = new ItemCoordinateRecorder();

    private ItemCoordinateRecorder() {
        setUnlocalizedName("questpointer.coordinateRecorder");
        setTextureName("questpointer:coordinateRecorder");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (world.isRemote) return false;

        if (!(player instanceof EntityPlayerMP)) return false;

        if (!QuestPointerPermissions.canUseRecorder(player)) {
            player.addChatMessage(new ChatComponentTranslation("availability.permission"));
            return false;
        }

        NetworkHandler.CHANNEL.sendTo(new OpenRecorderGuiPacket(player.dimension, x, y, z), (EntityPlayerMP) player);
        return true;
    }
}
