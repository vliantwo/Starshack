package starshack.module.impl.render;

import starshack.module.Module;
import starshack.module.setting.impl.DescriptionSetting;
import starshack.module.setting.impl.SliderSetting;

public class NoHurtCam extends Module {
    public SliderSetting multiplier;

    public NoHurtCam() {
        super("NoHurtCam", category.visuals);
        this.registerSetting(new DescriptionSetting("Default is 14x multiplier."));
        this.registerSetting(multiplier = new SliderSetting("Multiplier", 14, -40, 40, 1));
    }
}
