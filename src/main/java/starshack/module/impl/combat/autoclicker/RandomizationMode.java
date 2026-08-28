package starshack.module.impl.combat.autoclicker;

/**
 * Vape V4 风格的 Randomization 档位。
 * 不依赖 ModeSetting：用普通 int 常量，配合 SliderSetting 的选项模式使用。
 * <p>
 * 0 = Normal  (均匀随机，基础)
 * 1 = Extra   (突发 + 停顿，接近 Novoline-bS 多层随机)
 * 2 = Extra+  (高斯 + 疲劳 + 漂移 + 爆发，最像真人)
 */
public final class RandomizationMode {

    public static final int NORMAL = 0;
    public static final int EXTRA = 1;
    public static final int EXTRA_PLUS = 2;
    public static final String[] NAMES = {"Normal", "Extra", "Extra+"};

    private RandomizationMode() {
    }

    public static String nameOf(int mode) {
        if (mode < 0 || mode >= NAMES.length) return NAMES[NORMAL];
        return NAMES[mode];
    }
}
