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
import starshack.clickgui.StarsClickGui;
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
import starshack.socket.SocketBridge;
import starshack.utility.*;
import starshack.utility.profile.Profile;
import starshack.utility.profile.ProfileManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Mod(modid = "starshack", name = "StarShack",
        useMetadata = true, acceptedMinecraftVersions = "[1.8.9]")
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

    private static void autoSaveProfile() {
        if (Stars.currentProfile == null || Stars.profileManager == null) return;
        if (Stars.currentProfile.getModule() != null && !Stars.currentProfile.getModule().saved) {
            Stars.profileManager.saveProfile(Stars.currentProfile);
            Stars.currentProfile.getModule().saved = true;
        }
    }

    public static ModuleManager getModuleManager() {
        return moduleManager;
    }

    @EventHandler
    public void init(FMLInitializationEvent e) {
        Runtime.getRuntime().addShutdownHook(new Thread(SocketBridge::stop));
        Runtime.getRuntime().addShutdownHook(new Thread(scheduledExecutor::shutdown));
        Runtime.getRuntime().addShutdownHook(new Thread(cachedExecutor::shutdown));
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                if (Stars.currentProfile != null && Stars.profileManager != null)
                    Stars.profileManager.saveProfile(Stars.currentProfile);
            } catch (Throwable ignored) {
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
        clickGui = new StarsClickGui();
        profileManager = new ProfileManager();
        ScriptDefaults.reloadModules();
        scriptManager.loadScripts();
        profileManager.loadProfiles();

        if (Stars.currentProfile == null && Stars.profileManager != null
                && !Stars.profileManager.profiles.isEmpty())
            Stars.currentProfile = Stars.profileManager.profiles.get(0);
        if (Stars.currentProfile != null)
            profileManager.loadProfile(Stars.currentProfile.getName());

        ReflectionUtils.setKeyBindings();
        commandManager = new CommandManager();

        SocketBridge.start(25575);
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
        }
    }

    @SubscribeEvent
    public void onTick(ClientTickEvent e) {
        if (e.phase == Phase.END) {
            autoSaveProfile();
            if (!Utils.nullCheck()) return;
            if (mc.thePlayer.ticksExisted % 6000 == 0) {
                Entity.clearCache();
                NetworkPlayer.clearCache();
            }
            MouseHelper.updateWheelCache();
            for (Module module : getModuleManager().getModules()) {
                if (mc.currentScreen == null && module.canBeEnabled()) module.onKeyBind();
                else if (mc.currentScreen instanceof ClickGui) {
                    module.guiUpdate();
                    module.syncKeyBindState();
                } else module.syncKeyBindState();
                if (module.isEnabled()) module.onUpdate();
            }
            if (isKeyStrokeConfigGuiToggled) {
                isKeyStrokeConfigGuiToggled = false;
                mc.displayGuiScreen(new KeyStrokeConfigGui());
            }
        }
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

    private boolean applyKillAuraRangeConstraints() {
        if (ModuleManager.killAura == null) return false;
        SliderSetting ar = ModuleManager.killAura.getAttackRangeSetting();
        SliderSetting sr = ModuleManager.killAura.getSwingRangeSetting();
        SliderSetting aim = ModuleManager.killAura.getAimRangeSetting();
        if (ar == null || sr == null || aim == null) return false;
        boolean changed = false;
        if (sr.getInput() < ar.getInput()) {
            sr.setValue(ar.getInput());
            changed = true;
        }
        if (aim.getInput() < sr.getInput()) {
            aim.setValue(sr.getInput());
            changed = true;
        }
        return changed;
    }
}