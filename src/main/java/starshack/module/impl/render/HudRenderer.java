package starshack.module.impl.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemPotion;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.EnumChatFormatting;
import org.lwjgl.opengl.GL11;
import starshack.module.Module;
import starshack.module.ModuleManager;
import starshack.module.impl.combat.KillAura;
import starshack.utility.font.StarsFontRenderer;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

final class HudRenderer {
    private static final Minecraft MC = Minecraft.getMinecraft();
    private static final SimpleDateFormat CLOCK = new SimpleDateFormat("HH:mm");

    private HudRenderer() {
    }

    static boolean isActive() {
        return true;
    }

    static void render() {
        ScaledResolution resolution = new ScaledResolution(MC);
        if (enabled(HUD.starsFps)) drawFps(resolution);
        if (enabled(HUD.starsTime)) drawTime();
        if (enabled(HUD.starsArmor)) drawArmor(resolution);
        if (enabled(HUD.starsPotions)) drawPotions(resolution);
        if (enabled(HUD.starsUserInfo)) drawUserInfo(resolution);
        if (enabled(HUD.starsName)) drawName();
        if (enabled(HUD.starsCoords) || enabled(HUD.starsSpeed)) drawBottomLeft(resolution);
        if (enabled(HUD.starsTargets)) drawTargets();
        if (enabled(HUD.starsModuleList)) drawArrayList(resolution);
        if (enabled(HUD.starsInventory)) drawInventory();
    }

    static void drawEditorPreview() {
        ScaledResolution resolution = new ScaledResolution(MC);
        drawArrayList(resolution);
        drawInventory();
        drawTargets();
    }

    static int getInventoryWidth() {
        return 167;
    }

    static int getInventoryHeight() {
        return 73;
    }

    static int getTargetsWidth() {
        EntityLivingBase target = KillAura.attackingEntity;
        return Math.max(100, target == null ? 100 : width(target.getName()) + 58);
    }

    static int getTargetsHeight() {
        return KillAura.attackingEntity == null ? 14 : 28;
    }

    private static void drawArrayList(ScaledResolution resolution) {
        List<HudEntry> entries = new ArrayList<HudEntry>();
        int alpha = (int) HUD.starsBackgroundAlpha.getInput();
        int background = new Color(0, 0, 0, alpha).getRGB();
        for (Module module : ModuleManager.modules) {
            boolean hideVisual = HUD.hideVisualsFromList != null && HUD.hideVisualsFromList.isToggled()
                    && module.moduleCategory() == Module.category.visuals;
            if (module.isEnabled() && module != ModuleManager.hud && !module.isHidden() && !hideVisual) {
                entries.add(new HudEntry(module));
            }
        }
        entries.sort(HudEntry.COMPARE_BY_WIDTH);

        float scale = scale();
        GL11.glPushMatrix();
        GL11.glScalef(scale, scale, 1.0f);
        float screenWidth = resolution.getScaledWidth() / scale;
        float xAnchor = HUD.posX / scale;
        float y = HUD.posY / scale;
        int row = 0;
        for (HudEntry entry : entries) {
            int textWidth = entry.textWidth;
            int fontHeight = height();
            float x = screenWidth - textWidth - 4.0f;
            // A manually moved module list keeps its original horizontal anchor when it is not default.
            if (HUD.getRelativePosX() < 0.98f) x = xAnchor;
            int accent = arrayColor(row, 255);
            int mode = (int) HUD.starsBackground.getInput();
            float top = y - (row == 0 ? 2 : 0);
            float right = x + textWidth + 4;
            if (mode == 0 || mode == 2)
                Gui.drawRect((int) (x - 3), (int) top, (int) (x - 2), (int) (y + fontHeight + 2), accent);
            if (mode != 1) Gui.drawRect((int) (x - 2), (int) top, (int) right, (int) (y + fontHeight + 2), background);
            if (mode == 3) Gui.drawRect((int) (right - 1), (int) top, (int) right, (int) (y + fontHeight + 2), accent);
            if (mode == 0 && row == entries.size() - 1)
                Gui.drawRect((int) (x - 3), (int) (y + fontHeight + 1), (int) right, (int) (y + fontHeight + 2), accent);
            draw(entry.label, x, y + 1, accent, true);
            y += fontHeight + 2;
            row++;
        }
        GL11.glPopMatrix();
    }

    private static void drawName() {
        String name = value(HUD.starsClientName, "StarShack");
        int x = 2;
        long time = System.currentTimeMillis();
        for (int i = 0; i < name.length(); i++) {
            String character = String.valueOf(name.charAt(i));
            draw(character, x, 4, dynamicColor(time - i * 300L), true);
            x += width(character);
        }
    }

    private static void drawTime() {
        String name = value(HUD.starsClientName, "StarShack");
        int x = enabled(HUD.starsName) ? 4 + width(name) : 3;
        draw(EnumChatFormatting.GRAY + "(" + EnumChatFormatting.WHITE + CLOCK.format(new Date()) + EnumChatFormatting.GRAY + ")",
                x, 4, HUD.getHudColor(0), true);
    }

    private static void drawFps(ScaledResolution resolution) {
        int offset = bottomLeftOffset();
        draw(EnumChatFormatting.RESET + "FPS: " + EnumChatFormatting.WHITE + Minecraft.getDebugFPS(), 2,
                resolution.getScaledHeight() - height() - offset, HUD.getHudColor(0), true);
    }

    private static void drawBottomLeft(ScaledResolution resolution) {
        int y = resolution.getScaledHeight() - height() - 1;
        if (enabled(HUD.starsCoords)) {
            String xyz = String.format("%sXYZ:%s %.0f %.0f %.0f", EnumChatFormatting.RESET, EnumChatFormatting.WHITE,
                    MC.thePlayer.posX, MC.thePlayer.getEntityBoundingBox().minY, MC.thePlayer.posZ);
            draw(xyz, 2, y, HUD.getHudColor(0), true);
            y -= height() + 2;
        }
        if (enabled(HUD.starsSpeed)) {
            double speed = Math.sqrt(MC.thePlayer.motionX * MC.thePlayer.motionX + MC.thePlayer.motionZ * MC.thePlayer.motionZ) * 20.0;
            draw("Speed: " + EnumChatFormatting.WHITE + String.format("%.2f b/s", speed), 2, y, HUD.getHudColor(0), true);
        }
    }

    private static int bottomLeftOffset() {
        int count = (enabled(HUD.starsCoords) ? 1 : 0) + (enabled(HUD.starsSpeed) ? 1 : 0);
        return count * (height() + 2);
    }

    private static void drawUserInfo(ScaledResolution resolution) {
        String info = EnumChatFormatting.GRAY + "Build - " + EnumChatFormatting.WHITE + "#26831"
                + EnumChatFormatting.GRAY + " | UID - " + EnumChatFormatting.WHITE + MC.thePlayer.getName();
        draw(info, resolution.getScaledWidth() - width(info) - 2, resolution.getScaledHeight() - height() - 2, 0xFFFFFFFF, true);
    }

    private static void drawPotions(ScaledResolution resolution) {
        int y = resolution.getScaledHeight() - height() - (enabled(HUD.starsUserInfo) ? height() + 14 : 12);
        int index = 0;
        for (PotionEffect effect : MC.thePlayer.getActivePotionEffects()) {
            Potion potion = Potion.potionTypes[effect.getPotionID()];
            if (potion == null) continue;
            String level = effect.getAmplifier() > 0 ? " " + (effect.getAmplifier() + 1) : "";
            String label = potion.getName().replace("potion.", "") + level + " " + EnumChatFormatting.GRAY + Potion.getDurationString(effect);
            draw(label, resolution.getScaledWidth() - width(label) - 2, y, arrayColor(index++, 255), true);
            y -= height() + 1;
        }
    }

    private static void drawArmor(ScaledResolution resolution) {
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        for (int index = 3; index >= 0; index--) {
            ItemStack stack = MC.thePlayer.inventory.armorInventory[index];
            if (stack != null) stacks.add(stack);
        }
        if (MC.thePlayer.getCurrentEquippedItem() != null) stacks.add(MC.thePlayer.getCurrentEquippedItem());
        int x = resolution.getScaledWidth() / 2 - stacks.size() * 8;
        int y = resolution.getScaledHeight() - 57;
        RenderHelper.enableGUIStandardItemLighting();
        for (ItemStack stack : stacks) {
            MC.getRenderItem().renderItemAndEffectIntoGUI(stack, x, y);
            if (stack.isStackable()) draw(String.valueOf(stack.stackSize), x + 9, y + 10, HUD.getHudColor(0), true);
            else if (!(stack.getItem() instanceof ItemPotion))
                draw(String.valueOf(stack.getMaxDamage() - stack.getItemDamage()), x + 9, y + 10, HUD.getHudColor(0), true);
            x += 16;
        }
        RenderHelper.disableStandardItemLighting();
    }

    private static void drawInventory() {
        int x = (int) HUD.starsInventoryX.getInput();
        int y = (int) HUD.starsInventoryY.getInput();
        Gui.drawRect(x, y, x + 167, y + 73, 0xFF1D1D1D);
        Gui.drawRect(x + 1, y + 13, x + 166, y + 72, 0xFF282828);
        draw("Your Inventory", x + 3, y + 3, 0xFFFFFFFF, true);
        RenderHelper.enableGUIStandardItemLighting();
        for (int slot = 9; slot < 36; slot++) {
            ItemStack stack = MC.thePlayer.inventory.mainInventory[slot];
            if (stack == null) continue;
            int cell = slot - 9;
            int itemX = x + 3 + (cell % 9) * 18;
            int itemY = y + 16 + (cell / 9) * 18;
            MC.getRenderItem().renderItemAndEffectIntoGUI(stack, itemX, itemY);
            MC.getRenderItem().renderItemOverlayIntoGUI(MC.fontRendererObj, stack, itemX, itemY, null);
        }
        RenderHelper.disableStandardItemLighting();
    }

    private static void drawTargets() {
        int x = (int) HUD.starsTargetsX.getInput();
        int y = (int) HUD.starsTargetsY.getInput();
        EntityLivingBase target = KillAura.attackingEntity;
        int width = getTargetsWidth();
        Gui.drawRect(x, y - 13, x + width, y + getTargetsHeight(), 0xFF1D1D1D);
        Gui.drawRect(x + 1, y, x + width - 1, y + getTargetsHeight() - 1, 0xFF282828);
        draw("Your Targets", x + 3, y - 10, 0xFFFFFFFF, true);
        if (target != null) {
            String distance = (int) MC.thePlayer.getDistanceToEntity(target) + "m";
            draw(target.getName(), x + 3, y + 3, 0xFFC8C8C8, false);
            draw(distance, x + width - width(distance) - 3, y + 3, 0xFFC8C8C8, false);
        }
    }

    private static final class HudEntry {
        static final Comparator<HudEntry> COMPARE_BY_WIDTH = new Comparator<HudEntry>() {
            @Override
            public int compare(HudEntry a, HudEntry b) {
                return Integer.compare(b.textWidth, a.textWidth);
            }
        };

        final String label;
        final int textWidth;

        HudEntry(Module module) {
            module.getInfoUpdate();
            this.label = moduleLabel(module);
            this.textWidth = width(this.label);
        }
    }

    private static String moduleLabel(Module module) {
        String name = module.getNameInHud();
        if (HUD.lowercase != null && HUD.lowercase.isToggled()) name = name.toLowerCase();
        if (HUD.showInfo == null || !HUD.showInfo.isToggled() || module.getInfo().isEmpty()) return name;
        String suffix = module.getInfo();
        switch ((int) HUD.starsSuffix.getInput()) {
            case 1:
                return name + " " + EnumChatFormatting.GRAY + "- " + suffix;
            case 2:
                return name + " " + EnumChatFormatting.GRAY + "[" + suffix + "]";
            case 3:
                return name;
            default:
                return name + " " + EnumChatFormatting.GRAY + suffix;
        }
    }

    private static int arrayColor(int index, int alpha) {
        int mode = HUD.starsMode == null ? 0 : (int) HUD.starsMode.getInput();
        if (mode == 0) return withAlpha(HUD.getHudColor(0), alpha);
        if (mode == 1) return dynamicColor(System.currentTimeMillis() - index * 300L, alpha);
        float hue = (float) ((System.currentTimeMillis() - index * 110L) % 3600L) / 3600.0f;
        if (mode == 3) hue = hue < 0.5f ? 0.5f + hue : 1.5f - hue;
        return withAlpha(Color.HSBtoRGB(hue, 0.8f, 1.0f), alpha);
    }

    private static int dynamicColor(long time) {
        return dynamicColor(time, 255);
    }

    private static int dynamicColor(long time, int alpha) {
        Color base = new Color(HUD.getHudColor(0));
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        float brightness = 0.45f + 0.55f * (float) ((Math.sin(time / 600.0) + 1.0) / 2.0);
        return withAlpha(Color.HSBtoRGB(hsb[0], hsb[1], brightness), alpha);
    }

    private static int withAlpha(int rgb, int alpha) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    private static boolean enabled(starshack.module.setting.impl.ButtonSetting setting) {
        return setting == null || setting.isToggled();
    }

    private static boolean vanilla() {
        return HUD.starsFont != null && (int) HUD.starsFont.getInput() == 1;
    }

    private static StarsFontRenderer font() {
        return HUD.getHudFontRenderer();
    }

    private static float scale() {
        return HUD.starsScale == null ? 1.0f : (float) HUD.starsScale.getInput();
    }

    private static int width(String text) {
        return vanilla() ? MC.fontRendererObj.getStringWidth(text) : font().getStringWidth(text);
    }

    private static int height() {
        return vanilla() ? MC.fontRendererObj.FONT_HEIGHT : font().getLineHeight();
    }

    private static void draw(String text, float x, float y, int color, boolean shadow) {
        if (vanilla()) {
            if (shadow) MC.fontRendererObj.drawStringWithShadow(text, x, y, color);
            else MC.fontRendererObj.drawString(text, x, y, color, false);
        } else font().drawString(text, x, y, color, shadow);
    }

    private static String value(starshack.module.setting.impl.TextSetting setting, String fallback) {
        return setting == null || setting.getText().trim().isEmpty() ? fallback : setting.getText();
    }
}
