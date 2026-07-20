package com.github.postyizhan.questpointer;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public class Config {

    private static final String CATEGORY_OVERLAY = "overlay";

    /** Screen position/scale of the direction bar overlay. */
    public static float overlayPosX = 50f;
    public static float overlayPosY = 5f;
    public static float overlayScale = 1.0f;
    public static int overlayWidth = 160;

    private static Configuration config;
    private static Property overlayPosXProp;
    private static Property overlayPosYProp;
    private static Property overlayScaleProp;
    private static Property overlayWidthProp;

    public static void synchronizeConfiguration(File configFile) {
        config = new Configuration(configFile);

        overlayPosXProp = config.get(
            CATEGORY_OVERLAY,
            "posX",
            overlayPosX,
            "Horizontal position of the pointer overlay's CENTER, in percent of screen width (0-100).");
        overlayPosX = (float) overlayPosXProp.getDouble();

        overlayPosYProp = config.get(
            CATEGORY_OVERLAY,
            "posY",
            overlayPosY,
            "Vertical position of the pointer overlay, in percent of screen height (0-100).");
        overlayPosY = (float) overlayPosYProp.getDouble();

        overlayScaleProp = config
            .get(CATEGORY_OVERLAY, "scale", overlayScale, "Scale multiplier applied to the pointer overlay.");
        overlayScale = (float) overlayScaleProp.getDouble();

        overlayWidthProp = config
            .get(CATEGORY_OVERLAY, "width", overlayWidth, "Width, in pixels, of the pointer overlay bar.");
        overlayWidth = overlayWidthProp.getInt();

        if (config.hasChanged()) {
            config.save();
        }
    }

    public static void save() {
        if (config == null) return;
        overlayPosXProp.set((double) overlayPosX);
        overlayPosYProp.set((double) overlayPosY);
        overlayScaleProp.set((double) overlayScale);
        overlayWidthProp.set(overlayWidth);
        if (config.hasChanged()) config.save();
    }
}
