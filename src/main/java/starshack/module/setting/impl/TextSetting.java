package starshack.module.setting.impl;

import com.google.gson.JsonObject;
import starshack.Stars;                    // ★ 新增
import starshack.module.setting.Setting;

public class TextSetting extends Setting {
    private final String placeholder;
    private final int maxLength;
    private final Runnable onSubmit;
    private String text;
    public GroupSetting group;

    public TextSetting(String name, String text, String placeholder, int maxLength) {
        this(name, text, placeholder, maxLength, null);
    }

    // ★ 改动1：构造器直接赋值（不走 setText），避免"构造时误标脏"
    public TextSetting(String name, String text, String placeholder, int maxLength, Runnable onSubmit) {
        super(name);
        this.placeholder = placeholder == null ? "" : placeholder;
        this.maxLength = Math.max(1, maxLength);
        this.onSubmit = onSubmit;
        String next = text == null ? "" : text;
        if (next.length() > maxLength) {
            next = next.substring(0, maxLength);
        }
        this.text = next;   // ★ 直接赋值，不标脏
    }

    public TextSetting(GroupSetting group, String name, String text, String placeholder, int maxLength) {
        this(group, name, text, placeholder, maxLength, null);
    }

    public TextSetting(GroupSetting group, String name, String text, String placeholder, int maxLength, Runnable onSubmit) {
        this(name, text, placeholder, maxLength, onSubmit);
        this.group = group;
    }

    public String getText() {
        return text;
    }

    // ★ 改动2：setText 标脏（GUI 改文本走这里）
    public void setText(String text) {
        String next = text == null ? "" : text;
        if (next.length() > maxLength) {
            next = next.substring(0, maxLength);
        }
        this.text = next;
        markConfigDirty();
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public int getMaxLength() {
        return maxLength;
    }

    @Override
    public String getProfileKey() {
        return group == null ? getName() : group.getName() + "." + getName();
    }

    public void submit() {
        if (onSubmit != null) {
            onSubmit.run();
        }
    }

    // ★ 改动3：内联标脏
    private void markConfigDirty() {
        if (Stars.currentProfile != null && Stars.currentProfile.getModule() != null) {
            Stars.currentProfile.getModule().saved = false;
        }
    }

    @Override
    public void loadProfile(JsonObject data) {
    }
}