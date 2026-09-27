package starshack.module.impl.world;

import starshack.module.Module;
import starshack.module.setting.impl.ButtonSetting;
import starshack.module.setting.impl.SliderSetting;

public class Weather extends Module {
    public SliderSetting time;
    public SliderSetting lightning;
    public ButtonSetting rain;

    public Weather() {
        super("Weather", category.visuals);
        this.registerSetting(time = new SliderSetting("Time", 0, 0, 24, 0.1));
        this.registerSetting(lightning = new SliderSetting("Lightning", 0, 0, 1, 0.01));
        this.registerSetting(rain = new ButtonSetting("Rain", false));
    }
}
