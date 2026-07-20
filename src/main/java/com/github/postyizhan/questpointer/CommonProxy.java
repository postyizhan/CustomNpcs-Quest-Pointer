package com.github.postyizhan.questpointer;

import com.github.postyizhan.questpointer.item.ItemCoordinateRecorder;
import com.github.postyizhan.questpointer.network.NetworkHandler;
import com.github.postyizhan.questpointer.quest.QuestPointerController;
import com.github.postyizhan.questpointer.quest.QuestTrackingTicker;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public class CommonProxy {

    public void preInit(FMLPreInitializationEvent event) {
        Config.synchronizeConfiguration(event.getSuggestedConfigurationFile());

        GameRegistry.registerItem(ItemCoordinateRecorder.INSTANCE, "coordinateRecorder");
        NetworkHandler.init();
        // TickEvent.ServerTickEvent is posted on FMLCommonHandler's bus, NOT on
        // MinecraftForge.EVENT_BUS - registering there means onServerTick() would
        // silently never fire and the tracked-quest poll would be dead code.
        // Registered once at mod load (preInit runs a single time per game launch), the
        // ticker itself re-reads the player list every tick so it survives world reloads.
        FMLCommonHandler.instance()
            .bus()
            .register(new QuestTrackingTicker());

        QuestPointerMod.LOG.info("QuestPointer preInit complete");
    }

    public void init(FMLInitializationEvent event) {}

    public void postInit(FMLPostInitializationEvent event) {}

    public void serverStarting(FMLServerStartingEvent event) {}

    public void serverStarted(FMLServerStartedEvent event) {
        // Runs after CustomNPC+'s own FMLServerStartedEvent handler (we declare
        // required-after:customnpcs, so FML calls our lifecycle events later),
        // guaranteeing QuestController.Instance.quests is already populated.
        QuestPointerController.INSTANCE.load();
    }
}
