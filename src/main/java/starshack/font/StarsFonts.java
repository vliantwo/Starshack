package starshack.font;

import starshack.font.api.FontManager;
import starshack.font.api.FontRenderer;
import starshack.font.api.FontType;
import starshack.font.impl.SimpleFontManager;

/**
 * Bundled font atlas renderer and typefaces.
 */
public final class StarsFonts {
    private static final FontManager FONT_MANAGER = SimpleFontManager.create();

    private StarsFonts() {
    }

    public static FontRenderer sf(int size) {
        return FONT_MANAGER.font(FontType.SF, size);
    }

    public static FontRenderer thin(int size) {
        return FONT_MANAGER.font(FontType.SFTHIN, size);
    }

    public static FontRenderer bold(int size) {
        return FONT_MANAGER.font(FontType.SFBOLD, size);
    }

    public static FontRenderer icons(int size) {
        return FONT_MANAGER.font(FontType.ICONFONT, size);
    }

    public static FontRenderer oxide(int size) {
        return FONT_MANAGER.font(FontType.OXIDE, size);
    }
}
