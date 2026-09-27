package starshack.module.impl.render;

import starshack.mixin.impl.accessor.IAccessorEntityRenderer;
import starshack.module.Module;
import starshack.module.setting.impl.DescriptionSetting;
import starshack.module.setting.impl.SliderSetting;
import starshack.utility.Utils;

public class ExtendCamera extends Module {
    public SliderSetting distance;

    private float lastDistance;

    public ExtendCamera() {
        super("ExtendCamera", category.visuals);
        this.registerSetting(new DescriptionSetting("Extends camera in third person."));
        this.registerSetting(new DescriptionSetting("Default is 4 blocks."));
        this.registerSetting(distance = new SliderSetting("Distance", " block", 4, 1, 40, 0.5));
    }

    @Override
    public void onEnable() {
        setThirdPersonDistance((float) distance.getInput());
    }

    @Override
    public void onUpdate() {
        try {
            float input = (float) distance.getInput();
            if (lastDistance != input) {
                setThirdPersonDistance(lastDistance = input);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendMessage("&cThere was an issue setting third person distance.");
        }
    }

    @Override
    public void onDisable() {
        setThirdPersonDistance(4.0f);
    }

    private void setThirdPersonDistance(float distance) {
        ((IAccessorEntityRenderer) mc.entityRenderer).setThirdPersonDistance(distance);
    }
}
