package starshack.module.impl.combat.autoclicker;

import starshack.module.Module;
import starshack.module.setting.impl.ButtonSetting;
import starshack.module.setting.impl.SliderSetting;

/**
 * Vape V4 风格的 AutoClicker 配置。
 * <p>
 * 关键点：项目没有 ModeSetting，所以 "Randomization / Trigger mode / Item mode / Jitter"
 * 这类枚举选项一律用 SliderSetting(int, String[]) 的【选项模式】模拟：
 * new SliderSetting("Name", 默认索引, new String[]{"A","B","C"})
 * 拖动时取整数 0/1/2...，对应 VapeEnums / RandomizationMode 常量。
 * <p>
 * ⚠️ 重要：凡是"选项模式"的字段，声明类型必须是 SliderSetting，不能是 ButtonSetting。
 */
public class VapeAutoClickerConfig {

    // ---- 基础 ----
    public SliderSetting targetCPS;        // 目标 CPS（连续数值）
    public SliderSetting randomization;    // Randomization：0=Normal, 1=Extra, 2=Extra+

    // ---- 高级随机化（Extra+）----
    public ButtonSetting fatigue;          // 疲劳：越点越慢
    public ButtonSetting drift;            // 漂移：长期均值偏移
    public ButtonSetting doubleClick;      // 偶尔双击

    // ---- Vape 风格情景控制 ----
    public SliderSetting triggerMode;      // 0=Always, 1=Hover(对着实体), 2=Weapon(手持武器)
    public ButtonSetting limitToItem;      // 限制手持物品
    public SliderSetting itemMode;         // 0=Sword, 1=Any(拿东西就点)

    // ---- Break Blocks（沿用 Novoline 的反射破块，增强版）----
    public ButtonSetting breakBlocks;
    public SliderSetting breakDelay;       // 破块切换延迟(ms)
    public ButtonSetting breakOnlyWithTool;// 只在拿镐/铲时暂停破块

    // ---- 其他兼容项 ----
    public ButtonSetting simulateExhaust;  // 模拟饥饿消耗
    public ButtonSetting notUsingItem;    // 用物品时不点
    public ButtonSetting disableCreative; // 创造模式不点
    public SliderSetting jitter;          // ★ 修复：Jitter 用选项模式 → 必须是 SliderSetting（0=Off,1=Low,2=High）

    // ---- Vape 社区共识：CPS 差值校验 ----
    public SliderSetting safeRange;        // 推荐的 CPS 区间(用于 getInfo 提示)

    public VapeAutoClickerConfig(Module module) {
        // targetCPS：连续数值滑块，1~20，默认 10
        this.targetCPS = new SliderSetting("Target CPS", 10.0, 1.0, 20.0, 0.5);
        module.registerSetting(targetCPS);

        // Randomization：选项模式（int 索引 + String[]）
        this.randomization = new SliderSetting("Randomization",
                RandomizationMode.NORMAL, RandomizationMode.NAMES);
        module.registerSetting(randomization);

        this.fatigue = new ButtonSetting("Fatigue", false);
        module.registerSetting(fatigue);
        this.drift = new ButtonSetting("Drift", true);
        module.registerSetting(drift);
        this.doubleClick = new ButtonSetting("Double click", false);
        module.registerSetting(doubleClick);

        this.triggerMode = new SliderSetting("Trigger mode",
                VapeEnums.TriggerMode.ALWAYS, VapeEnums.TriggerMode.NAMES);
        module.registerSetting(triggerMode);
        this.limitToItem = new ButtonSetting("Limit to item", false);
        module.registerSetting(limitToItem);
        this.itemMode = new SliderSetting("Item mode", VapeEnums.ItemMode.SWORD, VapeEnums.ItemMode.NAMES);
        module.registerSetting(itemMode);

        this.breakBlocks = new ButtonSetting("Break blocks", false);
        module.registerSetting(breakBlocks);
        this.breakDelay = new SliderSetting("Break delay", "ms", 50.0, 0.0, 200.0, 10.0);
        module.registerSetting(breakDelay);
        this.breakOnlyWithTool = new ButtonSetting("Only with tool", false);
        module.registerSetting(breakOnlyWithTool);

        this.simulateExhaust = new ButtonSetting("Simulate exhaust", true);
        module.registerSetting(simulateExhaust);
        this.notUsingItem = new ButtonSetting("Not using item", false);
        module.registerSetting(notUsingItem);
        this.disableCreative = new ButtonSetting("Disable in creative", false);
        module.registerSetting(disableCreative);

        // ★ Jitter：选项模式（Off/Low/High）→ SliderSetting，字段类型也必须是 SliderSetting
        this.jitter = new SliderSetting("Jitter", VapeEnums.Jitter.OFF, VapeEnums.Jitter.NAMES);
        module.registerSetting(jitter);

        this.safeRange = new SliderSetting("Safe range", 4.0, 2.0, 8.0, 1.0);
        module.registerSetting(safeRange);
    }

    // ================= 取值辅助（供主模块调用）=================

    public int getRandomization() {
        return (int) Math.round(randomization.getInput());   // 0/1/2
    }

    public int getTriggerMode() {
        return (int) Math.round(triggerMode.getInput());
    }

    /**
     * ★ 修复：jitter 现在是 SliderSetting，有 getInput()（返回 double），强转 int 即得 0/1/2
     */
    public int getJitter() {
        return (int) Math.round(jitter.getInput());           // 0=Off, 1=Low, 2=High
    }

    /**
     * 便捷：是否启用抖动（非 Off 即为启用）
     */
    public boolean isJitterEnabled() {
        return getJitter() > 0;
    }

    public double getCPS() {
        return targetCPS.getInput();
    }

    public boolean isTriggerHover() {
        return getTriggerMode() == VapeEnums.TriggerMode.HOVER;
    }

    public boolean isTriggerWeapon() {
        return getTriggerMode() == VapeEnums.TriggerMode.WEAPON;
    }

    /**
     * Vape 社区共识：CPS 差值 = max - min，差值 >= 4 更自然
     */
    public double getCPSDiff() {
        return safeRange.getInput();  // 借用 safeRange 显示推荐差值
    }
}
