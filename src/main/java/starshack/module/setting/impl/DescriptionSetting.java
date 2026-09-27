package starshack.module.setting.impl;

import com.google.gson.JsonObject;
import starshack.module.setting.Setting;

public class DescriptionSetting extends Setting {
    private String desc;

    public DescriptionSetting(String t) {
        super(t);
        this.desc = t;
    }

    public String getDesc() {
        return this.desc;
    }

    @Override
    public void loadProfile(JsonObject data) {
    }
}
