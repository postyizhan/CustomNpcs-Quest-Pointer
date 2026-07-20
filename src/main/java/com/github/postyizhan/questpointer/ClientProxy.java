package com.github.postyizhan.questpointer;

import net.minecraftforge.common.MinecraftForge;

import com.github.postyizhan.questpointer.client.QuestPointerOverlay;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        super.preInit(event);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        super.init(event);
        MinecraftForge.EVENT_BUS.register(new QuestPointerOverlay());
    }
}
