package starshack.module.impl.combat.autoclicker;

/**
 * Vape V4 内部枚举：Jitter / TriggerMode / ItemMode。
 * 都对应 SliderSetting 的 int 选项（0/1/2...），不依赖 ModeSetting。
 * <p>
 * ★ 常量用大写（OFF/LOW/HIGH...）以匹配 Config 里 VapeEnums.Jitter.OFF 的写法。
 */
public final class VapeEnums {

    private VapeEnums() {
    }

    // ===== Jitter =====
    public static final class Jitter {
        public static final int OFF = 0;
        public static final int LOW = 1;
        public static final int HIGH = 2;
        public static final String[] NAMES = {"Off", "Low", "High"};
    }

    // ===== Trigger Mode =====
    public static final class TriggerMode {
        public static final int ALWAYS = 0;  // 按住就点
        public static final int HOVER = 1;  // 准星对着实体才点
        public static final int WEAPON = 2;  // 手持武器才点
        public static final String[] NAMES = {"Always", "Hover", "Weapon"};
    }

    // ===== Item Mode（Limit to Item 用）=====
    public static final class ItemMode {
        public static final int SWORD = 0;    // 只有拿剑
        public static final int ANY = 1;      // 拿任何东西
        public static final String[] NAMES = {"Sword", "Any"};
    }
}
