package starshack.module.impl.movement;

import starshack.module.Module;
import starshack.module.setting.impl.DescriptionSetting;

public class MovementFix extends Module {

    public MovementFix() {
        super("Movement Fix", category.movement);
        this.registerSetting(new DescriptionSetting("Aligns input with rotations"));
    }
}
