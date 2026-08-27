package starshack;

import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import starshack.clickgui.ClickGui;
import starshack.clickgui.NovolineClickGui;
import starshack.command.CommandManager;
import starshack.event.PostProfileLoadEvent;
import starshack.event.PostSetSliderEvent;
import starshack.helper.DebugHelper;
import starshack.helper.MouseHelper;
import starshack.helper.PingHelper;
import starshack.helper.RotationHelper;
import starshack.keystroke.KeyStrokeCommand;
import starshack.keystroke.KeyStrokeConfigGui;
import starshack.keystroke.KeyStrokeRenderer;
import starshack.lag.handler.UnifiedLagHandler;
import starshack.module.Module;
import starshack.module.ModuleManager;
import starshack.module.setting.impl.SliderSetting;
import starshack.script.ScriptDefaults;
import starshack.script.ScriptManager;
import starshack.script.model.Entity;
import starshack.script.model.NetworkPlayer;
import starshack.utility.*;
import starshack.utility.profile.Profile;
import starshack.utility.profile.ProfileManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Mod(modid = "starshack", name = "Starshack", version = "KMV5", acceptedMinecraftVersions = "[1.8.9]")
public class Stars {
    public static boolean DEBUG = false;

    public static Minecraft mc = Minecraft.getMinecraft();

    private static KeyStrokeRenderer keyStrokeRenderer;

    private static boolean isKeyStrokeConfigGuiToggled;

    private static final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2);
    private static final ExecutorService cachedExecutor = Executors.newCachedThreadPool();

    public static ModuleManager moduleManager;
    public static ClickGui clickGui;
    public static ProfileManager profileManager;
    public static ScriptManager scriptManager;
    public static CommandManager commandManager;
    public static PlayerRelationsManager playerRelationsManager;
    public static Profile currentProfile;
    public static PacketsHandler packetsHandler;
    public static UnifiedLagHandler lagHandler;

    private static boolean firstLoad;

    public Stars() {
        moduleManager = new ModuleManager();
    }

    /**
     * ★【新增方法】自动保存：如果当前 profile 的 saved 标记为 false（被 Setting 改动置脏），
     * 则写盘并复位标记。由 onTick 每 tick 调用。
     * 链式访问 getModule()，无需 import ProfileModule。
     */
    private static void autoSaveProfile() {
        if (Stars.currentProfile == null || Stars.profileManager == null) {
            return;
        }
        if (Stars.currentProfile.getModule() != null && !Stars.currentProfile.getModule().saved) {
            Stars.profileManager.saveProfile(Stars.currentProfile);
            Stars.currentProfile.getModule().saved = true;
        }
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        Runtime.getRuntime().addShutdownHook(new Thread(scheduledExecutor::shutdown));
        Runtime.getRuntime().addShutdownHook(new Thread(cachedExecutor::shutdown));

        // ★【修改③】关闭游戏兜底存盘（防崩溃/强杀时没存上）
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (Stars.currentProfile != null && Stars.profileManager != null) {
                    Stars.profileManager.saveProfile(Stars.currentProfile);
                }
            } catch (Throwable ignored) {
                // 关闭阶段环境可能已销毁，异常不影响退出
            }
        }));

        ClientCommandHandler.instance.registerCommand(new KeyStrokeCommand());

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new DebugHelper());
        MinecraftForge.EVENT_BUS.register(new MouseHelper());
        MinecraftForge.EVENT_BUS.register(RotationHelper.get());
        MinecraftForge.EVENT_BUS.register(new KeyStrokeRenderer());
        MinecraftForge.EVENT_BUS.register(new PingHelper());
        MinecraftForge.EVENT_BUS.register(packetsHandler = new PacketsHandler());
        MinecraftForge.EVENT_BUS.register(new ModuleUtils());
        MinecraftForge.EVENT_BUS.register(lagHandler = new UnifiedLagHandler());

        ReflectionUtils.setupFields();
        playerRelationsManager = new PlayerRelationsManager();
        playerRelationsManager.load();
        moduleManager.register();
        MinecraftForge.EVENT_BUS.register(new BlockHighlightSharedHandler());
        scriptManager = new ScriptManager();
        keyStrokeRenderer = new KeyStrokeRenderer();
        clickGui = new NovolineClickGui();
        profileManager = new ProfileManager();
        ScriptDefaults.reloadModules();
        scriptManager.loadScripts();
        profileManager.loadProfiles();

        // ★【修改①】启动时把当前 profile 的设置值灌回所有 Setting（缺这行 = 存了也读不回来）
        if (Stars.currentProfile == null && Stars.profileManager != null
                && Stars.profileManager.profiles != null && !Stars.profileManager.profiles.isEmpty()) {
            Stars.currentProfile = Stars.profileManager.profiles.get(0);
        }
        if (Stars.currentProfile != null) {
            profileManager.loadProfile(Stars.currentProfile.getName());
        }

        ReflectionUtils.setKeyBindings();

        commandManager = new CommandManager();
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent e) {
        if (e.phase == Phase.END) {
            // ★【修改②】自动保存：检测到脏标记就存盘（放在最前，不依赖 nullCheck，任何情况都能存）
            autoSaveProfile();

            if (Utils.nullCheck()) {
                if (mc.thePlayer.ticksExisted % 6000 == 0) { // reset cache every 5 minutes
                    Entity.clearCache();
                    NetworkPlayer.clearCache();
                    if (DebugHelper.BACKGROUND) {
                        Utils.sendMessage("&aticks % 6000 == 0 &7reached, clearing script caches. (&dEntity&7, &dNetworkPlayer&7)");
                    }
                }
                if (ReflectionUtils.ERROR) {
                    Utils.sendMessage("&cThere was an error, relaunch the game.");
                    ReflectionUtils.ERROR = false;
                }

                MouseHelper.updateWheelCache();

                for (Module module : getModuleManager().getModules()) {
                    if (mc.currentScreen == null && module.canBeEnabled()) {
                        module.onKeyBind();
                    } else if (mc.currentScreen instanceof ClickGui) {
                        module.guiUpdate();
                        module.syncKeyBindState();
                    } else {
                        module.syncKeyBindState();
                    }

                    if (module.isEnabled()) {
                        module.onUpdate();
                    }
                }
                if (mc.currentScreen == null) {
                    for (Module module : Stars.scriptManager.scripts.values()) {
                        module.onKeyBind();
                    }
                } else {
                    for (Module module : Stars.scriptManager.scripts.values()) {
                        module.syncKeyBindState();
                    }
                    if (mc.currentScreen instanceof ClickGui) {
                        if (applyKillAuraRangeConstraints()) {
                            clickGui.onSliderChange();
                        }
                        if (mc.thePlayer.getHealth() <= 0.0f) {
                            mc.displayGuiScreen(null);
                        }
                    }
                }
            }

            if (isKeyStrokeConfigGuiToggled) {
                isKeyStrokeConfigGuiToggled = false;
                mc.displayGuiScreen(new KeyStrokeConfigGui());
            }
        } else {
            MouseHelper.clearWheelCache();
            if (mc.currentScreen == null && Utils.nullCheck()) {
                for (Profile profile : Stars.profileManager.profiles) {
                    profile.getModule().onKeyBind();
                }
            } else if (Utils.nullCheck()) {
                for (Profile profile : Stars.profileManager.profiles) {
                    profile.getModule().syncKeyBindState();
                }
            }
        }
    }

    @SubscribeEvent
    public void onPostProfileLoad(PostProfileLoadEvent e) {
        applyKillAuraRangeConstraints();
        clickGui.onSliderChange();
    }

    @SubscribeEvent
    public void onPostSetSlider(PostSetSliderEvent e) {
        applyKillAuraRangeConstraints();
        clickGui.onSliderChange();
    }

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent e) {
        if (e.entity == mc.thePlayer) {
            if (!firstLoad) {
                firstLoad = true;
                scriptManager.loadScripts();
            }
            Entity.clearCache();
            NetworkPlayer.clearCache();
            starshack.utility.FrozenEntitySync.get().clearAll();
            if (DebugHelper.BACKGROUND) {
                Utils.sendMessage("&enew world&7, clearing script caches. (&dEntity&7, &dNetworkPlayer&7)");
            }
        }
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    public static ScheduledExecutorService getScheduledExecutor() {
        return scheduledExecutor;
    }

    public static ExecutorService getCachedExecutor() {
        return cachedExecutor;
    }

    public static KeyStrokeRenderer getKeyStrokeRenderer() {
        return keyStrokeRenderer;
    }

    public static void toggleKeyStrokeConfigGui() {
        isKeyStrokeConfigGuiToggled = true;
    }

    public static void handleFrozenKeybinds() {
        if (!Utils.nullCheck()) return;

        MouseHelper.updateWheelCache();

        if (mc.currentScreen == null) {
            for (Module module : getModuleManager().getModules()) {
                if (module.canBeEnabled()) {
                    module.onKeyBind();
                }
            }
            for (Module module : scriptManager.scripts.values()) {
                module.onKeyBind();
            }
        } else if (mc.currentScreen instanceof ClickGui) {
            for (Module module : getModuleManager().getModules()) {
                module.guiUpdate();
                module.syncKeyBindState();
            }
            for (Module module : scriptManager.scripts.values()) {
                module.syncKeyBindState();
            }
        } else {
            for (Module module : getModuleManager().getModules()) {
                module.syncKeyBindState();
            }
            for (Module module : scriptManager.scripts.values()) {
                module.syncKeyBindState();
            }
        }

        if (isKeyStrokeConfigGuiToggled) {
            isKeyStrokeConfigGuiToggled = false;
            mc.displayGuiScreen(new KeyStrokeConfigGui());
        }
    }

    private boolean applyKillAuraRangeConstraints() {
        if (ModuleManager.killAura == null) {
            return false;
        }

        SliderSetting attackRange = ModuleManager.killAura.getAttackRangeSetting();
        SliderSetting swingRange = ModuleManager.killAura.getSwingRangeSetting();
        SliderSetting aimRange = ModuleManager.killAura.getAimRangeSetting();
        if (attackRange == null || swingRange == null || aimRange == null) {
            return false;
        }

        boolean changed = false;
        double attack = attackRange.getInput();
        double swing = swingRange.getInput();
        double aim = aimRange.getInput();

        if (swing < attack) {
            swingRange.setValue(attack);
            swing = swingRange.getInput();
            changed = true;
        }

        if (aim < swing) {
            aimRange.setValue(swing);
            changed = true;
        }

        return changed;
    }
}