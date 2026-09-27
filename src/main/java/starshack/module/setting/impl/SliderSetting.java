package starshack.module.setting.impl;

import com.google.gson.JsonObject;
import net.minecraftforge.common.MinecraftForge;
import starshack.Stars;                    // ★ 新增（若主类路径不同请改这行）
import starshack.event.PostSetSliderEvent;
import starshack.module.setting.Setting;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class SliderSetting extends Setting {
    private final String settingName;
    private String[] options = null;
    private double defaultValue;
    private final double max;
    private final double min;
    private final double intervals;
    public boolean isString;
    private String suffix = "";
    public boolean canBeDisabled;
    public GroupSetting groupSetting;
    private String[] legacyProfileKeys;

    public SliderSetting(GroupSetting groupSetting, String settingName, double defaultValue, double min, double max, double intervals) {
        super(settingName);
        this.groupSetting = groupSetting;
        this.settingName = settingName;
        this.defaultValue = defaultValue;
        this.min = min;
        this.max = max;
        this.intervals = intervals;
        this.isString = false;
        this.legacyProfileKeys = new String[0];
    }

    public SliderSetting(String settingName, double defaultValue, double min, double max, double intervals) {
        this((GroupSetting) null, settingName, defaultValue, min, max, intervals);
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, String suffix, double defaultValue, double min, double max, double intervals) {
        this(groupSetting, settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
    }

    public SliderSetting(String settingName, String suffix, double defaultValue, double min, double max, double intervals) {
        this((GroupSetting) null, settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
    }

    public SliderSetting(String settingName, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(settingName, defaultValue, min, max, intervals);
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(GroupSetting group, String settingName, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(group, settingName, defaultValue, min, max, intervals);
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(String settingName, String suffix, boolean canBeDisabled, double defaultValue, double min, double max, double intervals) {
        this(settingName, defaultValue, min, max, intervals);
        this.suffix = suffix;
        this.canBeDisabled = canBeDisabled;
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, int defaultValue, String[] options) {
        super(settingName);
        this.groupSetting = groupSetting;
        this.settingName = settingName;
        this.options = options;
        this.defaultValue = defaultValue;
        this.min = 0;
        this.max = options.length - 1;
        this.intervals = 1;
        this.isString = true;
        this.legacyProfileKeys = new String[0];
    }

    public SliderSetting(String settingName, int defaultValue, String[] options) {
        this((GroupSetting) null, settingName, defaultValue, options);
    }

    public SliderSetting(String settingName, int defaultValue, String[] options, String... legacyProfileKeys) {
        this((GroupSetting) null, settingName, defaultValue, options);
        this.legacyProfileKeys = legacyProfileKeys != null ? legacyProfileKeys : new String[0];
    }

    public SliderSetting(String settingName, String suffix, int defaultValue, String[] options) {
        this((GroupSetting) null, settingName, defaultValue, options);
        this.suffix = suffix;
    }

    public SliderSetting(GroupSetting groupSetting, String settingName, String suffix, int defaultValue, String[] options) {
        this(groupSetting, settingName, defaultValue, options);
        this.suffix = suffix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public String[] getOptions() {
        return options;
    }

    public String getName() {
        return this.settingName;
    }

    @Override
    public String getProfileKey() {
        return groupSetting == null ? getName() : groupSetting.getName() + "." + getName();
    }

    public double getInput() {
        return roundToInterval(this.defaultValue, 4);
    }

    public double getMin() {
        return this.min;
    }

    public double getMax() {
        return this.max;
    }

    // ★ 改动1：setValue 末尾标脏（GUI 拖 slider / 代码改值都走这里）
    public double setValue(double newValue) {
        newValue = correctValue(newValue, this.min, this.max);
        newValue = (double) Math.round(newValue * (1.0D / this.intervals)) / (1.0D / this.intervals);
        this.defaultValue = newValue;
        markConfigDirty();
        return newValue;
    }

    // ★ 不用改：内部已调 setValue，自动标脏
    public void setValueWithEvent(double newValue) {
        double prev = this.defaultValue;
        MinecraftForge.EVENT_BUS.post(new PostSetSliderEvent(prev, this.setValue(newValue)));
    }

    // ★ 不用改：raw 路径给加载用，不标脏
    public void setValueRaw(double n) {
        this.defaultValue = n;
    }

    // ★ 改动2：带事件的 raw 设值（用户交互）也标脏
    public void setValueRawWithEvent(double n) {
        double prev = this.defaultValue;
        this.defaultValue = n;
        MinecraftForge.EVENT_BUS.post(new PostSetSliderEvent(prev, n));
        markConfigDirty();
    }

    // ★ 改动3：新增内联标脏（不依赖继承，直接访问 Stars）
    private void markConfigDirty() {
        if (Stars.currentProfile != null && Stars.currentProfile.getModule() != null) {
            Stars.currentProfile.getModule().saved = false;
        }
    }

    public static double correctValue(double v, double i, double a) {
        v = Math.max(i, v);
        v = Math.min(a, v);
        return v;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public static double roundToInterval(double v, int p) {
        if (p < 0) {
            return 0.0D;
        } else {
            BigDecimal bd = new BigDecimal(v);
            bd = bd.setScale(p, RoundingMode.HALF_UP);
            return bd.doubleValue();
        }
    }

    // ★ 改动4：loadProfile 直接赋值（同样校正），不标脏（加载 ≠ 修改）
    @Override
    public void loadProfile(JsonObject data) {
        String profileKey = getProfileKey();
        String legacyKey = getName();
        String key = null;
        if (data.has(profileKey)) {
            key = profileKey;
        } else if (data.has(legacyKey)) {
            key = legacyKey;
        } else {
            for (String legacyProfileKey : legacyProfileKeys) {
                if (data.has(legacyProfileKey)) {
                    key = legacyProfileKey;
                    break;
                }
            }
        }
        if (key != null && data.has(key) && data.get(key).isJsonPrimitive()) {
            double newValue = defaultValue;
            try {
                newValue = data.getAsJsonPrimitive(key).getAsDouble();
            } catch (Exception e) {

            }
            if (newValue == -1) {
                this.defaultValue = newValue;   // ★ 直接赋值，不标脏
                return;
            }
            // ★ 与 setValue 相同校正逻辑，但【不标脏】
            newValue = correctValue(newValue, this.min, this.max);
            newValue = (double) Math.round(newValue * (1.0D / this.intervals)) / (1.0D / this.intervals);
            this.defaultValue = newValue;
        }
    }
}