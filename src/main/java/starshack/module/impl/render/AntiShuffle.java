package starshack.module.impl.render;

import starshack.module.Module;
import starshack.module.setting.impl.DescriptionSetting;

public class AntiShuffle extends Module {
    private static String shuffleStr = "§k";

    public AntiShuffle() {
        super("Anti Shuffle", Module.category.visuals, 0);
        this.registerSetting(new DescriptionSetting("Removes obfuscation (" + shuffleStr + "hey" + "§" + "r)."));
    }

    public static String removeObfuscation(String s) {
        return s.replace(shuffleStr, "");
    }
}
