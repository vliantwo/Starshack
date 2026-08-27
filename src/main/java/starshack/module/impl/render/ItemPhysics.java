package starshack.module.impl.render;

import starshack.module.Module;
import starshack.module.setting.impl.SliderSetting;

public class ItemPhysics extends Module {
    public static ItemPhysics instance;

    public final SliderSetting rotationSpeed;

    public ItemPhysics() {
        super("Item Physics", category.visuals, 0);
        this.registerSetting(rotationSpeed = new SliderSetting("Rotation speed", 1.0, 0.0, 5.0, 0.1));
    }

    @Override
    public void onEnable() {
        instance = this;
    }

    @Override
    public void onDisable() {
        instance = null;
    }
}
