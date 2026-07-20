package com.github.postyizhan.questpointer.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import com.github.postyizhan.questpointer.Config;
import com.github.postyizhan.questpointer.quest.PointerEntry;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Renders a horizontal direction bar for the player's currently tracked quest
 * (as reported by CustomNPC+'s quest log tracking). One icon per pointer entry
 * in the same dimension as the player, sliding left/right as the player turns.
 * Points in a different dimension show a text hint above the bar instead.
 */
public class QuestPointerOverlay {

    private static final ResourceLocation ICON = new ResourceLocation("questpointer", "textures/gui/pointer.png");
    private static final int BAR_HEIGHT = 20;
    private static final int BASE_ICON_SIZE = 8;
    private static final int MAX_ICON_SIZE = 16;
    private static final float SCALE_DISTANCE_MIN = 10f;
    private static final float SCALE_DISTANCE_MAX = 60f;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;
        if (!QuestPointerClientData.isTracking()) return;

        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) return;

        List<PointerEntry> allPoints = QuestPointerClientData.getTrackedPoints();
        if (allPoints.isEmpty()) return;

        List<PointerEntry> samedimPoints = new ArrayList<PointerEntry>();
        List<PointerEntry> otherDimPoints = new ArrayList<PointerEntry>();
        for (PointerEntry entry : allPoints) {
            if (entry.dimension == player.dimension) samedimPoints.add(entry);
            else otherDimPoints.add(entry);
        }

        ScaledResolution res = event.resolution;
        int barWidth = Config.overlayWidth;
        // posX/posY are the percent-of-screen position of the BAR'S CENTER, not its
        // top-left corner - otherwise a default of 50% visibly places the bar off to
        // the right half of the screen instead of centering it.
        int centerScreenX = (int) (Config.overlayPosX / 100F * res.getScaledWidth());
        int actualX = centerScreenX - barWidth / 2;
        int actualY = (int) (Config.overlayPosY / 100F * res.getScaledHeight());
        float scale = Config.overlayScale;

        float partialTicks = event.partialTicks;
        double px = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks;
        double pz = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks;
        float yaw = player.prevRotationYaw + (player.rotationYaw - player.prevRotationYaw) * partialTicks;

        GL11.glPushMatrix();
        GL11.glTranslatef(actualX, actualY, 0);
        GL11.glScalef(scale, scale, scale);

        if (!otherDimPoints.isEmpty()) {
            drawOtherDimensionHint(mc, otherDimPoints, barWidth);
        }

        if (!samedimPoints.isEmpty()) {
            Gui.drawRect(0, 0, barWidth, BAR_HEIGHT, 0x80000000);

            final double fpx = px;
            final double fpz = pz;
            samedimPoints.sort(
                Comparator
                    .comparingDouble((PointerEntry e) -> -((fpx - e.x) * (fpx - e.x) + (fpz - e.z) * (fpz - e.z))));

            for (PointerEntry entry : samedimPoints) {
                renderMarkIcon(mc, entry, px, pz, yaw, barWidth);
            }
        }

        GL11.glPopMatrix();
    }

    private void drawOtherDimensionHint(Minecraft mc, List<PointerEntry> otherDimPoints, int barWidth) {
        // Show the nearest-by-dimension-name hint; multiple different dimensions are deduped by name.
        java.util.LinkedHashSet<String> names = new java.util.LinkedHashSet<String>();
        for (PointerEntry entry : otherDimPoints) {
            names.add(DimensionNameUtil.getName(entry.dimension));
        }
        String text = "目标位于: " + String.join(", ", names);
        int textWidth = mc.fontRenderer.getStringWidth(text);
        mc.fontRenderer.drawStringWithShadow(text, (barWidth - textWidth) / 2, -12, 0xFFFF55);
    }

    private void renderMarkIcon(Minecraft mc, PointerEntry entry, double playerX, double playerZ, float playerYaw,
        int barWidth) {
        Float iconPos = calculateIconPosition(entry.x, entry.z, playerX, playerZ, playerYaw, barWidth);
        if (iconPos == null) return;

        float distance = (float) Math
            .sqrt((entry.x - playerX) * (entry.x - playerX) + (entry.z - playerZ) * (entry.z - playerZ));

        int iconSize = calculateIconSize(distance);
        int iconX = MathHelper.clamp_int((int) iconPos.floatValue(), iconSize / 2, barWidth - iconSize / 2);
        int iconY = (BAR_HEIGHT - iconSize) / 2;

        renderTexturedIcon(mc, iconX - iconSize / 2, iconY, iconSize);
    }

    private void renderTexturedIcon(Minecraft mc, int x, int y, int size) {
        GL11.glPushMatrix();
        GL11.glColor4f(1f, 1f, 1f, 1f);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        mc.getTextureManager()
            .bindTexture(ICON);

        Tessellator tessellator = Tessellator.instance;
        tessellator.startDrawingQuads();
        tessellator.addVertexWithUV(x, y + size, 0, 0, 1);
        tessellator.addVertexWithUV(x + size, y + size, 0, 1, 1);
        tessellator.addVertexWithUV(x + size, y, 0, 1, 0);
        tessellator.addVertexWithUV(x, y, 0, 0, 0);
        tessellator.draw();

        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private Float calculateIconPosition(double targetX, double targetZ, double playerX, double playerZ, float playerYaw,
        int barWidth) {
        double dx = targetX - playerX;
        double dz = targetZ - playerZ;

        double angleToTarget = Math.toDegrees(Math.atan2(dz, dx));
        double adjustedPlayerYaw = playerYaw + 90;
        double relativeAngle = angleToTarget - adjustedPlayerYaw;
        while (relativeAngle < -180) relativeAngle += 360;
        while (relativeAngle > 180) relativeAngle -= 360;

        if (Math.abs(relativeAngle) > 90) {
            return null;
        }

        return (float) (barWidth / 2.0 + (relativeAngle / 90.0) * (barWidth / 2.0));
    }

    private int calculateIconSize(float distance) {
        if (distance <= SCALE_DISTANCE_MIN) return MAX_ICON_SIZE;
        if (distance >= SCALE_DISTANCE_MAX) return BASE_ICON_SIZE;

        float t = (distance - SCALE_DISTANCE_MIN) / (SCALE_DISTANCE_MAX - SCALE_DISTANCE_MIN);
        return (int) (MAX_ICON_SIZE - (MAX_ICON_SIZE - BASE_ICON_SIZE) * t);
    }
}
