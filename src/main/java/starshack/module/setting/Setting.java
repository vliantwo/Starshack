package starshack.module.setting;

import com.google.gson.JsonObject;
import starshack.Stars;
import starshack.clickgui.ClickGui;
import starshack.clickgui.components.impl.CategoryComponent;
import starshack.clickgui.components.impl.ModuleComponent;
import starshack.module.Module;

public abstract class Setting {
    public String name;
    public boolean visible = true;

    public Setting(String name) {
        this.name = name;
    }

    public void setVisible(boolean visible, Module module) {
        if (visible == this.visible) {
            return;
        }
        this.visible = visible;
        for (CategoryComponent categoryComponent : ClickGui.categories) {
            if (categoryComponent.category == module.moduleCategory()) {
                for (ModuleComponent moduleComponent : categoryComponent.modules) {
                    if (moduleComponent.mod.getName().equals(module.getName())) {
                        moduleComponent.reloadSettings();
                        break;
                    }
                }
            }
        }
    }

    public String getName() {
        return this.name;
    }

    public String getProfileKey() {
        return this.name;
    }

    public abstract void loadProfile(JsonObject data);
}