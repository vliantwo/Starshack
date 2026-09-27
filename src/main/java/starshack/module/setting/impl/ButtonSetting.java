package starshack.module.setting.impl;

import com.google.gson.JsonObject;
import starshack.Stars;                    // ★ 新增
import starshack.module.setting.Setting;

public class ButtonSetting extends Setting {
    private final String name;
    private boolean isEnabled;
    public boolean isMethodButton;
    private Runnable method;
    public GroupSetting group;
    private final String[] legacyProfileKeys;

    public ButtonSetting(String name, boolean isEnabled) {
        super(name);
        this.name = name;
        this.isEnabled = isEnabled;
        this.isMethodButton = false;
        this.legacyProfileKeys = new String[0];
    }

    public ButtonSetting(String name, boolean isEnabled, String... legacyProfileKeys) {
        super(name);
        this.name = name;
        this.isEnabled = isEnabled;
        this.isMethodButton = false;
        this.legacyProfileKeys = legacyProfileKeys != null ? legacyProfileKeys : new String[0];
    }

    public ButtonSetting(GroupSetting group, String name, boolean isEnabled) {
        super(name);
        this.group = group;
        this.name = name;
        this.isEnabled = isEnabled;
        this.isMethodButton = false;
        this.legacyProfileKeys = new String[0];
    }

    public ButtonSetting(GroupSetting group, String name, boolean isEnabled, String... legacyProfileKeys) {
        super(name);
        this.group = group;
        this.name = name;
        this.isEnabled = isEnabled;
        this.isMethodButton = false;
        this.legacyProfileKeys = legacyProfileKeys != null ? legacyProfileKeys : new String[0];
    }

    public ButtonSetting(String name, Runnable method) {
        super(name);
        this.name = name;
        this.isEnabled = false;
        this.isMethodButton = true;
        this.method = method;
        this.legacyProfileKeys = new String[0];
    }

    public void runMethod() {
        if (method != null) {
            method.run();
        }
    }

    public String getName() {
        return this.name;
    }

    @Override
    public String getProfileKey() {
        return group == null ? getName() : group.getName() + "." + getName();
    }

    public boolean isToggled() {
        return this.isEnabled;
    }

    // ★ 改动1：toggle 末尾标脏
    public void toggle() {
        this.isEnabled = !this.isEnabled;
        markConfigDirty();
    }

    // ★ 改动2：enable 末尾标脏
    public void enable() {
        this.isEnabled = true;
        markConfigDirty();
    }

    // ★ 改动3：disable 末尾标脏
    public void disable() {
        this.isEnabled = false;
        markConfigDirty();
    }

    // ★ 改动4：setEnabled 末尾标脏
    public void setEnabled(boolean b) {
        this.isEnabled = b;
        markConfigDirty();
    }

    // ★ 改动5：内联标脏
    private void markConfigDirty() {
        if (Stars.currentProfile != null && Stars.currentProfile.getModule() != null) {
            Stars.currentProfile.getModule().saved = false;
        }
    }

    // ★ 改动6：loadProfile 直接赋值，不标脏（加载 ≠ 修改）
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
        if (key != null && data.get(key).isJsonPrimitive() && !this.isMethodButton) {
            boolean booleanValue = isEnabled;
            try {
                booleanValue = data.getAsJsonPrimitive(key).getAsBoolean();
            } catch (Exception e) {
            }
            this.isEnabled = booleanValue;   // ★ 直接赋值，不标脏
        }
    }
}