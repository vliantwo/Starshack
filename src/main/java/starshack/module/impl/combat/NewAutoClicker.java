package starshack.module.impl.combat;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import starshack.module.Module;
import starshack.module.ModuleManager;
import starshack.module.setting.impl.ButtonSetting;
import starshack.module.setting.impl.DescriptionSetting;
import starshack.module.setting.impl.SliderSetting;
import starshack.utility.ReflectionUtils;
import starshack.utility.Utils;

import java.lang.reflect.Field;
import java.util.Random;

/**
 * NewAutoClicker —— 逻辑移植自 LiquidBounce Legacy 版 AutoClicker.kt
 * <p>
 * 真源核心逻辑对照：
 * - leftDelay / rightDelay = TimeUtils.randomClickDelay(minCPS, maxCPS)
 * 即 random(1000/maxCPS, 1000/minCPS]，在换算后的延迟区间均匀随机
 * - blockBrokenDelay = 1000/20 * (6+2) = 400ms，破坏方块后强制冷却，
 * 保证"clicker 永远不会在破坏方块的间隙里点击"
 * - leftCanAutoClick / rightCanAutoClick：curBlockDamageMP == 0F && !wasBreakingBlock
 * - Jitter：仅当左键处于激活且可点击状态时，每 tick Random.nextBoolean() 决定是否抖动
 */
public class NewAutoClicker extends Module {

    // ===== 方块破坏节流（对齐 Legacy：blockBrokenDelay / blockLastBroken / isBreakingBlock / wasBreakingBlock）=====
    private static final long BLOCK_BROKEN_DELAY = 1000L / 20L * (6L + 2L); // = 400ms
    private final Random rand = new Random();
    // ===== 左键 =====
    public ButtonSetting leftClick;
    public SliderSetting minCPS;
    public SliderSetting maxCPS;
    // ===== 右键 =====
    public ButtonSetting rightClick;
    public SliderSetting rightMinCPS;
    public SliderSetting rightMaxCPS;
    // ===== 行为 =====
    public ButtonSetting jitter;
    public ButtonSetting blockBreakDelay;       // 破坏方块时禁用点击（LiquidBounce Legacy 核心逻辑）
    public ButtonSetting onlyWhenHolding;       // 仅按住鼠标时点击
    public ButtonSetting notUsingItem;          // 使用物品时不点击
    public ButtonSetting disableInCreative;     // 创造模式禁用
    public ButtonSetting weaponOnly;            // 仅手持武器时点击
    // ===== 点击调度（对齐 Legacy：leftDelay / rightDelay / leftLastSwing / rightLastSwing）=====
    private long leftDelay;
    private long rightDelay;
    private long leftLastSwing;
    private long rightLastSwing;
    private long blockLastBroken;
    private boolean isBreakingBlock;
    private boolean wasBreakingBlock;

    public NewAutoClicker() {
        super("New Auto Clicker", category.combat, 0);
        this.closetModule = true;

        this.registerSetting(new DescriptionSetting("LiquidBounce style auto clicker."));

        // 左键
        this.registerSetting(leftClick = new ButtonSetting("Left click", true));
        this.registerSetting(minCPS = new SliderSetting("Min CPS", 8.0, 1.0, 20.0, 1.0));
        this.registerSetting(maxCPS = new SliderSetting("Max CPS", 12.0, 1.0, 20.0, 1.0));

        // 右键
        this.registerSetting(rightClick = new ButtonSetting("Right click", false));
        this.registerSetting(rightMinCPS = new SliderSetting("Right Min CPS", 8.0, 1.0, 20.0, 1.0));
        this.registerSetting(rightMaxCPS = new SliderSetting("Right Max CPS", 12.0, 1.0, 20.0, 1.0));

        // 行为
        this.registerSetting(jitter = new ButtonSetting("Jitter", false));
        this.registerSetting(blockBreakDelay = new ButtonSetting("Block break delay", true));
        this.registerSetting(onlyWhenHolding = new ButtonSetting("Only when holding mouse", true));
        this.registerSetting(notUsingItem = new ButtonSetting("Not using item", false));
        this.registerSetting(disableInCreative = new ButtonSetting("Disable in creative", false));
        this.registerSetting(weaponOnly = new ButtonSetting("Weapon only", false));
    }

    private static String cpsRange(SliderSetting min, SliderSetting max) {
        return (int) min.getInput() + "-" + (int) max.getInput();
    }

    /**
     * 读取 PlayerControllerMP 的 private 字段 curBlockDamageMP。
     * 1.8.9 MCP 映射下该字段是 private，无法直接访问，用反射取值。
     */
    private static float getCurBlockDamageMP() {
        try {
            Field field = net.minecraft.client.multiplayer.PlayerControllerMP.class.getDeclaredField("curBlockDamageMP");
            field.setAccessible(true);
            Object val = field.get(net.minecraft.client.Minecraft.getMinecraft().playerController);
            return val instanceof Float ? (Float) val : 0F;
        } catch (Exception e) {
            return 0F;
        }
    }

    /**
     * 对齐 LiquidBounce 的 TimeUtils.randomClickDelay(min, max)：
     * return random.nextInt(1000 / maxCPS, 1000 / minCPS + 1);
     * 即在 [1000/maxCPS, 1000/minCPS] 区间内均匀随机，CPS 越高延迟越小。
     */
    private static long randomClickDelay(int minCPS, int maxCPS) {
        minCPS = Math.max(1, minCPS);
        maxCPS = Math.max(minCPS, maxCPS); // 保证 min <= max
        int minDelay = 1000 / maxCPS;      // CPS 上限 → 延迟下限
        int maxDelay = 1000 / minCPS;      // CPS 下限 → 延迟上限
        if (minDelay == maxDelay) return minDelay;
        return minDelay + new Random().nextInt(maxDelay - minDelay + 1);
    }

    @Override
    public String getInfo() {
        if (leftClick.isToggled() && rightClick.isToggled())
            return cpsRange(minCPS, maxCPS) + " / " + cpsRange(rightMinCPS, rightMaxCPS);
        if (leftClick.isToggled()) return cpsRange(minCPS, maxCPS);
        if (rightClick.isToggled()) return cpsRange(rightMinCPS, rightMaxCPS);
        return "off";
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        // 松开所有模拟按键，避免"卡键"
        int attackKey = mc.gameSettings.keyBindAttack.getKeyCode();
        int useKey = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.setKeyBindState(attackKey, false);
        KeyBinding.setKeyBindState(useKey, false);
        ReflectionUtils.setButton(0, false);
        ReflectionUtils.setButton(1, false);
        reset();
    }

    private void reset() {
        leftDelay = randomClickDelay((int) minCPS.getInput(), (int) maxCPS.getInput());
        rightDelay = randomClickDelay((int) rightMinCPS.getInput(), (int) rightMaxCPS.getInput());
        leftLastSwing = 0L;
        rightLastSwing = 0L;
        blockLastBroken = 0L;
        isBreakingBlock = false;
        wasBreakingBlock = false;
    }

    /**
     * 对齐 LiquidBounce Legacy：在 UpdateEvent（ClientTickEvent START）里更新方块破坏状态机，
     * 在 RenderTickEvent END 里执行点击 —— 与 Legacy 的 onUpdate + onRender 分工一致。
     */
    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        if (!Utils.nullCheck()) return;

        // ===== 方块破坏状态机（完全对齐 Legacy）=====
        float blockDamage = getCurBlockDamageMP();
        boolean breakingNow = blockDamage > 0F;

        if (breakingNow) {
            isBreakingBlock = true;
        } else if (isBreakingBlock) {
            isBreakingBlock = false;
            wasBreakingBlock = true;
            blockLastBroken = System.currentTimeMillis();
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!Utils.nullCheck()) return;

        long now = System.currentTimeMillis();

        // 状态机收尾：wasBreakingBlock 只持续一帧
        boolean wasBreaking = wasBreakingBlock;
        wasBreakingBlock = false;

        // ===== 左键 =====
        if (leftClick.isToggled() && (!onlyWhenHolding.isToggled() || Mouse.isButtonDown(0))) {
            if (canLeftClick(now, wasBreaking)) {
                doLeftClick();
                leftLastSwing = now;
                leftDelay = randomClickDelay((int) minCPS.getInput(), (int) maxCPS.getInput());
            }
        } else {
            leftLastSwing = 0L;
        }

        // ===== 右键 =====
        if (rightClick.isToggled() && (!onlyWhenHolding.isToggled() || Mouse.isButtonDown(1))) {
            if (canRightClick(now, wasBreaking)) {
                doRightClick();
                rightLastSwing = now;
                rightDelay = randomClickDelay((int) rightMinCPS.getInput(), (int) rightMaxCPS.getInput());
            }
        } else {
            rightLastSwing = 0L;
        }

        // ===== Jitter（对齐 Legacy：仅左键激活且可点击时触发）=====
        if (jitter.isToggled() && leftClick.isToggled()
                && Mouse.isButtonDown(0) && canLeftClick(now, wasBreaking)) {
            if (rand.nextBoolean()) {
                mc.thePlayer.rotationYaw += rand.nextBoolean() ? -rand.nextFloat() : rand.nextFloat();
            }
        }
    }

    // ===== canAutoClick：对齐 Legacy 的 leftCanAutoClick / rightCanAutoClick =====
    private boolean canLeftClick(long now, boolean wasBreaking) {
        if (leftLastSwing != 0 && now - leftLastSwing < leftDelay) return false;
        if (ModuleManager.killAura != null && ModuleManager.killAura.isEnabled() && KillAura.target != null)
            return false;
        if (mc.currentScreen != null || !mc.inGameHasFocus) return false;
        if (notUsingItem.isToggled() && mc.thePlayer.isUsingItem()) return false;
        if (disableInCreative.isToggled() && mc.thePlayer.capabilities.isCreativeMode) return false;
        if (weaponOnly.isToggled() && !Utils.holdingWeapon()) return false;
        return canBreakClick(now, wasBreaking);
    }

    private boolean canRightClick(long now, boolean wasBreaking) {
        if (rightLastSwing != 0 && now - rightLastSwing < rightDelay) return false;
        if (mc.currentScreen != null || !mc.inGameHasFocus) return false;
        if (disableInCreative.isToggled() && mc.thePlayer.capabilities.isCreativeMode) return false;
        return canBreakClick(now, wasBreaking);
    }

    /**
     * 对齐 Legacy：!isBreakingBlock && !(currentTime - blockLastBroken < blockBrokenDelay)
     * blockBreakDelay 关闭时直接放行。
     */
    private boolean canBreakClick(long now, boolean wasBreaking) {
        if (!blockBreakDelay.isToggled()) return true;
        if (isBreakingBlock) return false;
        if (wasBreaking) return false;
        return now - blockLastBroken >= BLOCK_BROKEN_DELAY;
    }

    private void doLeftClick() {
        int key = mc.gameSettings.keyBindAttack.getKeyCode();
        KeyBinding.onTick(key);
        ReflectionUtils.setButton(0, true);
        ReflectionUtils.setButton(0, false);
    }

    private void doRightClick() {
        int key = mc.gameSettings.keyBindUseItem.getKeyCode();
        KeyBinding.onTick(key);
        ReflectionUtils.setButton(1, true);
        ReflectionUtils.setButton(1, false);
    }
}
