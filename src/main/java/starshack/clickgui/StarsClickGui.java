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
import starshack.font.StarsFonts;
import starshack.font.api.FontRenderer;
import starshack.module.Module;
import starshack.module.setting.Setting;
import starshack.module.setting.impl.*;
import starshack.utility.RenderUtils;
import starshack.utility.Utils;

import java.awt.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * "Stars" ClickGUI - unified three column window.
 * <p>
 * Visual language (v2): one accent ramp (blue -> cyan) used only for
 * progress / selection / enabled states, a real type scale instead of
 * one thin weight everywhere, and a shared metric table so the draw pass
 * and the hit-test pass can never drift apart.
 */
public final class StarsClickGui extends ClickGui {

    // ------------------------------------------------------------------
    // palette
    // ------------------------------------------------------------------
    private static final int OVERLAY = 0x4D000000;      // desktop dim behind the frosted window
    private static final int WIN_BG = 0xC4131419;       // window material: 77% -> frosted glass (the world tints it, that is what translucency means)
    private static final int WIN_EDGE = 0x26FFFFFF;     // window hairline
    private static final int SIDEBAR = 0x0FFFFFFF;      // sidebar material
    private static final int CONTENT = 0x06FFFFFF;      // content material
    private static final int HEADER_BG = 0x0AFFFFFF;    // toolbar material
    private static final int FIELD = 0x0FFFFFFF;
    private static final int FIELD_HOV = 0x1AFFFFFF;
    private static final int FIELD_EDGE = 0x1CFFFFFF;
    private static final int ROW_HOVER = 0x16FFFFFF;
    private static final int ROW_SEL = 0x330A84FF;
    private static final int CHIP = 0x1AFFFFFF;
    private static final int SWITCH_OFF = 0x33FFFFFF;
    private static final int TRACK = 0x30FFFFFF;
    private static final int BORDER = 0x26FFFFFF;
    private static final int STROKE = 0x14FFFFFF;
    private static final int STROKE_SOFT = 0x0AFFFFFF;
    private static final int ACCENT = 0xFF0A84FF;
    private static final int ACCENT2 = 0xFF64D2FF;
    private static final int ACCENT_SOFT = 0x400A84FF;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int TEXT_SOFT = 0xFFC7CBD1;
    private static final int TEXT_DIM = 0xFF9BA1AA;
    private static final int MUTED = 0xFF8A8F98;

    // ------------------------------------------------------------------
    // metrics
    // ------------------------------------------------------------------
    private static final int NAV_W = 52;
    private static final int MIN_SETTINGS = 178;
    private static final int MIN_HEIGHT = 300;
    private static final int HEADER_H = 34;
    private static final int SEARCH_H = 22;
    private static final float ROW_H = 26.0F;
    private static final float PAD = 9.0F;
    private static final float CTRL_W = 84.0F;
    private static final float RADIUS_WINDOW = 8.0F;
    private static final float RADIUS_ROW = 7.0F;
    private static final float TAB_BOX = 30.0F;

    private static final boolean USE_IMAGE_LOGO = true;
    private static final ResourceLocation LOGO_TEXTURE =
            new ResourceLocation("starshack", "textures/gui/logo.png");
    private final Map<Setting, Float> settingHoverProgress = new IdentityHashMap<>();
    private float modulePanelWidth = 132F;

    private float categoryFadeAlpha = 1F;
    private float categoryOffsetY = 0F;

    private final Map<Module, Float> moduleHoverProgress = new IdentityHashMap<>();
    private float targetModulePanelWidth = 132F;
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
    private String cachedSearchQuery = null;
    private Module.category cachedCategory = null;
    private boolean searching = false;
    private float scrollbarAlpha = 0F;
    private long lastScrollTime = 0L;

    private int keyboardIndex = 0;

    private boolean dragging, resizing;
    private int dragOffsetX, dragOffsetY, resizeOffsetX, resizeOffsetY;
    private int x = 100, y = 100, settingsWidth = MIN_SETTINGS, windowHeight = 330;
    private boolean positioned;

    public StarsClickGui() {
        super();
        for (Module.category c : Module.category.values()) moduleScroll.put(c, 0.0F);
    }

    // ------------------------------------------------------------------
    // small helpers
    // ------------------------------------------------------------------

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

    private static int withAlpha(int color, float alpha) {
        int a = (int) (((color >>> 24) & 0xFF) * clamp(alpha, 0F, 1F));
        return (a << 24) | (color & 0x00FFFFFF);
    }

    /**
     * Rounded rectangle filled with {@code color} plus a 1px {@code border} ring.
     * The ring is a real outline (GL line loop): drawing a filled border rect and then
     * covering it with the fill only works while the fill is opaque, which ours is not.
     */
    private static void box(float l, float t, float r, float b, float radius, int color, int border) {
        RenderUtils.drawRoundedRectangle(l, t, r, b, radius, color);
        if (border != 0) ring(l, t, r, b, radius, border);
    }

    private static void ring(float l, float t, float r, float b, float radius, int color) {
        float rad = Math.min(radius, Math.min((r - l) / 2.0F, (b - t) / 2.0F));
        Color c = new Color(color, true);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GL11.glEnable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(2.0F);
        GL11.glColor4f(c.getRed() / 255F, c.getGreen() / 255F, c.getBlue() / 255F, c.getAlpha() / 255F);
        GL11.glBegin(GL11.GL_LINE_LOOP);
        for (int i = 0; i <= 90; i += 6) {
            double a = i * 0.017453292F;
            GL11.glVertex2d(l + rad - Math.sin(a) * rad, t + rad - Math.cos(a) * rad);
        }
        for (int i = 90; i <= 180; i += 6) {
            double a = i * 0.017453292F;
            GL11.glVertex2d(l + rad - Math.sin(a) * rad, b - rad - Math.cos(a) * rad);
        }
        for (int i = 0; i <= 90; i += 6) {
            double a = i * 0.017453292F;
            GL11.glVertex2d(r - rad + Math.sin(a) * rad, b - rad + Math.cos(a) * rad);
        }
        for (int i = 90; i <= 180; i += 6) {
            double a = i * 0.017453292F;
            GL11.glVertex2d(r - rad + Math.sin(a) * rad, t + rad + Math.cos(a) * rad);
        }
        GL11.glEnd();
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glPopMatrix();
    }

    private static void hGrad(float l, float t, float r, float b, float radius, int c1, int c2) {
        if (r - l < 3.0F) {
            RenderUtils.drawRoundedRectangle(l, t, r, b, radius, c1);
            return;
        }
        RenderUtils.drawRoundedGradientRect(l, t, r, b, radius, c1, c1, c2, c2);
    }

    /**
     * Draws text vertically centred inside a row.
     */
    private static void textV(FontRenderer fr, CharSequence s, float x, float top, float rowH, int color) {
        float ty = Math.round(top + (rowH - fr.getHeight()) / 2.0F + 2.0F);
        fr.drawString(s, Math.round(x), ty, color, false);
    }

    private static String title(String n) {
        if (n == null || n.isEmpty()) return "";
        return n.substring(0, 1).toUpperCase(Locale.ROOT) + n.substring(1).toLowerCase(Locale.ROOT);
    }

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

    private static String keyName(int k) {
        if (k == 0) return "NONE";
        if (k == 1069) return "MScrollUp";
        if (k == 1070) return "MScrollDown";
        if (k >= 1000) return "M" + (k - 1000);
        String n = Keyboard.getKeyName(k);
        return n == null ? "NONE" : n;
    }

    // ------------------------------------------------------------------
    // layout (single source of truth for both drawing and hit testing)
    // ------------------------------------------------------------------

    private int totalWidth() {
        return NAV_W + modulePanelW() + settingsWidth;
    }

    private int modulePanelW() {
        return (int) modulePanelWidth;
    }

    /**
     * Search field lives in the toolbar, right aligned: fixed width so it never jitters.
     */
    private float searchTop() {
        return y + (HEADER_H - SEARCH_H) / 2.0F;
    }

    /**
     * Keeps the search clear of the title + module count on narrow windows.
     */
    private float searchWidth() {
        return clamp(totalWidth() - 212.0F, 120.0F, 190.0F);
    }

    private float searchLeft() {
        return x + totalWidth() - 12.0F - searchWidth();
    }

    private float searchRight() {
        return x + totalWidth() - 12.0F;
    }

    /**
     * Module rows.
     */
    private float rowLeft() {
        return x + NAV_W + PAD;
    }

    private float rowRight() {
        return x + NAV_W + modulePanelW() - PAD;
    }

    private float listTop() {
        return y + HEADER_H + 6.0F;
    }

    private float listViewport() {
        return windowHeight - (listTop() - y) - 6.0F;
    }

    private float settingsTop() {
        return y + HEADER_H + 27.0F;
    }

    private float settingsViewport() {
        return windowHeight - (settingsTop() - y) - 6.0F;
    }

    private float ctrlLeft() {
        return x + totalWidth() - PAD - 5.0F - CTRL_W;
    }

    private float ctrlRight() {
        return x + totalWidth() - PAD - 5.0F;
    }

    private float settingsLeft() {
        return x + NAV_W + modulePanelW();
    }

    private float settingHeight(Setting s) {
        if (s instanceof DescriptionSetting) return 16.0F;
        if (s instanceof SliderSetting && !((SliderSetting) s).isString) return 36.0F;
        if (s instanceof GroupSetting) return 26.0F;
        return 30.0F;
    }

    private void updateModulePanelWidth(List<Module> mods) {
        targetModulePanelWidth = 110F;
        FontRenderer fr = StarsFonts.thin(19);
        for (Module m : mods) {
            float w = fr.stringWidth(m.getName()) + 62F;
            if (w > targetModulePanelWidth) targetModulePanelWidth = w;
        }
        targetModulePanelWidth = Math.min(196F, Math.max(132F, targetModulePanelWidth));
        modulePanelWidth += (targetModulePanelWidth - modulePanelWidth) * 0.25F;
    }

    private List<Module> getDisplayModules() {
        if (cachedCategory == selectedCategory && cachedSearchQuery != null && cachedSearchQuery.equals(searchQuery)) {
            return filteredModules;
        }
        List<Module> all = modules(selectedCategory);
        filteredModules.clear();
        if (searchQuery.isEmpty()) {
            filteredModules.addAll(all);
        } else {
            String q = searchQuery.toLowerCase(Locale.ROOT);
            for (Module m : all) {
                if (m.matchesSearch(q)) filteredModules.add(m);
            }
        }
        cachedCategory = selectedCategory;
        cachedSearchQuery = searchQuery;
        return filteredModules;
    }

    private boolean caretVisible() {
        return System.currentTimeMillis() / 500L % 2L == 0L;
    }

    private float tabGap() {
        Module.category[] cats = Module.category.values();
        float avail = windowHeight - HEADER_H - 24.0F;
        return cats.length <= 1 ? TAB_BOX : Math.min(TAB_BOX + 4.0F, avail / cats.length);
    }

    private float tabTop(int i) {
        return y + HEADER_H + 12.0F + i * tabGap();
    }

    private float tabSize() {
        return Math.min(TAB_BOX, tabGap() - 4.0F);
    }

    private Module.category tabAt(int mx, int my) {
        Module.category[] cats = Module.category.values();
        float size = tabSize();
        float cx = x + NAV_W / 2.0F;
        for (int i = 0; i < cats.length; i++) {
            float l = cx - size / 2.0F, t = tabTop(i);
            if (inside(mx, my, l, t, size, size)) return cats[i];
        }
        return null;
    }

    private Module moduleAt(int mx, int my) {
        if (!inside(mx, my, x + NAV_W, listTop(), modulePanelW(), listViewport())) return null;
        float rowY = listTop() + moduleScroll.get(selectedCategory);
        for (Module m : getDisplayModules()) {
            if (inside(mx, my, rowLeft(), rowY, rowRight() - rowLeft(), ROW_H - 2.0F)) return m;
            rowY += ROW_H;
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
        float content = m.size() * ROW_H;
        return clamp(s, Math.min(0, listViewport() - content), 0);
    }

    private float clampSettingScroll(List<Setting> s, float sc) {
        float content = 0;
        for (Setting set : s) content += settingHeight(set);
        return clamp(sc, Math.min(0, settingsViewport() - content), 0);
    }

    private float settingScreenY(List<Setting> settings, Setting target, float scroll) {
        float sy = settingsTop() + scroll;
        for (Setting s : settings) {
            if (s == target) return sy;
            sy += settingHeight(s);
        }
        return sy;
    }

    // ------------------------------------------------------------------
    // window
    // ------------------------------------------------------------------

    private void drawWindow(int mx, int my) {
        int right = x + totalWidth();
        int bottom = y + windowHeight;

        // wide, soft macOS-style window shadow
        for (int i = 8; i >= 1; i--) {
            float o = i * 1.35F;
            RenderUtils.drawRoundedRectangle(x - o, y - o + 5.0F, right + o, bottom + o + 5.0F,
                    RADIUS_WINDOW + o, (0x04 + (8 - i) * 3) << 24);
        }

        // frosted body with a 1px highlight edge
        RenderUtils.drawRoundedRectangle(x, y, right, bottom, RADIUS_WINDOW, WIN_EDGE);
        RenderUtils.drawRoundedRectangle(x + 1, y + 1, right - 1, bottom - 1,
                RADIUS_WINDOW - 1.0F, WIN_BG);

        // sidebar material (slightly lighter than the content, squared off on the right)
        RenderUtils.drawRoundedRectangle(x + 1, y + 1, x + NAV_W, bottom - 1,
                RADIUS_WINDOW - 1.0F, SIDEBAR);
        Gui.drawRect(x + NAV_W - 5, y + 1, x + NAV_W, bottom - 1, SIDEBAR);

        // content material + full width toolbar
        Gui.drawRect(x + NAV_W, y + 1, right - 1, bottom - 1, CONTENT);
        Gui.drawRect(x + 1, y + 1, right - 1, y + HEADER_H, HEADER_BG);
        Gui.drawRect(x + 1, y + HEADER_H - 1, right - 1, y + HEADER_H, STROKE);
        Gui.drawRect(x + NAV_W, y + HEADER_H, x + NAV_W + 1, bottom - 1, STROKE);

        // toolbar: small app icon, title, count badge, breadcrumb
        float badge = y + (HEADER_H - 18.0F) / 2.0F;
        RenderUtils.drawRoundedRectangle(x + 14.0F, badge, x + 32.0F, badge + 18.0F,
                5.5F, 0x1FFFFFFF);
        if (USE_IMAGE_LOGO) {
            drawLogo(x + 23.0F, badge + 9.0F, 13.0F);
        } else {
            StarsFonts.thin(15).drawCenteredString("S", x + 23.0F, badge + 3.0F, ACCENT2);
        }

        FontRenderer titleFont = StarsFonts.thin(20);
        String title = categoryName(selectedCategory);
        float titleX = x + 39.0F;
        textV(titleFont, title, titleX, y + 1, HEADER_H - 1.0F, TEXT);
        drawPill(getDisplayModules().size() + " modules",
                titleX + titleFont.stringWidth(title) + 7.0F, y + 1, HEADER_H - 1.0F);


        if (inside(mx, my, right - 14, bottom - 14, 14, 14)) {
            RenderUtils.drawRoundedRectangle(right - 9, bottom - 4, right - 4, bottom - 1, 1.5F, ACCENT);
            RenderUtils.drawRoundedRectangle(right - 4, bottom - 9, right - 1, bottom - 4, 1.5F, ACCENT2);
        }
    }

    /**
     * logo.png is 1030x1030 but the mark only covers 600x595 anchored at the top left,
     * so we draw a larger quad and let the transparent padding fall outside the badge.
     */
    private void drawLogo(float centerX, float centerY, float size) {
        final float TEX = 1030.0F, SRC_X = 6.0F, SRC_Y = 8.0F, SRC_W = 600.0F, SRC_H = 595.0F;
        // drawModalRectWithCustomSizedTexture maps (u..u+width)/textureWidth, so passing the quad
        // size as its own texture size maps the whole image; the padding then falls outside the badge.
        int q = Math.round(size * TEX / Math.max(SRC_W, SRC_H));
        float ratio = (float) q / TEX;
        int qx = Math.round(centerX - (SRC_X + SRC_W / 2.0F) * ratio);
        int qy = Math.round(centerY - (SRC_Y + SRC_H / 2.0F) * ratio);
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableBlend();
        mc.getTextureManager().bindTexture(LOGO_TEXTURE);
        drawModalRectWithCustomSizedTexture(qx, qy, 0, 0, q, q, q, q);
    }


    private void drawPill(String text, float l, float top, float h) {
        FontRenderer fr = StarsFonts.thin(13);
        float w = fr.stringWidth(text) + 12.0F;
        float bH = 15.0F, t = top + (h - bH) / 2.0F;
        RenderUtils.drawRoundedRectangle(l, t, l + w, t + bH, 7.5F, 0x14FFFFFF);
        textV(fr, text, l + 6.0F, t, bH, MUTED);
    }

    // ------------------------------------------------------------------
    // category rail
    // ------------------------------------------------------------------

    private void drawTabs(int mx, int my) {
        Module.category[] cats = Module.category.values();
        float size = tabSize();
        float cx = x + NAV_W / 2.0F;
        FontRenderer icons = StarsFonts.icons(size >= 28 ? 34 : 28);

        for (int i = 0; i < cats.length; i++) {
            Module.category cat = cats[i];
            float top = tabTop(i);
            float l = cx - size / 2.0F;
            boolean sel = cat == selectedCategory;
            boolean hov = inside(mx, my, l, top, size, size);

            if (sel) {
                RenderUtils.drawRoundedRectangle(l, top, l + size, top + size, 8.0F, ACCENT_SOFT);
            } else if (hov) {
                RenderUtils.drawRoundedRectangle(l, top, l + size, top + size, 8.0F, ROW_HOVER);
            }
            int col = sel ? 0xFFFFFFFF : (hov ? TEXT : MUTED);
            icons.drawCenteredString(tabIcon(cat), cx, top + (size - icons.getHeight()) / 2.0F + 1.0F, col);
        }
    }

    // ------------------------------------------------------------------
    // module list
    // ------------------------------------------------------------------

    private void drawModules(int mx, int my) {
        List<Module> mods = getDisplayModules();
        updateModulePanelWidth(mods);

        float scroll = clampModuleScroll(mods, moduleScroll.get(selectedCategory));
        moduleScroll.put(selectedCategory, scroll);

        GlStateManager.pushAttrib();
        GlStateManager.color(1F, 1F, 1F, categoryFadeAlpha);

        drawSearchField(mx, my);

        scissor(x + NAV_W, listTop(), modulePanelW(), listViewport());
        float rowY = listTop() + scroll + categoryOffsetY;

        for (Module m : mods) {
            float rowTop = rowY;
            float rowH = ROW_H - 2.0F;
            boolean hov = inside(mx, my, rowLeft(), rowTop, rowRight() - rowLeft(), rowH);
            boolean sel = m == selectedModule;

            float cur = lerp(moduleHoverProgress.getOrDefault(m, 0F), hov ? 1F : 0F, 0.3F);
            moduleHoverProgress.put(m, cur);

            if (sel) {
                // selected = neutral highlight only; no colour anywhere on the row
                RenderUtils.drawRoundedRectangle(rowLeft(), rowTop, rowRight(), rowTop + rowH,
                        RADIUS_ROW, 0x1FFFFFFF);
            } else if (cur > 0.01F) {
                RenderUtils.drawRoundedRectangle(rowLeft(), rowTop, rowRight(), rowTop + rowH,
                        RADIUS_ROW, withAlpha(ROW_HOVER, cur));
            }

            float dotX = rowLeft() + 11.0F;
            float midY = rowTop + rowH / 2.0F;
            if (m.isEnabled()) {
                drawCircle(dotX, midY, 3.3F, ACCENT);
                drawCircle(dotX, midY, 1.8F, ACCENT2);
            }

            String name = bindingModule == m ? "press a key" : m.getName();
            int nameColor = bindingModule == m ? ACCENT2 : ((m.isEnabled() || sel) ? TEXT : TEXT_DIM);
            FontRenderer nameFont = StarsFonts.thin(19);
            float keyReserve = 0.0F;
            if (m != bindingModule && m.getKeycode() != 0) {
                keyReserve = StarsFonts.thin(13).stringWidth(keyName(m.getKeycode())) + 18.0F;
            }
            float nameRight = rowRight() - 8.0F - keyReserve;
            float nameW = nameRight - (dotX + 9.0F);
            String shown = nameFont.trimStringToWidth(name, (int) Math.max(10, nameW), false);
            textV(nameFont, shown, dotX + 9.0F, rowTop, rowH, nameColor);

            if (keyReserve > 0.0F) {
                drawKeyChip(keyName(m.getKeycode()), rowRight() - 5.0F, rowTop, rowH);
            }

            rowY += ROW_H;
        }
        endScissor();
        GlStateManager.popAttrib();

        boolean overList = inside(mx, my, x + NAV_W, listTop(), modulePanelW(), listViewport());
        boolean showBar = overList || System.currentTimeMillis() - lastScrollTime < 1400L;
        scrollbarAlpha = lerp(scrollbarAlpha, showBar ? 1F : 0F, 0.18F);
        if (scrollbarAlpha > 0.02F) {
            drawScrollbar(x + NAV_W + modulePanelW() - 4.0F, listTop() + 3.0F, listViewport() - 6.0F,
                    mods.size() * ROW_H, scroll, scrollbarAlpha);
        }
    }

    private void drawSearchField(int mx, int my) {
        float top = searchTop();
        boolean hov = inside(mx, my, searchLeft(), top, searchRight() - searchLeft(), SEARCH_H);
        int bg = hov ? FIELD_HOV : FIELD;
        box(searchLeft(), top, searchRight(), top + SEARCH_H, 6.5F, bg,
                searching ? ACCENT : FIELD_EDGE);

        float gx = searchLeft() + 12.0F, gy = top + SEARCH_H / 2.0F - 1.0F;
        drawCircle(gx, gy, 3.6F, searching ? ACCENT : MUTED);
        drawCircle(gx, gy, 2.3F, bg);
        Gui.drawRect((int) (gx + 2.5F), (int) (gy + 2.5F), (int) (gx + 4.0F), (int) (gy + 4.0F),
                searching ? ACCENT : MUTED);

        String text = searchQuery.isEmpty() ? "Search" : searchQuery;
        int col = searchQuery.isEmpty() ? MUTED : TEXT;
        FontRenderer fr = StarsFonts.thin(15);
        textV(fr, text, searchLeft() + 21.0F, top, SEARCH_H, col);
        if (searching && caretVisible()) {
            float cx = searchLeft() + 21.0F + fr.stringWidth(text) + 1.0F;
            Gui.drawRect((int) cx, (int) (top + 6.0F), (int) cx + 1, (int) (top + SEARCH_H - 6.0F), ACCENT);
        }
    }

    private void drawKeyChip(String key, float rightX, float rowTop, float rowH) {
        FontRenderer fr = StarsFonts.thin(13);
        float w = fr.stringWidth(key) + 10.0F, h = 14.0F;
        float l = rightX - w, t = rowTop + (rowH - h) / 2.0F;
        RenderUtils.drawRoundedRectangle(l, t, rightX, t + h, 4.0F, CHIP);
        textV(fr, key, l + 5.0F, t, h, TEXT_DIM);
    }

    private void drawScrollbar(float barX, float top, float viewport, float content, float scroll, float alpha) {
        if (content <= viewport + 0.5F || alpha <= 0.02F) return;
        float barH = Math.max(20.0F, viewport * viewport / content);
        float t = -scroll / (content - viewport);
        float barY = top + t * (viewport - barH);
        RenderUtils.drawRoundedRectangle(barX, barY, barX + 3.0F, barY + barH, 1.5F,
                withAlpha(0x6BFFFFFF, alpha));
    }

    // ------------------------------------------------------------------
    // settings
    // ------------------------------------------------------------------

    private void drawSettings(int mx, int my) {
        if (selectedModule == null) {
            drawEmptyState();
            return;
        }

        List<Setting> settings = visibleSettings(selectedModule);
        if (settings.isEmpty()) {
            drawEmptyState();
            return;
        }
        float sl = settingsLeft();
        float sr = x + totalWidth();

        FontRenderer head = StarsFonts.thin(16);
        textV(head, selectedModule.getName(), sl + 14.0F, y + HEADER_H + 3.0F, 18.0F, TEXT);
        Gui.drawRect((int) sl, (int) (y + HEADER_H + 23.0F), (int) sr, (int) (y + HEADER_H + 24.0F), STROKE);

        float scroll = clampSettingScroll(settings, settingScroll.getOrDefault(selectedModule, 0.0F));
        settingScroll.put(selectedModule, scroll);

        GlStateManager.pushAttrib();
        GlStateManager.color(1F, 1F, 1F, categoryFadeAlpha);

        float sy = settingsTop() + scroll + categoryOffsetY;
        scissor(sl, settingsTop(), sr - sl, settingsViewport());

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

        float content = 0;
        for (Setting s : settings) content += settingHeight(s);
        boolean overSet = inside(mx, my, settingsLeft(), settingsTop(), sr - settingsLeft(), settingsViewport());
        scrollbarAlpha = lerp(scrollbarAlpha, (overSet || System.currentTimeMillis() - lastScrollTime < 1400L) ? 1F : 0F, 0.18F);
        if (scrollbarAlpha > 0.02F) {
            drawScrollbar(sr - 4.0F, settingsTop() + 3.0F, settingsViewport() - 6.0F, content, scroll, scrollbarAlpha);
        }

        GlStateManager.popAttrib();
    }

    private void drawEmptyState() {
        float l = settingsLeft(), r = x + totalWidth();
        float cx = (l + r) / 2.0F, cy = y + windowHeight / 2.0F;
        drawCircle(cx, cy - 20.0F, 3.0F, withAlpha(ACCENT, 0.85F));
        StarsFonts.thin(17).drawCenteredString("No module selected", cx, cy, TEXT_SOFT);
        StarsFonts.thin(13).drawCenteredString("Right-click a module in the list to edit it", cx, cy + 18.0F, MUTED);
    }

    private void drawSetting(Setting setting, float sy, int mx, int my) {
        float sl = settingsLeft();
        float sr = x + totalWidth();
        float left = sl + 14.0F;
        float right = ctrlRight();
        float rowH = settingHeight(setting);

        if (setting instanceof DescriptionSetting) {
            StarsFonts.thin(13).drawString(((DescriptionSetting) setting).getDesc(), left,
                    Math.round(sy + 3.0F), MUTED, false);
            return;
        }

        boolean hov = inside(mx, my, sl + 4.0F, sy, sr - sl - 8.0F, rowH);
        float cur = lerp(settingHoverProgress.getOrDefault(setting, 0F), hov ? 1F : 0F, 0.3F);
        settingHoverProgress.put(setting, cur);

        if (cur > 0.01F) {
            RenderUtils.drawRoundedRectangle(sl + 4.0F, sy + 1.0F, sr - 4.0F, sy + rowH - 1.5F, 6.0F,
                    withAlpha(ROW_HOVER, cur));
        } else if (!(setting instanceof GroupSetting)) {
            Gui.drawRect((int) (sl + 14.0F), (int) (sy + rowH - 1.0F), (int) (sr - 14.0F),
                    (int) (sy + rowH - 0.5F), 0x12FFFFFF);
        }

        boolean group = setting instanceof GroupSetting;
        boolean twoLine = setting instanceof SliderSetting && !((SliderSetting) setting).isString;
        FontRenderer label = group ? StarsFonts.thin(15) : StarsFonts.thin(16);
        float labelMax = ctrlLeft() - left - 10.0F;
        String labelText = label.trimStringToWidth(setting.getName(), (int) Math.max(20, labelMax), false);
        textV(label, labelText, left, sy, twoLine ? 20.0F : rowH, group ? TEXT_SOFT : TEXT);

        if (group) {
            GroupSetting g = (GroupSetting) setting;
            FontRenderer chev = StarsFonts.thin(15);
            String arrow = g.isOpened() ? "v" : ">";
            textV(chev, arrow, right - chev.stringWidth(arrow), sy, rowH, ACCENT);
            return;
        }

        if (setting instanceof SliderSetting) {
            SliderSetting s = (SliderSetting) setting;
            if (s.isString) {
                drawCombo(s, sy, right, hov || openCombo == s);
            } else {
                drawSlider(s, sy, right, mx, hov);
            }
            return;
        }

        if (setting instanceof ButtonSetting) {
            ButtonSetting b = (ButtonSetting) setting;
            if (b.isMethodButton) {
                FontRenderer fr = StarsFonts.thin(15);
                box(right - 26.0F, sy + (rowH - 18.0F) / 2.0F, right, sy + (rowH + 18.0F) / 2.0F,
                        6.0F, FIELD, hov ? ACCENT : FIELD_EDGE);
                textV(fr, "+", right - 13.0F - fr.stringWidth("+") / 2.0F,
                        sy + (rowH - 18.0F) / 2.0F, 18.0F, ACCENT);
            } else {
                float target = b.isToggled() ? 1F : 0F;
                float anim = lerp(switchAnimProgress.getOrDefault(b, target), target, 0.28F);
                switchAnimProgress.put(b, anim);
                drawSwitch(right - 30.0F, sy + (rowH - 16.0F) / 2.0F, anim);
            }
            return;
        }

        if (setting instanceof TextSetting) {
            TextSetting t = (TextSetting) setting;
            boolean focus = activeSetting == t;
            box(ctrlLeft(), sy + (rowH - 20.0F) / 2.0F, right, sy + (rowH + 20.0F) / 2.0F, 6.0F,
                    FIELD, focus ? ACCENT : FIELD_EDGE);
            FontRenderer fr = StarsFonts.thin(15);
            String v = t.getText().isEmpty() ? t.getPlaceholder() : t.getText();
            v = fr.trimStringToWidth(v, (int) (CTRL_W - 16.0F), true);
            textV(fr, v + (focus && caretVisible() ? "|" : ""), ctrlLeft() + 7.0F, sy, rowH,
                    t.getText().isEmpty() ? MUTED : TEXT);
            return;
        }

        if (setting instanceof KeySetting) {
            boolean focus = activeSetting == setting;
            box(ctrlLeft(), sy + (rowH - 20.0F) / 2.0F, right, sy + (rowH + 20.0F) / 2.0F, 6.0F,
                    FIELD, focus ? ACCENT : FIELD_EDGE);
            FontRenderer fr = StarsFonts.thin(15);
            String v = focus ? "press a key" : keyName(((KeySetting) setting).getKey());
            textV(fr, v, ctrlLeft() + 7.0F, sy, rowH, focus ? ACCENT2 : TEXT_SOFT);
            return;
        }

        if (setting instanceof ColorSetting) {
            ColorSetting c = (ColorSetting) setting;
            ColorMode mode = colorModes.getOrDefault(c, ColorMode.HUE);
            float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
            float t = sy + (rowH - 12.0F) / 2.0F;
            float cl = ctrlLeft(), cw = right - cl;
            for (int i = 0; i < cw; i++) {
                float v = i / (cw - 1.0F);
                int rgb = mode == ColorMode.HUE ? Color.HSBtoRGB(v, hsb[1], hsb[2])
                        : mode == ColorMode.SATURATION ? Color.HSBtoRGB(hsb[0], v, hsb[2])
                        : Color.HSBtoRGB(hsb[0], hsb[1], v);
                Gui.drawRect((int) (cl + i), (int) t, (int) (cl + i + 1), (int) (t + 12.0F), rgb);
            }
            float marker = mode == ColorMode.HUE ? hsb[0] : mode == ColorMode.SATURATION ? hsb[1] : hsb[2];
            float mxp = cl + marker * (cw - 1.0F);
            Gui.drawRect((int) (mxp - 1.0F), (int) t - 2, (int) (mxp + 1.0F), (int) (t + 14.0F), 0xFFFFFFFF);
            if (activeSetting == c && Mouse.isButtonDown(0)) updateColor(c, mode, mx, cl, cw);
        }
    }

    private void drawSwitch(float l, float t, float anim) {
        float w = 30.0F, h = 16.0F, r = h / 2.0F;
        RenderUtils.drawRoundedRectangle(l, t, l + w, t + h, r, SWITCH_OFF);
        if (anim > 0.01F) {
            hGrad(l, t, l + w, t + h, r, withAlpha(ACCENT, anim), withAlpha(ACCENT2, anim));
        }
        float kx = l + 2.0F + anim * (w - h);
        drawCircle(kx + (h - 4.0F) / 2.0F + 0.5F, t + h / 2.0F + 0.5F, (h - 4.0F) / 2.0F, 0x33000000);
        drawCircle(kx + (h - 4.0F) / 2.0F, t + h / 2.0F, (h - 4.0F) / 2.0F, 0xFFFFFFFF);
    }

    private void drawSlider(SliderSetting s, float sy, float right, int mx, boolean hov) {
        double range = Math.max(0.00001D, s.getMax() - s.getMin());
        float pct = (float) ((s.getInput() - s.getMin()) / range);
        float cl = ctrlLeft();
        float trackY = sy + 22.0F;
        float knobX = cl + (right - cl) * pct;

        RenderUtils.drawRoundedRectangle(cl, trackY, right, trackY + 4.0F, 2.0F, TRACK);
        if (knobX - cl > 1.5F) {
            hGrad(cl, trackY, knobX, trackY + 4.0F, 2.0F, ACCENT, ACCENT2);
        }
        if (hov || activeSetting == s) {
            drawCircle(knobX, trackY + 2.0F, 6.5F, withAlpha(ACCENT, 0.28F));
        }
        String value = Utils.asWholeNum(s.getInput()) + s.getSuffix();
        FontRenderer fr = StarsFonts.thin(14);
        textV(fr, value, right - fr.stringWidth(value), sy, 20.0F, ACCENT2);
        drawCircle(knobX, trackY + 2.0F, 4.0F, 0xFFFFFFFF);
        if (activeSetting == s && Mouse.isButtonDown(0)) updateSlider(s, mx, cl, right - cl);
    }

    private void drawCombo(SliderSetting s, float sy, float right, boolean hov) {
        float cl = ctrlLeft();
        float t = sy + (settingHeight(s) - 20.0F) / 2.0F;
        box(cl, t, right, t + 20.0F, 6.0F, hov ? FIELD_HOV : FIELD, openCombo == s ? ACCENT : FIELD_EDGE);
        String[] o = s.getOptions();
        int idx = (int) clamp((float) s.getInput(), 0, o.length - 1);
        FontRenderer fr = StarsFonts.thin(15);
        textV(fr, fr.trimStringToWidth(o[idx], (int) (CTRL_W - 26.0F), true), cl + 8.0F, t, 20.0F, TEXT);
        textV(fr, "v", right - 13.0F, t, 20.0F, MUTED);
    }

    private void drawComboOptions(SliderSetting s, float sy, int mx, int my) {
        String[] o = s.getOptions();
        float right = ctrlRight(), left = right - CTRL_W;
        float rowH = 16.0F;
        float top = sy + settingHeight(s) - 4.0F, bottom = top + o.length * rowH + 4.0F;

        scissor(settingsLeft(), settingsTop(), x + totalWidth() - settingsLeft(), settingsViewport());
        RenderUtils.drawRoundedRectangle(left - 1.0F, top, right + 1.0F, bottom, 7.0F, 0x24FFFFFF);
        RenderUtils.drawRoundedRectangle(left, top + 1.0F, right, bottom - 1.0F, 6.0F, 0xF51B1D24);
        for (int i = 0; i < o.length; i++) {
            float ry = top + 2.0F + i * rowH;
            boolean hov = inside(mx, my, left, ry, CTRL_W, rowH);
            boolean cur = (int) s.getInput() == i;
            if (hov) {
                RenderUtils.drawRoundedRectangle(left + 2.0F, ry + 0.5F, right - 2.0F, ry + rowH - 0.5F,
                        4.0F, ACCENT);
            }
            FontRenderer fr = StarsFonts.thin(15);
            textV(fr, fr.trimStringToWidth(o[i], (int) (CTRL_W - 16.0F), true), left + 8.0F, ry, rowH,
                    hov ? 0xFFFFFFFF : (cur ? ACCENT2 : TEXT_SOFT));
        }
        endScissor();
    }
    // ------------------------------------------------------------------
    // input
    // ------------------------------------------------------------------

    @Override
    public void mouseClicked(int mx, int my, int button) throws IOException {
        int right = x + totalWidth();

        // The search field lives inside the toolbar drag strip, so it must be hit tested first
        // (and excluded from the strip) or a click on it would start a window drag instead.
        float sTop = searchTop();
        if (button == 0 && inside(mx, my, searchLeft(), sTop, searchRight() - searchLeft(), SEARCH_H)) {
            searching = true;
            return;
        }
        searching = false;

        if (button == 0 && inside(mx, my, x, y - 10, totalWidth(), 30)
                && !inside(mx, my, searchLeft(), sTop, searchRight() - searchLeft(), SEARCH_H)) {
            dragging = true;
            dragOffsetX = x - mx;
            dragOffsetY = y - my;
            return;
        }
        if (button == 0 && inside(mx, my, right - 12, y + windowHeight - 12, 12, 12)) {
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
            float right = ctrlRight(), left = right - CTRL_W;
            String[] o = openCombo.getOptions();
            float top = comboY + settingHeight(openCombo) - 4.0F + 2.0F;
            for (int i = 0; i < o.length; i++) {
                if (inside(mx, my, left, top + i * 15.0F, CTRL_W, 15.0F)) {
                    openCombo.setValueWithEvent(i);
                    openCombo = null;
                    return;
                }
            }
            openCombo = null;
        }
        float sy = settingsTop() + scroll;
        for (Setting s : settings) {
            float rowH = settingHeight(s);
            if (inside(mx, my, settingsLeft(), sy, x + totalWidth() - settingsLeft(), rowH)) {
                handleSettingClick(s, mx, sy, button);
                return;
            }
            sy += rowH;
        }
        if (button == 0) activeSetting = null;
    }

    private void handleSettingClick(Setting setting, int mx, float sy, int button) {
        if (openCombo != null && setting != openCombo) {
            openCombo = null;
        }
        float cl = ctrlLeft();
        float cw = ctrlRight() - cl;

        if (setting instanceof SliderSetting) {
            SliderSetting s = (SliderSetting) setting;
            if (s.isString) {
                if (button == 0) openCombo = openCombo == s ? null : s;
            } else if (button == 0) {
                activeSetting = s;
                updateSlider(s, mx, cl, cw);
            }
        } else if (setting instanceof ButtonSetting && button == 0) {
            ButtonSetting b = (ButtonSetting) setting;
            if (b.isMethodButton) b.runMethod();
            else b.toggle();
        } else if ((setting instanceof TextSetting || setting instanceof KeySetting) && button == 0) {
            activeSetting = setting;
        } else if (setting instanceof ColorSetting) {
            ColorSetting c = (ColorSetting) setting;
            if (button == 1) {
                colorModes.put(c, ColorMode.values()[(colorModes.getOrDefault(c, ColorMode.HUE).ordinal() + 1) % 3]);
            } else if (button == 0) {
                activeSetting = c;
                updateColor(c, colorModes.getOrDefault(c, ColorMode.HUE), mx, cl, cw);
            }
        } else if (setting instanceof GroupSetting && button == 0) {
            ((GroupSetting) setting).setOpened(!((GroupSetting) setting).isOpened());
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0) return;
        int mx = Mouse.getEventX() * width / mc.displayWidth;
        int my = height - Mouse.getEventY() * height / mc.displayHeight - 1;
        lastScrollTime = System.currentTimeMillis();
        float dir = wheel > 0 ? 14.0F : -14.0F;
        if (inside(mx, my, x + NAV_W, listTop(), modulePanelW(), listViewport())) {
            moduleScroll.put(selectedCategory, moduleScroll.get(selectedCategory) + dir);
        } else if (selectedModule != null
                && inside(mx, my, settingsLeft(), settingsTop(), settingsWidth, settingsViewport())) {
            settingScroll.put(selectedModule, settingScroll.getOrDefault(selectedModule, 0.0F) + dir);
        }
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
            } else if (key == Keyboard.KEY_BACK && !t.getText().isEmpty()) {
                t.setText(t.getText().substring(0, t.getText().length() - 1));
            } else if (Character.isDefined(ch) && !Character.isISOControl(ch)) {
                t.setText(t.getText() + ch);
            }
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
        float viewport = listViewport();
        float targetY = keyboardIndex * ROW_H;
        float scroll = moduleScroll.get(selectedCategory);
        if (targetY < -scroll) {
            moduleScroll.put(selectedCategory, -targetY);
        } else if (targetY + ROW_H > -scroll + viewport) {
            moduleScroll.put(selectedCategory, -(targetY + ROW_H - viewport));
        }
    }

    // ------------------------------------------------------------------
    // background, scaling, misc
    // ------------------------------------------------------------------

    /**
     * Dims the world, then frosts only the window area: 25 offset copies of the framebuffer
     * plus a strong darkening, so the colour that bleeds through the translucent material is
     * blurred and muted instead of looking like dirty glass.
     */
    private void drawBlurBackground() {
        Gui.drawRect(0, 0, width, height, OVERLAY);
        if (!OpenGlHelper.isFramebufferEnabled()) return;

        mc.getFramebuffer().bindFramebufferTexture();
        GlStateManager.pushMatrix();
        GlStateManager.disableDepth();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableLighting();
        int w = width, h = height;

        scissor(x + 3.0F, y + 3.0F, totalWidth() - 6.0F, windowHeight - 6.0F);
        float[] offsets = {-3.0F, -1.5F, 0.0F, 1.5F, 3.0F};
        for (float ox : offsets) {
            for (float oy : offsets) {
                GlStateManager.color(1.0F, 1.0F, 1.0F, 0.055F);
                drawTexturedFullscreenQuad(ox, oy, w, h);
            }
        }
        GlStateManager.color(0.0F, 0.0F, 0.0F, 0.34F);
        drawTexturedFullscreenQuad(0, 0, w, h);
        endScissor();

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

    private void drawCircle(float cx, float cy, float radius, int color) {
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        Color c = new Color(color, true);
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
            categoryFadeAlpha = Math.min(1F, categoryFadeAlpha + 0.09F);
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

    private void updateWindowDrag(int mx, int my) {
        if (dragging) {
            x = dragOffsetX + mx;
            y = dragOffsetY + my;
            constrainWindow();
        }
        if (resizing) {
            settingsWidth = Math.max(MIN_SETTINGS,
                    Math.min(resizeOffsetX + mx, Math.max(MIN_SETTINGS, width - x - NAV_W - modulePanelW())));
            windowHeight = Math.max(MIN_HEIGHT, Math.min(resizeOffsetY + my, Math.max(MIN_HEIGHT, height - y)));
        }
    }

    private void constrainWindow() {
        x = Math.max(0, Math.min(x, Math.max(0, width - totalWidth())));
        y = Math.max(10, Math.min(y, Math.max(10, height - windowHeight)));
    }

    private void updateSlider(SliderSetting s, int mx, float left, float width) {
        double frac = clamp((mx - left) / Math.max(1.0F, width), 0, 1);
        s.setValueWithEvent(s.getMin() + (s.getMax() - s.getMin()) * frac);
    }

    private void updateColor(ColorSetting c, ColorMode mode, int mx, float left, float width) {
        float v = clamp((mx - left) / Math.max(1.0F, width), 0, 1);
        if (mode == ColorMode.HUE) c.setHue(v * 360.0F);
        else if (mode == ColorMode.SATURATION) c.setSaturation(v);
        else c.setBrightness(v);
    }

    private enum ColorMode {
        HUE, SATURATION, BRIGHTNESS
    }
}