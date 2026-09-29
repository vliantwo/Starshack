package starshack.module;

import starshack.Stars;
import starshack.helper.MouseHelper;
import starshack.module.impl.render.NotificationManager;
import starshack.module.impl.render.ToggleSoundManager;
import starshack.module.setting.Setting;
import starshack.module.setting.impl.ButtonSetting;
import starshack.module.setting.impl.SliderSetting;
import starshack.script.Script;
import starshack.utility.Utils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class Module {
    protected ArrayList<Setting> settings;
    private String moduleName;
    private Module.category moduleCategory;
    private volatile boolean enabled;
    private int keycode;
    protected static Minecraft mc;
    private boolean isToggled = false;
    public boolean canBeEnabled = true;
    public boolean ignoreOnSave = false;
    public boolean hidden = false;
    public Script script = null;
    public boolean closetModule = false;
    public boolean alwaysOn = false;
    public String lastInfo;
    public static boolean sort; // global boolean in charge of sorting upon info change
    public static List<String> categoriesString = new ArrayList<>();

    static { // loads the categories
        for (category cat : category.values()) {
            categoriesString.add(cat.name());
        }
    }

    // 常用模块的搜索别名，按类名（小写无空格）索引，方便在 ClickGUI 里用缩写搜到模块
    private static final Map<String, String[]> DEFAULT_ALIASES = new HashMap<>();

    static {
        // combat
        DEFAULT_ALIASES.put("killaura", new String[]{"ka", "killeraura"});
        DEFAULT_ALIASES.put("newautoclicker", new String[]{"autoclicker", "nac", "cps"});
        DEFAULT_ALIASES.put("autoclicker", new String[]{"ac"});
        DEFAULT_ALIASES.put("velocity", new String[]{"velo"});
        DEFAULT_ALIASES.put("backtrack", new String[]{"bt"});
        DEFAULT_ALIASES.put("hitbox", new String[]{"boxes", "hitboxes"});
        DEFAULT_ALIASES.put("knockbackdelay", new String[]{"kbdelay", "kb"});
        DEFAULT_ALIASES.put("aimassist", new String[]{"aimbot", "aim"});
        DEFAULT_ALIASES.put("rodaimbot", new String[]{"rod", "rob"});
        DEFAULT_ALIASES.put("clickassist", new String[]{"ca"});
        DEFAULT_ALIASES.put("tpaura", new String[]{"tpa"});
        DEFAULT_ALIASES.put("starautoclicker", new String[]{"starclicker", "click"});
        // movement
        DEFAULT_ALIASES.put("sprint", new String[]{"run"});
        DEFAULT_ALIASES.put("bhop", new String[]{"bunnyhop"});
        DEFAULT_ALIASES.put("fly", new String[]{"flight"});
        DEFAULT_ALIASES.put("noslow", new String[]{"noslowdown"});
        DEFAULT_ALIASES.put("longjump", new String[]{"lj"});
        DEFAULT_ALIASES.put("teleport", new String[]{"tp"});
        DEFAULT_ALIASES.put("vclip", new String[]{"clip", "noclip"});
        DEFAULT_ALIASES.put("invmove", new String[]{"inv", "inventory"});
        DEFAULT_ALIASES.put("keepsprint", new String[]{"keep"});
        DEFAULT_ALIASES.put("timer", new String[]{"tickrate"});
        // player
        DEFAULT_ALIASES.put("scaffold", new String[]{"scaff", "tower"});
        DEFAULT_ALIASES.put("nofall", new String[]{"fall"});
        DEFAULT_ALIASES.put("freecam", new String[]{"fc", "camera"});
        DEFAULT_ALIASES.put("fastmine", new String[]{"instamine"});
        DEFAULT_ALIASES.put("fastplace", new String[]{"place", "build"});
        DEFAULT_ALIASES.put("safewalk", new String[]{"shift", "edge"});
        DEFAULT_ALIASES.put("waterbucket", new String[]{"bucket", "mlg"});
        DEFAULT_ALIASES.put("ghosthand", new String[]{"interact"});
        DEFAULT_ALIASES.put("autotool", new String[]{"tool"});
        DEFAULT_ALIASES.put("autoswap", new String[]{"swap"});
        DEFAULT_ALIASES.put("antiafk", new String[]{"afk"});
        DEFAULT_ALIASES.put("fakelag", new String[]{"lag"});
        DEFAULT_ALIASES.put("invmanager", new String[]{"inv", "chest"});
        DEFAULT_ALIASES.put("bridgeassist", new String[]{"bridge"});
        DEFAULT_ALIASES.put("bedaura", new String[]{"bed"});
        DEFAULT_ALIASES.put("clutch", new String[]{"waterclutch"});
        // render / visuals
        DEFAULT_ALIASES.put("playeresp", new String[]{"esp", "player"});
        DEFAULT_ALIASES.put("itemesp", new String[]{"esp", "items"});
        DEFAULT_ALIASES.put("chestesp", new String[]{"esp", "chest"});
        DEFAULT_ALIASES.put("blockesp", new String[]{"esp", "blocks"});
        DEFAULT_ALIASES.put("bedesp", new String[]{"esp", "bed"});
        DEFAULT_ALIASES.put("mobesp", new String[]{"esp", "mobs"});
        DEFAULT_ALIASES.put("nametags", new String[]{"tags", "nametag"});
        DEFAULT_ALIASES.put("tracers", new String[]{"traces", "lines"});
        DEFAULT_ALIASES.put("chams", new String[]{"wallhack", "seegeometry"});
        DEFAULT_ALIASES.put("xray", new String[]{"see"});
        DEFAULT_ALIASES.put("nohurtcam", new String[]{"nohurt"});
        DEFAULT_ALIASES.put("indicators", new String[]{"indic", "hp"});
        DEFAULT_ALIASES.put("damagetags", new String[]{"dmgtags", "dmg"});
        DEFAULT_ALIASES.put("breakprogress", new String[]{"brkprogress", "break"});
        DEFAULT_ALIASES.put("hitparticles", new String[]{"particles", "hitfx"});
        DEFAULT_ALIASES.put("tnntimer", new String[]{"tnt", "countdown"});
        DEFAULT_ALIASES.put("trajectories", new String[]{"traj", "path"});
        DEFAULT_ALIASES.put("itemphysics", new String[]{"physics"});
        DEFAULT_ALIASES.put("saturation", new String[]{"sat"});
        DEFAULT_ALIASES.put("antidebuff", new String[]{"debuff", "nobuff"});
        DEFAULT_ALIASES.put("antishuffle", new String[]{"shuffle"});
        DEFAULT_ALIASES.put("arrows", new String[]{"arrow"});
        DEFAULT_ALIASES.put("extendcamera", new String[]{"extcam", "camera"});
        DEFAULT_ALIASES.put("fallview", new String[]{"fall", "freecam"});
        DEFAULT_ALIASES.put("freelook", new String[]{"fl", "freecam"});
        DEFAULT_ALIASES.put("holdlook", new String[]{"hold"});
        DEFAULT_ALIASES.put("blockoverlay", new String[]{"overlay"});
        DEFAULT_ALIASES.put("hudedit", new String[]{"hudedit", "editor"});
        // world
        DEFAULT_ALIASES.put("antibot", new String[]{"botfilter", "nobot"});
        DEFAULT_ALIASES.put("weather", new String[]{"rain", "clear"});
        // fun
        DEFAULT_ALIASES.put("extrabobbing", new String[]{"bobbing", "headbob"});
        DEFAULT_ALIASES.put("flametrail", new String[]{"flame", "trail"});
        DEFAULT_ALIASES.put("slyport", new String[]{"elytra"});
        DEFAULT_ALIASES.put("spin", new String[]{"spinner"});
        // other
        DEFAULT_ALIASES.put("chatbypass", new String[]{"bypass", "chat"});
        DEFAULT_ALIASES.put("disabler", new String[]{"dis"});
        DEFAULT_ALIASES.put("anticheat", new String[]{"ac", "hyt", "hypixel"});
        DEFAULT_ALIASES.put("fakechat", new String[]{"fakemsg"});
        DEFAULT_ALIASES.put("latencyalerts", new String[]{"ping", "latency"});
        DEFAULT_ALIASES.put("namehider", new String[]{"hide", "name"});
        DEFAULT_ALIASES.put("viewpackets", new String[]{"packets"});
        // minigames
        DEFAULT_ALIASES.put("autorequeue", new String[]{"aq", "requeue"});
        DEFAULT_ALIASES.put("bedwars", new String[]{"bw"});
        DEFAULT_ALIASES.put("bridgeinfo", new String[]{"bridge"});
        DEFAULT_ALIASES.put("duelsstats", new String[]{"duels"});
        DEFAULT_ALIASES.put("murdermystery", new String[]{"mm", "murder"});
        DEFAULT_ALIASES.put("skywars", new String[]{"sw"});
        DEFAULT_ALIASES.put("speedbuilders", new String[]{"sb"});
        DEFAULT_ALIASES.put("sumofences", new String[]{"sumo"});
        DEFAULT_ALIASES.put("woolwars", new String[]{"ww"});
        DEFAULT_ALIASES.put("autowho", new String[]{"who", "ah"});
    }

    /** 该模块用于搜索匹配的别名；同时匹配显示名与这些别名。 */
    public String[] getAliases() {
        return DEFAULT_ALIASES.get(getClass().getSimpleName().toLowerCase(Locale.ROOT));
    }

    public boolean matchesSearch(String query) {
        if (getName().toLowerCase(Locale.ROOT).contains(query)) {
            return true;
        }
        String[] aliases = getAliases();
        if (aliases != null) {
            for (String alias : aliases) {
                if (alias.contains(query)) {
                    return true;
                }
            }
        }
        return searchAcronym().startsWith(query);
    }

    /** 显示名每个词的首字母缩写（空格分隔 + 驼峰边界），如 "New Auto Clicker" -> "nac"。 */
    private String searchAcronym() {
        String name = getName();
        StringBuilder sb = new StringBuilder();
        char prev = ' ';
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (Character.isLetterOrDigit(c)) {
                boolean camelStart = Character.isLowerCase(prev) && Character.isUpperCase(c);
                if (i == 0 || !Character.isLetterOrDigit(prev) || camelStart) {
                    sb.append(Character.toLowerCase(c));
                }
            }
            prev = c;
        }
        return sb.toString();
    }

    public Module(String moduleName, Module.category moduleCategory, int keycode) {
        this.moduleName = moduleName;
        this.moduleCategory = moduleCategory;
        this.keycode = keycode;
        this.enabled = false;
        mc = Minecraft.getMinecraft();
        this.settings = new ArrayList();
    }

    public static Module getModule(Class<? extends Module> a) {
        return ModuleManager.getModule(a);
    }

    public Module(String name, Module.category moduleCategory) {
        this.moduleName = name;
        this.moduleCategory = moduleCategory;
        this.keycode = 0;
        this.enabled = false;
        mc = Minecraft.getMinecraft();
        this.settings = new ArrayList();
    }

    public Module(Script script) {
        super();
        this.enabled = false;
        this.moduleName = script.name;
        this.script = script;
        this.keycode = 0;
        this.moduleCategory = category.scripts;
        this.settings = new ArrayList<>();
    }

    public void onKeyBind() {
        if (this.keycode != 0) {
            try {
                if (!this.isToggled && (this.keycode >= 1000 ? ((this.keycode == 1069 || this.keycode == 1070) ? MouseHelper.isScrollDown(this.keycode) : Mouse.isButtonDown(this.keycode - 1000)) : Keyboard.isKeyDown(this.keycode))) {
                    this.toggle();
                    this.isToggled = true;
                } else if ((this.keycode >= 1000 ? ((this.keycode == 1069 || this.keycode == 1070) ? !MouseHelper.isScrollDown(this.keycode) : !Mouse.isButtonDown(this.keycode - 1000)) : !Keyboard.isKeyDown(this.keycode))) {
                    this.isToggled = false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                Utils.sendMessage("&cFailed to check keybinding. Setting to none");
                this.keycode = 0;
            }
        }
    }

    public void syncKeyBindState() {
        if (this.keycode == 0) {
            return;
        }

        try {
            this.isToggled = this.keycode >= 1000
                    ? ((this.keycode == 1069 || this.keycode == 1070) ? MouseHelper.isScrollDown(this.keycode) : Mouse.isButtonDown(this.keycode - 1000))
                    : Keyboard.isKeyDown(this.keycode);
        } catch (Exception e) {
            e.printStackTrace();
            Utils.sendMessage("&cFailed to check keybinding. Setting to none");
            this.keycode = 0;
            this.isToggled = false;
        }
    }

    public boolean canBeEnabled() {
        if (this.script != null && script.error) {
            return false;
        }
        return this.canBeEnabled;
    }

    public boolean isHidden() {
        return hidden;
    }

    public void setHidden(boolean hidden) {
        this.hidden = hidden;
    }

    public void enable() {
        if (!this.canBeEnabled() || this.isEnabled()) {
            return;
        }
        this.setEnabled(true);
        ModuleManager.organizedModules.add(this);
        if (ModuleManager.hud != null && ModuleManager.hud.isEnabled()) {
            ModuleManager.sort();
        }

        if (this.script != null) {
            Stars.scriptManager.onEnable(script);
        } else {
            if (!alwaysOn) {
                MinecraftForge.EVENT_BUS.register(this);
            }
            this.onEnable();
        }

        NotificationManager.moduleState(this, true);
        ToggleSoundManager.moduleState(this, true);
    }

    public void disable() {
        if (!this.isEnabled()) {
            return;
        }
        this.setEnabled(false);
        ModuleManager.organizedModules.remove(this);
        if (this.script != null) {
            Stars.scriptManager.onDisable(script);
        } else {
            if (!alwaysOn) {
                MinecraftForge.EVENT_BUS.unregister(this);
            }
            this.onDisable();
        }

        NotificationManager.moduleState(this, false);
        ToggleSoundManager.moduleState(this, false);
    }

    // getInfo 通用缓存：滑块等设置变化时全局使所有依赖设置的 info 一次性重算，帧内不再每帧格式化
    private static int infoEpoch = 0;
    private int cachedInfoEpoch = -1;
    private String cachedInfoStr = null;

    // 动态信息的短周期定时缓存（毫秒 TTL）：只在周期到了才重算，降到约 10Hz
    private long timedCacheNextRefresh = 0L;
    private String timedCachedInfo = null;

    /** 任意设置被修改时调用，使全部缓存的 info 失效（下一帧重算一次）。 */
    public static void invalidateInfoCaches() {
        infoEpoch++;
    }

    public String getInfo() {
        long ttl = cacheTtlMillis();
        if (ttl > 0) {
            long now = System.currentTimeMillis();
            if (timedCachedInfo == null || now >= timedCacheNextRefresh) {
                timedCachedInfo = computeTimedInfo();
                if (timedCachedInfo == null) {
                    timedCachedInfo = "";
                }
                timedCacheNextRefresh = now + ttl;
            }
            return timedCachedInfo;
        }
        if (cachedInfoStr == null || cachedInfoEpoch != infoEpoch) {
            cachedInfoStr = computeInfo();
            if (cachedInfoStr == null) {
                cachedInfoStr = "";
            }
            cachedInfoEpoch = infoEpoch;
        }
        return cachedInfoStr;
    }

    /** 仅读取设置值、每帧结果不变的模块应重写此方法并交由 getInfo 缓存。 */
    protected String computeInfo() {
        return "";
    }

    /** 依赖实时状态、结果每帧变化的模块：重写此方法并返回毫秒 TTL，按短周期刷新缓存。 */
    protected String computeTimedInfo() {
        return "";
    }

    protected long cacheTtlMillis() {
        return 0L;
    }

    public String getInfoUpdate() { // when called updates the modules info, and sorts if necessary
        String info = getInfo();
        if (info != lastInfo) {
            sort = true;
        }
        lastInfo = info;
        return info;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getName() {
        return this.moduleName;
    }

    public String getNameInHud() {
        return this.moduleName;
    }

    public ArrayList<Setting> getSettings() {
        return this.settings;
    }

    public void registerSetting(Setting Setting) {
        this.settings.add(Setting);
    }

    public Module.category moduleCategory() {
        return this.moduleCategory;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    public void toggle() {
        if (this.isEnabled()) {
            this.disable();
        } else {
            this.enable();
        }
        if (Stars.currentProfile != null && !(this instanceof starshack.module.impl.client.Gui)) {
            Stars.currentProfile.getModule().saved = false;
        }
    }

    public void onUpdate() {
    }

    public void guiUpdate() {
    }

    public void guiButtonToggled(ButtonSetting b) {
    }

    public void onSlide(SliderSetting setting) {
    }

    public int getKeycode() {
        return this.keycode;
    }

    public void setBind(int keybind) {
        this.keycode = keybind;
    }

    public static enum category {
        combat,
        movement,
        player,
        visuals,
        exploits,
        misc,
        configs,
        scripts;
    }
}
