package starshack.clickgui;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
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

public final class StarsClickGui extends ClickGui {

    // ===== Vape 主题色（内联）=====
    private static final int BG_OVERLAY = 0x78000000;
    private static final int NAV = 0xFF14161E;
    private static final int CHANNELS = 0xFF1C2030;
    private static final int CONTENT = 0xFF1C2030;
    private static final int HEADER = 0xFF111214;
    private static final int ROW_HOVER = 0xFF2A3045;
    private static final int ACCENT = 0xFF0099FF;   // ← 改了：加了 0xFF
    private static final int ACCENT_CYAN = 0xFF00E5FF;   // ← 改了：加了 0xFF
    private static final int TEXT = 0xFFE8ECF2;
    private static final int TEXT_DIM = 0xFF8A90A0;
    private static final int MUTED = 0xFF6B7280;
    private static final int CONTROL_BG = 0x64000000;
    private static final int CHECK = ACCENT_CYAN;
    private static final float RADIUS_WINDOW = 6.0F;
    private static final float RADIUS_PANEL = 4.0F;

    private static final int NAV_W = 45, MODULES_W = 105, MIN_SETTINGS = 190, MIN_HEIGHT = 300, HEADER_H = 21;

    private final Map<Module.category, Float> moduleScroll = new EnumMap<>(Module.category.class);
    private final Map<Module, Float> settingScroll = new IdentityHashMap<>();
    private final Map<ColorSetting, ColorMode> colorModes = new IdentityHashMap<>();

    private Module.category selectedCategory = Module.category.combat;
    private Module selectedModule;
    private Module bindingModule;
    private Setting activeSetting;
    private SliderSetting openCombo;

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

    // ===== 工具 =====
    private static boolean inside(float mx, float my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static String title(String n) {
        if (n == null || n.isEmpty()) return "";
        return n.substring(0, 1).toUpperCase(Locale.ROOT) + n.substring(1).toLowerCase(Locale.ROOT);
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

    private void drawWindow(int mx, int my) {
        int right = x + totalWidth();
        RenderUtils.drawRoundedRectangle(x, y - 10, right, y + 5, RADIUS_WINDOW, NAV);
        RenderUtils.drawRoundedRectangle(x, y, x + NAV_W, y + windowHeight, RADIUS_WINDOW, NAV);
        RenderUtils.drawRoundedRectangle(x + NAV_W + MODULES_W, y, right, y + windowHeight, RADIUS_PANEL, CONTENT);
        Gui.drawRect(x + NAV_W, y, x + NAV_W + MODULES_W, y + windowHeight, CHANNELS);
        Gui.drawRect(x + NAV_W, y + 20, right, y + HEADER_H, HEADER);
        RenderUtils.drawRoundedRectangle(x + 7, y + 5, x + 37, y + 35, 15.0F, CONTENT);
        NovolineFonts.icons(35).drawCenteredString("?", x + 22, y + 14, TEXT);
        if (selectedModule == null) {
            fontRendererObj.drawStringWithShadow("<------------", x + 59, y + 65, MUTED);
            fontRendererObj.drawStringWithShadow("Select a module", x + NAV_W + MODULES_W + 18, y + 72, MUTED);
            NovolineFonts.thin(16).drawCenteredString("S T A R S H A C K", x + NAV_W + MODULES_W + settingsWidth / 2.0F, y + 7, TEXT_DIM);
        }
        if (inside(mx, my, right - 7, y + windowHeight - 7, 7, 7)) {
            Gui.drawRect(right - 5, y + windowHeight - 1, right, y + windowHeight, accent());
            Gui.drawRect(right - 1, y + windowHeight - 5, right, y + windowHeight, accent());
        }
    }

    private void drawTabs(int mx, int my) {
        Module.category[] cats = Module.category.values();
        float sp = cats.length <= 1 ? 35.0F : Math.min(35.0F, (windowHeight - 82.0F) / (cats.length - 1));
        for (int i = 0; i < cats.length; i++) {
            Module.category cat = cats[i];
            float cy = y + 63.0F + i * sp;
            boolean sel = cat == selectedCategory, hov = inside(mx, my, x + 7, cy - 15, 30, 30);
            if (sel || hov) Gui.drawRect(x, (int) cy - (sel ? 10 : 5), x + 2, (int) cy + (sel ? 10 : 5), ACCENT_CYAN);
            drawCircle(x + 22, cy, 15.0F, CONTENT);
            NovolineFonts.icons(35).drawCenteredString(tabIcon(cat), x + 22, cy - 6, hov ? accent() : TEXT);
            if (hov) {
                String name = title(cat.name());
                float tx = x - NovolineFonts.thin(16).stringWidth(name) - 12;
                RenderUtils.drawRoundedRectangle(tx, cy - 6, x - 5, cy + 5, 5.0F, CONTENT);
                NovolineFonts.thin(16).drawString(name, tx + 3, cy - 3, TEXT, false);
            }
        }
    }

    private void drawModules(int mx, int my) {
        NovolineFonts.thin(20).drawString(tabIcon(selectedCategory), x + 50, y + 7, TEXT, false);
        NovolineFonts.thin(20).drawString(title(selectedCategory.name()), x + 63, y + 7, TEXT, false);
        List<Module> mods = modules(selectedCategory);
        float scroll = clampModuleScroll(mods, moduleScroll.get(selectedCategory));
        moduleScroll.put(selectedCategory, scroll);
        float rowY = y + 30.0F + scroll;
        scissor(x + NAV_W, y + HEADER_H, MODULES_W, windowHeight - HEADER_H);
        for (Module m : mods) {
            boolean hov = inside(mx, my, x + NAV_W, rowY - 4, MODULES_W, 18);
            if (hov || m == selectedModule)
                Gui.drawRect(x + NAV_W, (int) rowY - 4, x + NAV_W + MODULES_W, (int) rowY + 14, ROW_HOVER);
            NovolineFonts.bold(26).drawString("#", x + 50, rowY - 2, TEXT_DIM, false);
            String name = bindingModule == m ? "Press a key..." : m.getName();
            NovolineFonts.thin(20).drawString(NovolineFonts.thin(20).trimStringToWidth(name, 87), x + 63, rowY, m.isEnabled() ? ACCENT : MUTED, false);
            rowY += 18.0F;
        }
        endScissor();
        if (selectedModule != null)
            NovolineFonts.thin(20).drawString(selectedModule.getName() + " Settings", x + NAV_W + MODULES_W + 15, y + 7, TEXT, false);
    }

    private void drawSettings(int mx, int my) {
        if (selectedModule == null) return;
        List<Setting> settings = visibleSettings(selectedModule);
        if (settings.isEmpty()) {
            String t = "NO SETTINGS ;(";
            fontRendererObj.drawStringWithShadow(t, x + NAV_W + MODULES_W + (settingsWidth - fontRendererObj.getStringWidth(t)) / 2.0F, y + (windowHeight - fontRendererObj.FONT_HEIGHT) / 2.0F, MUTED);
            return;
        }
        float scroll = settingScroll.getOrDefault(selectedModule, 0.0F);
        scroll = clampSettingScroll(settings, scroll);
        settingScroll.put(selectedModule, scroll);
        float sy = y + 32.0F + scroll;
        scissor(x + NAV_W + MODULES_W, y + HEADER_H, settingsWidth, windowHeight - HEADER_H);
        for (Setting s : settings) {
            drawSetting(s, sy, mx, my);
            sy += settingHeight(s);
        }
        endScissor();
        if (openCombo != null && settings.contains(openCombo)) drawComboOptions(openCombo, sy, mx, my);
    }

    private static String keyName(int k) {
        if (k == 0) return "NONE";
        if (k == 1069) return "MScrollUp";
        if (k == 1070) return "MScrollDown";
        if (k >= 1000) return "M" + (k - 1000);
        String n = Keyboard.getKeyName(k);
        return n == null ? "NONE" : n;
    }

    private void drawComboOptions(SliderSetting s, float sy, int mx, int my) {
        String[] o = s.getOptions();
        float right = x + NAV_W + MODULES_W + settingsWidth - 10.0F, left = right - 70.0F, bottom = sy + 10 + o.length * 11;
        scissor(x + NAV_W + MODULES_W, y + HEADER_H, settingsWidth, windowHeight - HEADER_H);
        bordered(left, sy + 10, right, bottom, CONTROL_BG);
        for (int i = 0; i < o.length; i++) {
            boolean hov = inside(mx, my, left, sy + 10 + i * 11, 70, 11);
            NovolineFonts.thin(16).drawCenteredString(o[i], left + 35, sy + 13 + i * 11, (int) s.getInput() == i || hov ? accent() : TEXT);
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
            selectedCategory = tab;
            selectedModule = null;
            activeSetting = null;
            openCombo = null;
            return;
        }
        Module mod = moduleAt(mx, my);
        if (mod != null) {
            if (button == 0 && mod.canBeEnabled()) mod.toggle();
            else if (button == 1) {
                selectedModule = selectedModule == mod ? null : mod;
                activeSetting = null;
                openCombo = null;
            } else if (button == 2) bindingModule = mod;
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
                if (inside(mx, my, left, comboY + 10 + i * 11, 70, 11)) {
                    openCombo.setValueWithEvent(i);
                    openCombo = null;
                    return;
                }
            }
        }
        float sy = y + 32 + scroll;
        for (Setting s : settings) {
            if (inside(mx, my, x + NAV_W + MODULES_W, sy - 3, settingsWidth, settingHeight(s))) {
                handleSettingClick(s, mx, sy, button);
                return;
            }
            sy += settingHeight(s);
        }
        if (button == 0) activeSetting = null;
    }

    private void handleSettingClick(Setting setting, int mx, float sy, int button) {
        float cLeft = x + totalWidth() - 80.0F;
        if (setting instanceof SliderSetting) {
            SliderSetting s = (SliderSetting) setting;
            if (s.isString) {
                if (button == 0) openCombo = openCombo == s ? null : s;
                else if (button == 1) cycleCombo(s, -1);
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

    @Override
    public void mouseReleased(int mx, int my, int state) {
        if (state == 0) {
            dragging = false;
            resizing = false;
            if (activeSetting instanceof SliderSetting || activeSetting instanceof ColorSetting) activeSetting = null;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth, my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        float dir = wheel > 0 ? 12.0F : -12.0F;
        if (inside(mx, my, x + NAV_W, y + HEADER_H, MODULES_W, windowHeight - HEADER_H))
            moduleScroll.put(selectedCategory, moduleScroll.get(selectedCategory) + dir);
        else if (selectedModule != null && inside(mx, my, x + NAV_W + MODULES_W, y + HEADER_H, settingsWidth, windowHeight - HEADER_H))
            settingScroll.put(selectedModule, settingScroll.getOrDefault(selectedModule, 0.0F) + dir);
    }

    @Override
    public void keyTyped(char ch, int key) {
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
        if (key == Keyboard.KEY_ESCAPE) mc.displayGuiScreen(null);
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
        if (!modules(selectedCategory).contains(selectedModule)) selectedModule = null;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        mc.getFramebuffer().bindFramebuffer(false);
        drawBlurBackground();
        ScaledResolution res = new ScaledResolution(this.mc);
        double scale = width <= 0 ? 1.0D : res.getScaledWidth() / (double) width;
        int lx = (int) Math.floor(mouseX / scale), ly = (int) Math.floor(mouseY / scale);
        updateWindowDrag(lx, ly);

        GlStateManager.pushMatrix();
        GlStateManager.scale(scale, scale, 1.0D);
        drawWindow(lx, ly);
        drawTabs(lx, ly);
        drawModules(lx, ly);
        drawSettings(lx, ly);
        GlStateManager.popMatrix();
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

    private void drawSetting(Setting setting, float sy, int mx, int my) {
        float left = x + NAV_W + MODULES_W + 10.0F, right = x + NAV_W + MODULES_W + settingsWidth - 10.0F, cLeft = right - 70.0F;
        NovolineFonts.thin(17).drawString(setting instanceof DescriptionSetting ? ((DescriptionSetting) setting).getDesc() : setting.getName(),
                left, sy, setting instanceof DescriptionSetting ? TEXT_DIM : TEXT, false);
        if (setting instanceof DescriptionSetting) return;

        if (setting instanceof SliderSetting) {
            SliderSetting s = (SliderSetting) setting;
            if (s.isString) {
                bordered(cLeft, sy - 2, right, sy + 8, inside(mx, my, cLeft, sy - 2, 70, 10) ? accent() : CONTROL_BG);
                String[] o = s.getOptions();
                int idx = (int) clamp((float) s.getInput(), 0, o.length - 1);
                NovolineFonts.thin(16).drawCenteredString(o[idx], cLeft + 35, sy, TEXT);
            } else {
                double range = Math.max(0.00001D, s.getMax() - s.getMin());
                float pct = (float) ((s.getInput() - s.getMin()) / range);
                bordered(cLeft, sy + 2, right, sy + 4, CONTROL_BG);
                Gui.drawRect((int) cLeft, (int) sy + 2, (int) (cLeft + 70 * pct), (int) sy + 4, accent());
                drawCircle(cLeft + 70 * pct, sy + 3, 2, TEXT);
                NovolineFonts.thin(12).drawCenteredString(Utils.asWholeNum(s.getInput()) + s.getSuffix(), cLeft + 70 * pct, sy - 5, TEXT);
                if (activeSetting == s && Mouse.isButtonDown(0)) updateSlider(s, mx, cLeft);
            }
            return;
        }
        if (setting instanceof ButtonSetting) {
            ButtonSetting b = (ButtonSetting) setting;
            bordered(right - 10, sy - 2, right, sy + 8, CONTROL_BG);
            if (b.isMethodButton) NovolineFonts.thin(16).drawCenteredString("+", right - 5, sy, accent());
            else if (b.isToggled()) drawCheck(right - 8, sy + 2, CHECK);
            return;
        }
        if (setting instanceof TextSetting) {
            TextSetting t = (TextSetting) setting;
            bordered(cLeft, sy - 2, right, sy + 8, activeSetting == t ? accent() : CONTROL_BG);
            String v = t.getText().isEmpty() ? t.getPlaceholder() : t.getText();
            v = NovolineFonts.thin(16).trimStringToWidth(v, 64, true);
            NovolineFonts.thin(16).drawString(v + (activeSetting == t && caretVisible() ? "|" : ""), cLeft + 2, sy, t.getText().isEmpty() ? MUTED : TEXT, false);
            return;
        }
        if (setting instanceof KeySetting) {
            bordered(cLeft, sy - 2, right, sy + 8, activeSetting == setting ? accent() : CONTROL_BG);
            NovolineFonts.thin(16).drawCenteredString(activeSetting == setting ? "Press a key" : keyName(((KeySetting) setting).getKey()), cLeft + 35, sy, TEXT);
            return;
        }
        if (setting instanceof ColorSetting) {
            ColorSetting c = (ColorSetting) setting;
            ColorMode mode = colorModes.getOrDefault(c, ColorMode.HUE);
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            for (int i = 0; i < 70; i++) {
                float v = i / 69.0F;
                int rgb = mode == ColorMode.HUE ? Color.HSBtoRGB(v, hsb[1], hsb[2]) : mode == ColorMode.SATURATION ? Color.HSBtoRGB(hsb[0], v, hsb[2]) : Color.HSBtoRGB(hsb[0], hsb[1], v);
                Gui.drawRect((int) cLeft + i, (int) sy - 2, (int) cLeft + i + 1, (int) sy + 8, rgb);
            }
            float marker = mode == ColorMode.HUE ? hsb[0] : mode == ColorMode.SATURATION ? hsb[1] : hsb[2];
            Gui.drawRect((int) (cLeft + marker * 69), (int) sy - 2, (int) (cLeft + marker * 69) + 1, (int) sy + 8, TEXT);
            if (activeSetting == c && Mouse.isButtonDown(0)) updateColor(c, mode, mx, cLeft);
            return;
        }
        if (setting instanceof GroupSetting) {
            GroupSetting g = (GroupSetting) setting;
            NovolineFonts.thin(16).drawString(g.isOpened() ? "-" : "+", right - 8, sy, g.isOpened() ? accent() : TEXT, false);
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void bordered(float l, float t, float r, float b, int border) {
        Gui.drawRect((int) l - 1, (int) t - 1, (int) r + 1, (int) b + 1, border);
        Gui.drawRect((int) l, (int) t, (int) r, (int) b, CHANNELS);
    }

    private void drawCheck(float cx, float cy, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Color c = new Color(color, false); // ← 改了：false 忽略 alpha，保证不透明
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
        GL11.glLineWidth(1.0F);
        GL11.glBegin(GL11.GL_LINE_STRIP);
        GL11.glVertex2f(cx, cy + 2.0F);
        GL11.glVertex2f(cx + 2.0F, cy + 4.0F);
        GL11.glVertex2f(cx + 5.5F, cy);
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void drawCircle(float cx, float cy, float radius, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Color c = new Color(color, false); // ← 改了：false 忽略 alpha，保证不透明
        GL11.glColor4f(c.getRed() / 255f, c.getGreen() / 255f, c.getBlue() / 255f, c.getAlpha() / 255f);
        GL11.glBegin(GL11.GL_TRIANGLE_FAN);
        GL11.glVertex2f(cx, cy);
        for (int i = 0; i <= 36; i++) {
            double a = Math.PI * 2.0D * i / 36.0D;
            GL11.glVertex2d(cx + Math.sin(a) * radius, cy + Math.cos(a) * radius);
        }
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private void scissor(float sx, float sy, float sw, float sh) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        RenderUtils.scissor(sx, sy, sw, sh);
    }

    private void endScissor() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    private int totalWidth() {
        return NAV_W + MODULES_W + settingsWidth;
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
            settingsWidth = Math.max(MIN_SETTINGS, Math.min(resizeOffsetX + mx, Math.max(MIN_SETTINGS, width - x - NAV_W - MODULES_W)));
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
        if (!inside(mx, my, x + NAV_W, y + HEADER_H, MODULES_W, windowHeight - HEADER_H)) return null;
        float rowY = y + 30 + moduleScroll.get(selectedCategory);
        for (Module m : modules(selectedCategory)) {
            if (inside(mx, my, x + NAV_W, rowY - 4, MODULES_W, 18)) return m;
            rowY += 18;
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
        float content = m.size() * 18.0F, viewport = windowHeight - 30.0F;
        return clamp(s, Math.min(0, viewport - content), 0);
    }

    private float clampSettingScroll(List<Setting> s, float sc) {
        float content = 0;
        for (Setting set : s) content += settingHeight(set);
        float viewport = windowHeight - 32.0F;
        return clamp(sc, Math.min(0, viewport - content), 0);
    }

    private float settingScreenY(List<Setting> settings, Setting target, float scroll) {
        float sy = y + 32 + scroll;
        for (Setting s : settings) {
            if (s == target) return sy;
            sy += settingHeight(s);
        }
        return sy;
    }

    private float settingHeight(Setting s) {
        return s instanceof DescriptionSetting ? 14.0F : 18.0F;
    }

    private void updateSlider(SliderSetting s, int mx, float left) {
        double frac = clamp((mx - left) / 70.0F, 0, 1);
        s.setValueWithEvent(s.getMin() + (s.getMax() - s.getMin()) * frac);
    }

    private void cycleCombo(SliderSetting s, int amount) {
        int len = s.getOptions().length, next = ((int) s.getInput() + amount) % len;
        if (next < 0) next += len;
        s.setValueWithEvent(next);
    }

    private void updateColor(ColorSetting c, ColorMode mode, int mx, float left) {
        float v = clamp((mx - left) / 70.0F, 0, 1);
        if (mode == ColorMode.HUE) c.setHue(v * 360.0F);
        else if (mode == ColorMode.SATURATION) c.setSaturation(v);
        else c.setBrightness(v);
    }

    private enum ColorMode {HUE, SATURATION, BRIGHTNESS}
}