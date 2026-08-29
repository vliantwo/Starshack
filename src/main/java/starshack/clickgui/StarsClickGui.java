package starshack.clickgui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import starshack.Stars;
import starshack.module.Module;
import starshack.module.setting.Setting;
import starshack.module.setting.impl.*;
import starshack.novoline.font.NovolineFonts;
import starshack.utility.RenderUtils;
import starshack.utility.Utils;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public final class StarsClickGui extends ClickGui {

    private static final int BG_OVERLAY = 0x78000000;
    private static final int NAV = 0xFF14161E;
    private static final int CHANNELS = 0xFF1C2030;
    private static final int CONTENT = 0xFF1C2030;
    private static final int HEADER = 0xFF111214;
    private static final int ROW_HOVER = 0xFF2A3045;
    private static final int ACCENT = 0xFF0099FF;
    private static final int ACCENT_CYAN = 0xFF00E5FF;
    private static final int TEXT = 0xFFE8ECF2;
    private static final int TEXT_DIM = 0xFF8A90A0;
    private static final int MUTED = 0xFF6B7280;
    private static final int CONTROL_BG = 0x64000000;
    private static final int CHECK = ACCENT_CYAN;
    private static final float RADIUS_WINDOW = 6.0F;

    private static final int SELECTED_BG = 0x00000000;
    private static final int BORDER_HIGHLIGHT = 0x14FFFFFF;

    private static final boolean USE_IMAGE_LOGO = true;
    private static final String LOGO_TEXT = "S";
    private static final ResourceLocation LOGO_TEXTURE =
            new ResourceLocation("starshack", "textures/gui/logo.png");

    private static final int NAV_W = 45, MIN_SETTINGS = 190, MIN_HEIGHT = 300, HEADER_H = 21;
    private static final int ROW_HEIGHT = 24;
    private static final int ROW_PADDING = 4;
    private static final float ROW_RADIUS = 4.0F;
    private static final float SWITCH_W = 32.0F;
    private static final float SWITCH_H = 16.0F;
    private static final float SWITCH_PAD = 2.0F;
    private static final int SEARCH_BAR_H = 20;

    private float modulePanelWidth = 115F;
    private float targetModulePanelWidth = 115F;

    private float categoryFadeAlpha = 1F;
    private float categoryOffsetY = 0F;

    private final Map<Module, Float> moduleHoverProgress = new IdentityHashMap<>();
    private final Map<ButtonSetting, Float> switchAnimProgress = new IdentityHashMap<>();

    private final Map<Module.category, Float> moduleScroll = new EnumMap<>(Module.category.class);
    private final Map<Module, Float> settingScroll = new IdentityHashMap<>();
    private final Map<ColorSetting, ColorMode> colorModes = new IdentityHashMap<>();

    private Module.category selectedCategory = Module.category.combat;
    private Module selectedModule;
    private Module bindingModule;
    private Setting activeSetting;
    private SliderSetting openCombo;

    private String searchQuery = "";
    private List<Module> filteredModules = new ArrayList<>();
    private boolean searching = false;

    private Module tooltipModule = null;
    private float tooltipAlpha = 0F;

    private int keyboardIndex = 0;

    private boolean dragging, resizing;
    private int dragOffsetX, dragOffsetY, resizeOffsetX, resizeOffsetY;
    private int x = 100, y = 100, settingsWidth = MIN_SETTINGS, windowHeight = MIN_HEIGHT;
    private boolean positioned;

    public StarsClickGui() {
        super();
        for (Module.category c : Module.category.values()) moduleScroll.put(c, 0.0F);
    }

    private int accent() {
        return ACCENT;
    }

    private static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * clamp(t, 0F, 1F);
    }

    private static int blendColor(int color, float factor) {
        factor = clamp(factor, 0F, 1F);
        int a = (color >> 24) & 0xFF, r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
        r = (int) (r + (0xE8 - r) * factor);
        g = (int) (g + (0xEC - g) * factor);
        b = (int) (b + (0xF2 - b) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static String title(String n) {
        if (n == null || n.isEmpty()) return "";
        return n.substring(0, 1).toUpperCase(Locale.ROOT) + n.substring(1).toLowerCase(Locale.ROOT);
    }

    // ★ 返回分类的完整名称（无前缀字母），用于 Header 标题
    private static String categoryName(Module.category c) {
        return c == null ? "" : title(c.name());
    }

    private static String tabIcon(Module.category c) {
        if (c == null) return "?";
        switch (c) {
            case combat:
                return "D";
            case movement:
                return "A";
            case player:
                return "B";
            case visuals:
                return "C";
            case misc:
                return "F";
            case exploits:
                return "G";
            case configs:
                return "H";
            case scripts:
                return "E";
            default:
                return "?";
        }
    }

    private List<Module> getDisplayModules() {
        List<Module> all = modules(selectedCategory);
        if (searchQuery.isEmpty()) {
            filteredModules = new ArrayList<>(all);
            return filteredModules;
        }
        String q = searchQuery.toLowerCase(Locale.ROOT);
        filteredModules = all.stream()
                .filter(m -> m.getName().toLowerCase(Locale.ROOT).contains(q))
                .collect(Collectors.toList());
        return filteredModules;
    }

    private void updateModulePanelWidth(List<Module> mods) {
        targetModulePanelWidth = 90F;
        for (Module m : mods) {
            float w = NovolineFonts.thin(20).stringWidth(m.getName()) + 50F;
            if (w > targetModulePanelWidth) targetModulePanelWidth = w;
        }
        targetModulePanelWidth = Math.min(190F, Math.max(115F, targetModulePanelWidth));
        modulePanelWidth += (targetModulePanelWidth - modulePanelWidth) * 0.2F;
    }

    private int modulePanelW() {
        return (int) modulePanelWidth;
    }

    private static String keyName(int k) {
        if (k == 0) return "NONE";
        if (k == 1069) return "MScrollUp";
        if (k == 1070) return "MScrollDown";
        if (k >= 1000) return "M" + (k - 1000);
        String n = Keyboard.getKeyName(k);
        return n == null ? "NONE" : n;
    }

    private void drawTabs(int mx, int my) {
        Module.category[] cats = Module.category.values();
        float sp = cats.length <= 1 ? 35.0F : Math.min(35.0F, (windowHeight - 82.0F) / (cats.length - 1));
        for (int i = 0; i < cats.length; i++) {
            Module.category cat = cats[i];
            float cy = y + 63.0F + i * sp;
            boolean sel = cat == selectedCategory;
            boolean hov = inside(mx, my, x + 7, cy - 15, 30, 30);

            if (sel) {
                drawCircle(x + 22, cy, 15.0F, ACCENT);
                NovolineFonts.icons(35).drawCenteredString(tabIcon(cat), x + 22, cy - 6, TEXT);
                Gui.drawRect(x + NAV_W - 3, (int) (cy - 10), x + NAV_W, (int) (cy + 10), ACCENT_CYAN);
            } else {
                drawCircle(x + 22, cy, 15.0F, hov ? ROW_HOVER : CONTENT);
                NovolineFonts.icons(35).drawCenteredString(tabIcon(cat), x + 22, cy - 6, hov ? TEXT : TEXT_DIM);
            }

            if (hov && !sel) {
                String name = title(cat.name());
                float tw = NovolineFonts.thin(15).stringWidth(name) + 12;
                float tx = x + NAV_W + 4, ty = cy - 8;
                RenderUtils.drawRoundedRectangle(tx, ty, tx + tw, ty + 14, 4.0F, CONTENT);
                NovolineFonts.thin(15).drawString(name, tx + 6, ty + 3, TEXT, false);
            }
        }
    }

    private void drawModules(int mx, int my) {
        List<Module> mods = getDisplayModules();
        updateModulePanelWidth(mods);

        float scroll = clampModuleScroll(mods, moduleScroll.get(selectedCategory));
        moduleScroll.put(selectedCategory, scroll);

        GlStateManager.pushAttrib();
        GlStateManager.color(1F, 1F, 1F, categoryFadeAlpha);

        float searchY = y + HEADER_H + 4.0F;
        float searchLeft = x + NAV_W + ROW_PADDING;
        float searchRight = x + NAV_W + modulePanelW() - ROW_PADDING;
        RenderUtils.drawRoundedRectangle(searchLeft, searchY, searchRight, searchY + SEARCH_BAR_H - 4, 4.0F, CHANNELS);
        String searchText = searchQuery.isEmpty() ? "Search..." : searchQuery;
        int searchColor = searchQuery.isEmpty() ? MUTED : TEXT;
        NovolineFonts.thin(16).drawString(searchText, searchLeft + 6, searchY + 4, searchColor, false);
        if (searching && caretVisible()) {
            float cx = searchLeft + 6 + NovolineFonts.thin(16).stringWidth(searchText);
            Gui.drawRect((int) cx, (int) (searchY + 3), (int) cx + 1, (int) (searchY + SEARCH_BAR_H - 7), TEXT);
        }

        scissor(x + NAV_W, y + HEADER_H + SEARCH_BAR_H, modulePanelW(), windowHeight - HEADER_H - SEARCH_BAR_H);
        float rowY = searchY + SEARCH_BAR_H + scroll + categoryOffsetY;

        tooltipModule = null;
        for (int i = 0; i < mods.size(); i++) {
            Module m = mods.get(i);
            float rowTop = rowY;
            float rowBottom = rowY + ROW_HEIGHT - 2;
            boolean hov = inside(mx, my, x + NAV_W + ROW_PADDING, rowTop, modulePanelW() - ROW_PADDING * 2, ROW_HEIGHT - 2);

            float target = hov ? 1F : 0F;
            float cur = lerp(moduleHoverProgress.getOrDefault(m, 0F), target, 0.25F);
            moduleHoverProgress.put(m, cur);

            if (m == selectedModule) {
                Gui.drawRect(x + NAV_W + ROW_PADDING, (int) rowTop, x + NAV_W + ROW_PADDING + 3, (int) rowBottom, ACCENT_CYAN);
                NovolineFonts.thin(20).drawString(
                        NovolineFonts.thin(20).trimStringToWidth(m.getName(), modulePanelW() - 42, false),
                        x + NAV_W + 20, rowTop + 5, ACCENT_CYAN, false);
            } else {
                if (cur > 0.01F) {
                    int bg = blendColor(ROW_HOVER, cur);
                    RenderUtils.drawRoundedRectangle(x + NAV_W + ROW_PADDING, rowTop, x + NAV_W + modulePanelW() - ROW_PADDING, rowBottom, ROW_RADIUS, bg);
                }
                float textX = x + NAV_W + 14;
                if (m.isEnabled()) {
                    drawCircle(textX - 2, rowTop + (ROW_HEIGHT - 2) / 2.0F, 3.0F, ACCENT_CYAN);
                }
                String name = bindingModule == m ? "Press a key..." : m.getName();
                int nameColor = m.isEnabled() ? TEXT : TEXT_DIM;
                NovolineFonts.thin(20).drawString(
                        NovolineFonts.thin(20).trimStringToWidth(name, modulePanelW() - 42, false),
                        textX + 6, rowTop + 5, nameColor, false);
            }

            if (m != bindingModule && m.getKeycode() != 0) {
                String key = keyName(m.getKeycode());
                NovolineFonts.thin(13).drawString(key,
                        x + NAV_W + modulePanelW() - ROW_PADDING - NovolineFonts.thin(13).stringWidth(key) - 6,
                        rowTop + 8, MUTED, false);
            }

            if (hov && tooltipAlpha > 0.5F) {
                tooltipModule = m;
            }

            rowY += ROW_HEIGHT;
        }
        endScissor();
        GlStateManager.popAttrib();

        drawTooltip(mx, my);

        if (selectedModule != null)
            NovolineFonts.thin(20).drawString(selectedModule.getName() + " Settings", x + NAV_W + modulePanelW() + 15, y + 7, TEXT, false);
    }

    private void drawTooltip(int mx, int my) {
        tooltipAlpha = lerp(tooltipAlpha, tooltipModule != null ? 1F : 0F, 0.15F);
        if (tooltipModule == null || tooltipAlpha < 0.05F) return;

        String desc = tooltipModule.getName();
        float pad = 6.0F;
        float tw = NovolineFonts.thin(14).stringWidth(desc) + pad * 2;
        float th = 16.0F + pad * 2;
        float tx = mx + 14;
        float ty = my + 14;

        if (tx + tw > width) tx = mx - tw - 8;
        if (ty + th > height) ty = my - th - 8;

        GlStateManager.pushMatrix();
        GlStateManager.color(1F, 1F, 1F, tooltipAlpha);

        RenderUtils.drawRoundedRectangle(tx, ty, tx + tw, ty + th, 4.0F, 0xE614161E);
        drawRoundedOutline(tx, ty, tx + tw, ty + th, 4.0F, ACCENT);
        NovolineFonts.thin(14).drawString(desc, tx + pad, ty + pad, TEXT, false);

        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private void drawSettings(int mx, int my) {
        if (selectedModule == null) return;
        List<Setting> settings = visibleSettings(selectedModule);
        if (settings.isEmpty()) {
            String t = "NO SETTINGS ;(";
            NovolineFonts.thin(18).drawCenteredString(t,
                    x + NAV_W + modulePanelW() + settingsWidth / 2.0F, y + windowHeight / 2.0F, MUTED);
            return;
        }
        float scroll = settingScroll.getOrDefault(selectedModule, 0.0F);
        scroll = clampSettingScroll(settings, scroll);
        settingScroll.put(selectedModule, scroll);

        GlStateManager.pushAttrib();
        GlStateManager.color(1F, 1F, 1F, categoryFadeAlpha);

        float sy = y + HEADER_H + 6.0F + scroll + categoryOffsetY;
        scissor(x + NAV_W + modulePanelW(), y + HEADER_H, settingsWidth, windowHeight - HEADER_H);

        float comboDrawY = -1;
        for (Setting s : settings) {
            if (s == openCombo) comboDrawY = sy;
            drawSetting(s, sy, mx, my);
            sy += settingHeight(s);
        }
        endScissor();

        if (openCombo != null && settings.contains(openCombo) && comboDrawY != -1) {
            drawComboOptions(openCombo, comboDrawY, mx, my);
        }

        GlStateManager.popAttrib();
    }

    private void drawSetting(Setting setting, float sy, int mx, int my) {
        float left = x + NAV_W + modulePanelW() + 14.0F;
        float right = x + NAV_W + modulePanelW() + settingsWidth - 14.0F;
        float ctrlW = 70.0F;
        float cLeft = right - ctrlW;

        if (!(setting instanceof DescriptionSetting)) {
            Gui.drawRect((int) left, (int) sy - 1, (int) right, (int) sy, 0x10FFFFFF);
        }

        NovolineFonts.thin(17).drawString(
                setting instanceof DescriptionSetting ? ((DescriptionSetting) setting).getDesc() : setting.getName(),
                left, sy + 2, setting instanceof DescriptionSetting ? TEXT_DIM : TEXT, false);
        if (setting instanceof DescriptionSetting) return;

        if (setting instanceof SliderSetting) {
            SliderSetting s = (SliderSetting) setting;
            if (s.isString) {
                RenderUtils.drawRoundedRectangle(cLeft, sy, right, sy + 12, 3.0F, CHANNELS);
                drawRoundedOutline(cLeft, sy, right, sy + 12, 3.0F, inside(mx, my, cLeft, sy, ctrlW, 12) ? accent() : CONTROL_BG);
                String[] o = s.getOptions();
                int idx = (int) clamp((float) s.getInput(), 0, o.length - 1);
                NovolineFonts.thin(15).drawCenteredString(o[idx], cLeft + ctrlW / 2.0F, sy + 2, TEXT);
            } else {
                double range = Math.max(0.00001D, s.getMax() - s.getMin());
                float pct = (float) ((s.getInput() - s.getMin()) / range);
                float trackY = sy + 5;
                RenderUtils.drawRoundedRectangle(cLeft, trackY, right, trackY + 4, 2.0F, CONTROL_BG);
                RenderUtils.drawRoundedRectangle(cLeft, trackY, cLeft + ctrlW * pct, trackY + 4, 2.0F, accent());
                drawCircle(cLeft + ctrlW * pct, trackY + 2, 2, TEXT);
                NovolineFonts.thin(12).drawCenteredString(Utils.asWholeNum(s.getInput()) + s.getSuffix(), cLeft + ctrlW * pct, sy - 5, TEXT);
                if (activeSetting == s && Mouse.isButtonDown(0)) updateSlider(s, mx, cLeft);
            }
            return;
        }

        if (setting instanceof ButtonSetting) {
            ButtonSetting b = (ButtonSetting) setting;
            if (b.isMethodButton) {
                drawRoundedOutline(cLeft, sy - 1, right, sy + 11, 3.0F, inside(mx, my, cLeft, sy - 1, ctrlW, 12) ? accent() : CONTROL_BG);
                NovolineFonts.thin(15).drawCenteredString("+", cLeft + ctrlW / 2.0F, sy + 1, accent());
            } else {
                float target = b.isToggled() ? 1F : 0F;
                float cur = lerp(switchAnimProgress.getOrDefault(b, target), target, 0.25F);
                switchAnimProgress.put(b, cur);

                float sx = cLeft + (ctrlW - SWITCH_W) / 2.0F;
                float syy = sy + 1;
                int trackColor = cur > 0.5F ? ACCENT : CONTROL_BG;
                RenderUtils.drawRoundedRectangle(sx, syy, sx + SWITCH_W, syy + SWITCH_H, SWITCH_H / 2.0F, trackColor);
                float knobX = sx + SWITCH_PAD + cur * (SWITCH_W - SWITCH_H);
                drawCircle(knobX + (SWITCH_H - SWITCH_PAD * 2) / 2.0F, syy + SWITCH_H / 2.0F, (SWITCH_H - SWITCH_PAD * 2) / 2.0F, TEXT);
            }
            return;
        }

        if (setting instanceof TextSetting) {
            TextSetting t = (TextSetting) setting;
            drawRoundedOutline(cLeft, sy - 1, right, sy + 11, 3.0F, activeSetting == t ? accent() : CONTROL_BG);
            String v = t.getText().isEmpty() ? t.getPlaceholder() : t.getText();
            v = NovolineFonts.thin(16).trimStringToWidth(v, 64, true);
            NovolineFonts.thin(16).drawString(v + (activeSetting == t && caretVisible() ? "|" : ""), cLeft + 3, sy + 1, t.getText().isEmpty() ? MUTED : TEXT, false);
            return;
        }

        if (setting instanceof KeySetting) {
            drawRoundedOutline(cLeft, sy - 1, right, sy + 11, 3.0F, activeSetting == setting ? accent() : CONTROL_BG);
            NovolineFonts.thin(15).drawCenteredString(activeSetting == setting ? "Press a key" : keyName(((KeySetting) setting).getKey()), cLeft + ctrlW / 2.0F, sy + 1, TEXT);
            return;
        }

        if (setting instanceof ColorSetting) {
            ColorSetting c = (ColorSetting) setting;
            ColorMode mode = colorModes.getOrDefault(c, ColorMode.HUE);
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            for (int i = 0; i < 70; i++) {
                float v = i / 69.0F;
                int rgb = mode == ColorMode.HUE ? Color.HSBtoRGB(v, hsb[1], hsb[2])
                        : mode == ColorMode.SATURATION ? Color.HSBtoRGB(hsb[0], v, hsb[2])
                        : Color.HSBtoRGB(hsb[0], hsb[1], v);
                Gui.drawRect((int) cLeft + i, (int) sy - 1, (int) cLeft + i + 1, (int) sy + 11, rgb);
            }
            float marker = mode == ColorMode.HUE ? hsb[0] : mode == ColorMode.SATURATION ? hsb[1] : hsb[2];
            Gui.drawRect((int) (cLeft + marker * 69), (int) sy - 1, (int) (cLeft + marker * 69) + 1, (int) sy + 11, TEXT);
            if (activeSetting == c && Mouse.isButtonDown(0)) updateColor(c, mode, mx, cLeft);
            return;
        }

        if (setting instanceof GroupSetting) {
            GroupSetting g = (GroupSetting) setting;
            NovolineFonts.thin(16).drawString(g.isOpened() ? "-" : "+", right - 8, sy + 2, g.isOpened() ? accent() : TEXT, false);
        }
    }

    private void drawComboOptions(SliderSetting s, float sy, int mx, int my) {
        String[] o = s.getOptions();
        float right = x + NAV_W + modulePanelW() + settingsWidth - 10.0F, left = right - 70.0F, bottom = sy + 10 + o.length * 13;
        scissor(x + NAV_W + modulePanelW(), y + HEADER_H, settingsWidth, windowHeight - HEADER_H);
        RenderUtils.drawRoundedRectangle(left, sy + 10, right, bottom, 3.0F, CHANNELS);
        drawRoundedOutline(left, sy + 10, right, bottom, 3.0F, CONTROL_BG);
        for (int i = 0; i < o.length; i++) {
            boolean hov = inside(mx, my, left, sy + 10 + i * 13, 70, 13);
            if (hov)
                Gui.drawRect((int) left + 1, (int) (sy + 10 + i * 13), (int) right - 1, (int) (sy + 11 + i * 13), ROW_HOVER);
            NovolineFonts.thin(15).drawCenteredString(o[i], left + 35, sy + 12 + i * 13, (int) s.getInput() == i || hov ? accent() : TEXT);
        }
        endScissor();
    }

    @Override
    public void mouseClicked(int mx, int my, int button) throws IOException {
        int right = x + totalWidth();
        if (button == 0 && inside(mx, my, x, y - 10, totalWidth(), 30)) {
            dragging = true;
            dragOffsetX = x - mx;
            dragOffsetY = y - my;
            return;
        }
        if (button == 0 && inside(mx, my, right - 7, y + windowHeight - 7, 7, 7)) {
            resizing = true;
            resizeOffsetX = settingsWidth - mx;
            resizeOffsetY = windowHeight - my;
            return;
        }
        Module.category tab = tabAt(mx, my);
        if (tab != null && button == 0) {
            if (tab != selectedCategory) {
                categoryFadeAlpha = 0F;
                categoryOffsetY = 8F;
            }
            selectedCategory = tab;
            selectedModule = null;
            activeSetting = null;
            openCombo = null;
            return;
        }

        float searchY = y + HEADER_H + 4.0F;
        float searchLeft = x + NAV_W + ROW_PADDING;
        float searchRight = x + NAV_W + modulePanelW() - ROW_PADDING;
        if (button == 0 && inside(mx, my, searchLeft, searchY, searchRight - searchLeft, SEARCH_BAR_H)) {
            searching = true;
            return;
        } else {
            searching = false;
        }

        Module mod = moduleAt(mx, my);
        if (mod != null) {
            if (button == 0 && mod.canBeEnabled()) mod.toggle();
            else if (button == 1) {
                selectedModule = selectedModule == mod ? null : mod;
                activeSetting = null;
                openCombo = null;
            } else if (button == 2) bindingModule = mod;
            List<Module> disp = getDisplayModules();
            keyboardIndex = disp.indexOf(mod);
            return;
        }
        if (selectedModule != null) clickSettingAt(mx, my, button);
    }

    private void clickSettingAt(int mx, int my, int button) {
        List<Setting> settings = visibleSettings(selectedModule);
        float scroll = settingScroll.getOrDefault(selectedModule, 0.0F);
        if (openCombo != null) {
            float comboY = settingScreenY(settings, openCombo, scroll);
            float right = x + totalWidth() - 10.0F, left = right - 70.0F;
            String[] o = openCombo.getOptions();
            for (int i = 0; i < o.length; i++) {
                if (inside(mx, my, left, comboY + 10 + i * 13, 70, 13)) {
                    openCombo.setValueWithEvent(i);
                    openCombo = null;
                    return;
                }
            }
            openCombo = null;
        }
        float sy = y + HEADER_H + 6.0F + scroll;
        for (Setting s : settings) {
            if (inside(mx, my, x + NAV_W + modulePanelW(), sy - 3, settingsWidth, settingHeight(s))) {
                handleSettingClick(s, mx, sy, button);
                return;
            }
            sy += settingHeight(s);
        }
        if (button == 0) activeSetting = null;
    }

    private void handleSettingClick(Setting setting, int mx, float sy, int button) {
        if (openCombo != null && setting != openCombo) {
            openCombo = null;
        }

        float cLeft = x + totalWidth() - 80.0F;
        if (setting instanceof SliderSetting) {
            SliderSetting s = (SliderSetting) setting;
            if (s.isString) {
                if (button == 0) openCombo = openCombo == s ? null : s;
            } else if (button == 0) {
                activeSetting = s;
                updateSlider(s, mx, cLeft);
            }
        } else if (setting instanceof ButtonSetting && button == 0) {
            ButtonSetting b = (ButtonSetting) setting;
            if (b.isMethodButton) b.runMethod();
            else b.toggle();
        } else if ((setting instanceof TextSetting || setting instanceof KeySetting) && button == 0)
            activeSetting = setting;
        else if (setting instanceof ColorSetting) {
            ColorSetting c = (ColorSetting) setting;
            if (button == 1)
                colorModes.put(c, ColorMode.values()[(colorModes.getOrDefault(c, ColorMode.HUE).ordinal() + 1) % 3]);
            else if (button == 0) {
                activeSetting = c;
                updateColor(c, colorModes.getOrDefault(c, ColorMode.HUE), mx, cLeft);
            }
        } else if (setting instanceof GroupSetting && button == 0)
            ((GroupSetting) setting).setOpened(!((GroupSetting) setting).isOpened());
    }

    private void drawWindow(int mx, int my) {
        int right = x + totalWidth();

        RenderUtils.drawRoundedRectangle(x, y, right, y + windowHeight, RADIUS_WINDOW, CONTENT);
        RenderUtils.drawRoundedRectangle(x, y, x + NAV_W, y + windowHeight, RADIUS_WINDOW, NAV);
        Gui.drawRect(x + NAV_W, y, x + NAV_W + modulePanelW(), y + windowHeight, CHANNELS);
        Gui.drawRect(x + NAV_W, y, right, y + HEADER_H, HEADER);
        RenderUtils.drawRoundedRectangle(x, y, right, y + 1, 1, BORDER_HIGHLIGHT);

        RenderUtils.drawRoundedRectangle(x + 7, y + 5, x + 37, y + 35, 15.0F, 0xFF222838);
        if (USE_IMAGE_LOGO) {
            GlStateManager.color(1F, 1F, 1F, 1F);
            GlStateManager.enableBlend();
            mc.getTextureManager().bindTexture(LOGO_TEXTURE);
            drawModalRectWithCustomSizedTexture(x + 10, y + 8, 0, 0, 24, 24, 24, 24);
        } else {
            NovolineFonts.thin(35).drawCenteredString(LOGO_TEXT, x + 22, y + 11, ACCENT_CYAN);
        }

        // ★ Header 标题：去掉前缀字母（D Combat → Combat），直接显示分类全名
        NovolineFonts.thin(20).drawString(categoryName(selectedCategory), x + 50, y + 7, TEXT, false);
        int modCount = getDisplayModules().size();
        String countStr = modCount + " modules";
        NovolineFonts.thin(14).drawString(countStr,
                x + 50 + NovolineFonts.thin(20).stringWidth(categoryName(selectedCategory)) + 10, y + 10, TEXT_DIM, false);

        if (selectedModule != null) {
            NovolineFonts.thin(20).drawString(selectedModule.getName() + " Settings", x + NAV_W + modulePanelW() + 15, y + 7, TEXT, false);
        } else {
            NovolineFonts.thin(16).drawCenteredString("S T A R S H A C K", x + NAV_W + modulePanelW() + settingsWidth / 2.0F, y + 7, TEXT_DIM);
        }

        if (inside(mx, my, right - 10, y + windowHeight - 10, 10, 10)) {
            Gui.drawRect(right - 6, y + windowHeight - 2, right, y + windowHeight, accent());
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth, my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        float dir = wheel > 0 ? 12.0F : -12.0F;
        if (inside(mx, my, x + NAV_W, y + HEADER_H, modulePanelW(), windowHeight - HEADER_H))
            moduleScroll.put(selectedCategory, moduleScroll.get(selectedCategory) + dir);
        else if (selectedModule != null && inside(mx, my, x + NAV_W + modulePanelW(), y + HEADER_H, settingsWidth, windowHeight - HEADER_H))
            settingScroll.put(selectedModule, settingScroll.getOrDefault(selectedModule, 0.0F) + dir);
    }

    @Override
    public void keyTyped(char ch, int key) {
        if (searching) {
            if (key == Keyboard.KEY_ESCAPE) {
                searching = false;
                searchQuery = "";
                return;
            }
            if (key == Keyboard.KEY_RETURN) {
                searching = false;
                return;
            }
            if (key == Keyboard.KEY_BACK && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                return;
            }
            if (Character.isDefined(ch) && !Character.isISOControl(ch)) {
                searchQuery += ch;
                return;
            }
            return;
        }

        if (bindingModule != null) {
            bindingModule.setBind(key == Keyboard.KEY_ESCAPE ? 0 : key);
            bindingModule = null;
            return;
        }
        if (activeSetting instanceof KeySetting) {
            ((KeySetting) activeSetting).setKey(key == Keyboard.KEY_ESCAPE ? 0 : key);
            activeSetting = null;
            return;
        }
        if (activeSetting instanceof TextSetting) {
            TextSetting t = (TextSetting) activeSetting;
            if (key == Keyboard.KEY_ESCAPE) activeSetting = null;
            else if (key == Keyboard.KEY_RETURN) {
                t.submit();
                activeSetting = null;
            } else if (key == Keyboard.KEY_BACK && !t.getText().isEmpty())
                t.setText(t.getText().substring(0, t.getText().length() - 1));
            else if (Character.isDefined(ch) && !Character.isISOControl(ch)) t.setText(t.getText() + ch);
            return;
        }
        if (key == Keyboard.KEY_ESCAPE && openCombo != null) {
            openCombo = null;
            return;
        }

        List<Module> disp = getDisplayModules();
        if (disp.isEmpty()) {
            super.keyTyped(ch, key);
            return;
        }
        if (key == Keyboard.KEY_UP) {
            keyboardIndex = Math.max(0, keyboardIndex - 1);
            ensureKeyboardIndexVisible();
            return;
        }
        if (key == Keyboard.KEY_DOWN) {
            keyboardIndex = Math.min(disp.size() - 1, keyboardIndex + 1);
            ensureKeyboardIndexVisible();
            return;
        }
        if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_RIGHT) {
            if (keyboardIndex >= 0 && keyboardIndex < disp.size()) {
                Module m = disp.get(keyboardIndex);
                if (m.canBeEnabled()) m.toggle();
                selectedModule = m;
            }
            return;
        }

        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(null);
        else super.keyTyped(ch, key);
    }

    @Override
    public void mouseReleased(int mx, int my, int state) {
        if (state == 0) {
            dragging = false;
            resizing = false;
            if (activeSetting instanceof SliderSetting || activeSetting instanceof ColorSetting) activeSetting = null;
        }
    }

    @Override
    public void onGuiClosed() {
        dragging = false;
        resizing = false;
        bindingModule = null;
        activeSetting = null;
        openCombo = null;
        super.onGuiClosed();
    }

    @Override
    public void initGui() {
        super.initGui();
        if (!positioned) {
            this.x = Math.max(8, (this.width - totalWidth()) / 2);
            this.y = Math.max(14, (this.height - windowHeight) / 2);
            positioned = true;
        }
        constrainWindow();
        if (selectedModule != null && !getDisplayModules().contains(selectedModule)) selectedModule = null;
    }

    private void ensureKeyboardIndexVisible() {
        float visibleH = windowHeight - HEADER_H - SEARCH_BAR_H;
        float targetY = keyboardIndex * ROW_HEIGHT;
        float scroll = moduleScroll.get(selectedCategory);
        if (targetY < -scroll) {
            moduleScroll.put(selectedCategory, -targetY);
        } else if (targetY + ROW_HEIGHT > -scroll + visibleH) {
            moduleScroll.put(selectedCategory, -(targetY + ROW_HEIGHT - visibleH));
        }
    }

    private void drawBlurBackground() {
        if (!OpenGlHelper.isFramebufferEnabled()) {
            Gui.drawRect(0, 0, width, height, BG_OVERLAY);
            return;
        }
        mc.getFramebuffer().bindFramebufferTexture();
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        int w = width, h = height;
        float[] offsets = {-3.0F, -1.5F, 0.0F, 1.5F, 3.0F};
        for (float ox : offsets) {
            for (float oy : offsets) {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 0.04F);
                drawTexturedFullscreenQuad(ox, oy, w, h);
            }
        }
        GlStateManager.color(0.0F, 0.0F, 0.0F, 0.50F);
        drawTexturedFullscreenQuad(0, 0, w, h);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.disableBlend();
        GlStateManager.enableDepth();
        GlStateManager.popMatrix();
        GlStateManager.bindTexture(0);
    }

    private void drawTexturedFullscreenQuad(float offsetX, float offsetY, int w, int h) {
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0.0F, 1.0F);
        GL11.glVertex2f(offsetX, offsetY);
        GL11.glTexCoord2f(1.0F, 1.0F);
        GL11.glVertex2f(w + offsetX, offsetY);
        GL11.glTexCoord2f(1.0F, 0.0F);
        GL11.glVertex2f(w + offsetX, h + offsetY);
        GL11.glTexCoord2f(0.0F, 0.0F);
        GL11.glVertex2f(offsetX, h + offsetY);
        GL11.glEnd();
    }

    private void drawRoundedOutline(float l, float t, float r, float b, float radius, int color) {
        Gui.drawRect((int) l, (int) t, (int) r, (int) t + 1, color);
        Gui.drawRect((int) l, (int) (b - 1), (int) r, (int) b, color);
        Gui.drawRect((int) l, (int) t, (int) l + 1, (int) b, color);
        Gui.drawRect((int) (r - 1), (int) t, (int) r, (int) b, color);
    }

    private void drawCircle(float cx, float cy, float radius, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Color c = new Color(color, false);
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 36; i++) {
            double a = Math.PI * 2 * i / 36;
            GL11.glVertex2d(cx + Math.sin(a) * radius, cy + Math.cos(a) * radius);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        mc.getFramebuffer().bindFramebuffer(false);
        drawBlurBackground();
        ScaledResolution res = new ScaledResolution(this.mc);
        double scale = width <= 0 ? 1.0D : res.getScaledWidth() / (double) width;
        int lx = (int) Math.floor(mouseX / scale), ly = (int) Math.floor(mouseY / scale);
        updateWindowDrag(lx, ly);

        if (categoryFadeAlpha < 1F) {
            categoryFadeAlpha = Math.min(1F, categoryFadeAlpha + 0.08F);
            categoryOffsetY *= 0.85F;
            if (Math.abs(categoryOffsetY) < 0.5F) categoryOffsetY = 0F;
        }

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0D);
        drawWindow(lx, ly);
        drawTabs(lx, ly);
        drawModules(lx, ly);
        drawSettings(lx, ly);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void scissor(float sx, float sy, float sw, float sh) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(sx, sy, sw, sh);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private int totalWidth() {
        return NAV_W + modulePanelW() + settingsWidth;
    }

    private boolean caretVisible() {
        return System.currentTimeMillis() / 500L % 2L == 0L;
    }

    private void updateWindowDrag(int mx, int my) {
        if (dragging) {
            x = dragOffsetX + mx;
            y = dragOffsetY + my;
            constrainWindow();
        }
        if (resizing) {
            settingsWidth = Math.max(MIN_SETTINGS, Math.min(resizeOffsetX + mx, Math.max(MIN_SETTINGS, width - x - NAV_W - modulePanelW())));
            windowHeight = Math.max(MIN_HEIGHT, Math.min(resizeOffsetY + my, Math.max(MIN_HEIGHT, height - y)));
        }
    }

    private void constrainWindow() {
        x = Math.max(0, Math.min(x, Math.max(0, width - totalWidth())));
        y = Math.max(10, Math.min(y, Math.max(10, height - windowHeight)));
    }
    private Module.category tabAt(int mx, int my) {
        Module.category[] cats = Module.category.values();
        float sp = cats.length <= 1 ? 35.0F : Math.min(35.0F, (windowHeight - 82.0F) / (cats.length - 1));
        for (int i = 0; i < cats.length; i++) if (inside(mx, my, x + 7, y + 63 + i * sp - 15, 30, 30)) return cats[i];
        return null;
    }
    private Module moduleAt(int mx, int my) {
        if (!inside(mx, my, x + NAV_W, y + HEADER_H + SEARCH_BAR_H, modulePanelW(), windowHeight - HEADER_H - SEARCH_BAR_H))
            return null;
        float rowY = y + HEADER_H + 4.0F + SEARCH_BAR_H + moduleScroll.get(selectedCategory);
        for (Module m : getDisplayModules()) {
            if (inside(mx, my, x + NAV_W + ROW_PADDING, rowY, modulePanelW() - ROW_PADDING * 2, ROW_HEIGHT - 2))
                return m;
            rowY += ROW_HEIGHT;
        }
        return null;
    }

    private List<Module> modules(Module.category cat) {
        return new ArrayList<>(Stars.getModuleManager().inCategory(cat));
    }

    private List<Setting> visibleSettings(Module mod) {
        List<Setting> r = new ArrayList<>();
        for (Setting s : mod.getSettings()) if (s.visible) r.add(s);
        return r;
    }

    private float clampModuleScroll(List<Module> m, float s) {
        float content = m.size() * (float) ROW_HEIGHT;
        float viewport = windowHeight - HEADER_H - SEARCH_BAR_H - 8.0F;
        return clamp(s, Math.min(0, viewport - content), 0);
    }

    private float clampSettingScroll(List<Setting> s, float sc) {
        float content = 0;
        for (Setting set : s) content += settingHeight(set);
        float viewport = windowHeight - HEADER_H - 8.0F;
        return clamp(sc, Math.min(0, viewport - content), 0);
    }

    private float settingScreenY(List<Setting> settings, Setting target, float scroll) {
        float sy = y + HEADER_H + 6.0F + scroll;
        for (Setting s : settings) {
            if (s == target) return sy;
            sy += settingHeight(s);
        }
        return sy;
    }

    private float settingHeight(Setting s) {
        return s instanceof DescriptionSetting ? 14.0F : 22.0F;
    }

    private void updateSlider(SliderSetting s, int mx, float left) {
        double frac = clamp((mx - left) / 70.0F, 0, 1);
        s.setValueWithEvent(s.getMin() + (s.getMax() - s.getMin()) * frac);
    }

    private void updateColor(ColorSetting c, ColorMode mode, int mx, float left) {
        float v = clamp((mx - left) / 70.0F, 0, 1);
        if (mode == ColorMode.HUE) c.setHue(v * 360.0F);
        else if (mode == ColorMode.SATURATION) c.setSaturation(v);
        else c.setBrightness(v);
    }

    private enum ColorMode {HUE, SATURATION, BRIGHTNESS}
}
