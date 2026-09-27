package starshack.module.setting.impl;

import com.google.gson.JsonObject;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import starshack.Stars;                    // ★ 新增
import starshack.helper.MouseHelper;
import starshack.module.setting.Setting;

public class KeySetting extends Setting {
    private int key;
    public GroupSetting group;

    public KeySetting(String name, int key) {
        super(name);
        this.key = key;
    }

    public KeySetting(GroupSetting group, String name, int key) {
        super(name);
        this.group = group;
        this.key = key;
    }

    public int getKey() {
        return this.key;
    }

    public String getName() {
        return super.getName();
    }

    @Override
    public String getProfileKey() {
        return group == null ? getName() : group.getName() + "." + getName();
    }

    // ★ 改动1：setKey 标脏
    public void setKey(int key) {
        this.key = key;
        markConfigDirty();
    }

    // ★ 改动2：内联标脏
    private void markConfigDirty() {
        if (Stars.currentProfile != null && Stars.currentProfile.getModule() != null) {
            Stars.currentProfile.getModule().saved = false;
        }
    }

    public boolean isPressed() {
        if (this.getKey() == 0) {
            return false;
        }
        if (this.getKey() >= 1000) {
            return (this.getKey() == 1069 || this.getKey() == 1070) ? MouseHelper.isScrollDown(this.getKey()) : Mouse.isButtonDown(this.getKey() - 1000);
        } else {
            return Keyboard.isKeyDown(this.getKey());
        }
    }

    // ★ loadProfile 原本就是直接赋值 → 不用改（天然不标脏）
    @Override
    public void loadProfile(JsonObject data) {
        String profileKey = getProfileKey();
        String legacyKey = getName();
        String key = data.has(profileKey) ? profileKey : legacyKey;
        if (data.has(key) && data.get(key).isJsonPrimitive()) {
            int keyValue = this.key;
            try {
                keyValue = data.getAsJsonPrimitive(key).getAsInt();
            } catch (Exception ignored) {
            }
            this.key = keyValue;
        }
    }
}