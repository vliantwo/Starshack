package starshack.module.impl.combat.autoclicker;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.init.Blocks;
import net.minecraft.item.*;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Mouse;
import starshack.module.Module;
import starshack.module.ModuleManager;
import starshack.module.impl.combat.KillAura;
import starshack.utility.ReflectionUtils;
import starshack.utility.Utils;

import java.util.Random;

/**
 * Vape V4 风格 AutoClicker（L4 架构）。
 * <p>
 * 架构：
 * 感知(Context) → 决策(State: IDLE/AIMING/BURST) → 策略(ClickStrategy) → 执行(KeyBinding + 反射)
 * <p>
 * Vape V4 差异化特性：
 * - Randomization 三档（Normal / Extra / Extra+）
 * - Trigger Mode（Always / Hover / Weapon）
 * - Limit to Item + Item Mode
 * - Break Blocks 增强（Delay + OnlyWithTool）
 * - Jitter（Off / Low / High）
 * - Fatigue / Drift / DoubleClick（Extra+ 特性）
 * - CPS 差值提示（Vape 社区共识：diff >= 4）
 */
public class VapeAutoClicker extends Module {

    // ============ 常量 ============
    private static final int MIN_DELAY = 20;   // 50 CPS 上限，防极端
    private static final int MAX_DELAY = 1000; // 1 CPS 下限

    // ============ 点击调度 ============
    private final Random rand = new Random();
    public VapeAutoClickerConfig cfg;
    private State state = State.IDLE;
    private long nextClickTime = 0L;
    private int clickCount = 0;          // 用于 Fatigue
    private double driftOffset = 0;      // 漂移累计（Extra+）

    // ============ 破块 ============
    private boolean isHoldingBlockBreak = false;

    public VapeAutoClicker() {
        // ★ 对齐真实 Module 构造器：Module(String name, category cat)（无 keycode 的三参构造不存在，用两参）
        super("Auto Clicker V4", Module.category.combat);
        this.cfg = new VapeAutoClickerConfig(this);
        this.closetModule = true;   // ★ 真实字段名是 closetModule（少一个 t），已在 Module.java:32 确认
    }

    @Override
    public String getInfo() {
        double cps = cfg.getCPS();
        String mode = RandomizationMode.nameOf(cfg.getRandomization());
        return String.format("%.1f | %s", cps, mode);
    }

    @Override
    public void onEnable() {
        reset();
    }

    @Override
    public void onDisable() {
        releaseBreak();
        reset();
    }

    private void reset() {
        nextClickTime = 0L;
        clickCount = 0;
        driftOffset = 0;
        state = State.IDLE;
        releaseBreak();
    }

    // ================= 主逻辑（RenderTick，每帧）=================
    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (!Utils.nullCheck()) return;

        // 1. 感知：当前上下文
        Context ctx = collect();

        // 2. 破块处理（Break Blocks，沿用 Novoline 反射手法）—— 必须在决策前，影响 state
        handleBreakBlocks(ctx);

        // 3. 决策：状态流转（Trigger Mode 在这里生效）
        State next = updateState(ctx);

        // 4. Jitter（点击时微调瞄准，反检测）
        if (cfg.isJitterEnabled()) {
            applyJitter(ctx);
        }

        // 5. 执行：到时间就点
        if (next == State.BURST && System.currentTimeMillis() >= nextClickTime) {
            doClick();
            nextClickTime = System.currentTimeMillis() + nextDelay();
        }

        this.state = next;
    }

    // ================= 感知层 =================
    private Context collect() {
        Context ctx = new Context();
        ctx.leftDown = Mouse.isButtonDown(0);
        ctx.usingItem = mc.thePlayer.isUsingItem();
        ctx.inCreative = mc.thePlayer.capabilities.isCreativeMode;
        ctx.inGame = (mc.currentScreen == null && mc.inGameHasFocus);

        if (mc.objectMouseOver != null) {
            if (mc.objectMouseOver.entityHit instanceof EntityLivingBase) {
                ctx.target = (EntityLivingBase) mc.objectMouseOver.entityHit;
            }
            if (mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
                ctx.breakPos = mc.objectMouseOver.getBlockPos();
            }
        }
        ctx.canEdit = mc.thePlayer.capabilities.allowEdit;
        return ctx;
    }

    // ================= 决策层（状态机）=================
    private State updateState(Context ctx) {
        // 全局前置：KillAura 在攻击目标时让位
        KillAura ka = ModuleManager.killAura;
        boolean kaActive = ka != null && ka.isEnabled() && KillAura.target != null;

        if (!ctx.leftDown || !ctx.inGame || kaActive) {
            return State.IDLE;
        }

        // Trigger Mode 判断（Vape 核心）
        if (cfg.isTriggerHover() && ctx.target == null) {
            return State.IDLE;   // Hover：没对着实体就不点
        }
        if (cfg.isTriggerWeapon() && !holdingWeapon()) {
            return State.IDLE;   // Weapon：没拿武器就不点
        }
        if (cfg.limitToItem.isToggled() && !matchesItemMode()) {
            return State.IDLE;   // Limit to Item：不符合物品模式就不点
        }

        // notUsingItem / disableCreative
        if (cfg.notUsingItem.isToggled() && ctx.usingItem) return State.IDLE;
        if (cfg.disableCreative.isToggled() && ctx.inCreative) return State.IDLE;

        // 正在破方块时，不攻击点击（避免冲突）
        if (cfg.breakBlocks.isToggled() && ctx.breakPos != null && ctx.canEdit) {
            return State.IDLE;
        }

        return State.BURST;
    }

    // ================= 策略层：下一次延迟 =================
    private long nextDelay() {
        int randomization = cfg.getRandomization();
        double cps = cfg.getCPS();

        // 根据 Randomization 档位选策略
        ClickStrategy strategy = new VapeRandomizationStrategy(randomization);
        long delay = strategy.nextDelay(rand, cps);

        // ---- Extra+ 专属：Fatigue（越点越慢）----
        if (randomization == RandomizationMode.EXTRA_PLUS && cfg.fatigue.isToggled()) {
            clickCount++;
            if (clickCount > 50) {
                double fatigue = 1.0 + (clickCount - 50) * 0.01; // 每多一次 +1%
                delay = (long) (delay * Math.min(1.6, fatigue));
            }
        }

        // ---- Extra+ 专属：Drift（长期均值偏移）----
        if (randomization == RandomizationMode.EXTRA_PLUS && cfg.drift.isToggled()) {
            driftOffset += (rand.nextDouble() - 0.5) * 2.0; // ±1ms/click
            driftOffset = Math.max(-15, Math.min(15, driftOffset));
            delay += (long) driftOffset;
        }

        // ---- DoubleClick：偶尔一次点两下 ----
        if (cfg.doubleClick.isToggled() && rand.nextInt(100) < 8) {
            delay = Math.max(20, delay / 2);  // 半延迟 = 双击
        }

        return Math.max(MIN_DELAY, Math.min(MAX_DELAY, delay));
    }

    // ================= 执行层 =================
    private void doClick() {
        // ★ 沿用 Novoline 反射手法：setButton(0, true) 模拟左键按下
        ReflectionUtils.setButton(0, true);
        // 配合一次 keybind tick，确保 Minecraft 识别到点击
        KeyBinding.onTick(mc.gameSettings.keyBindAttack.getKeyCode());
    }

    // ================= Break Blocks（沿用 Novoline 逻辑 + Vape 增强）=================
    private void handleBreakBlocks(Context ctx) {
        if (!cfg.breakBlocks.isToggled()) {
            releaseBreak();
            return;
        }
        if (!ctx.canEdit) {
            releaseBreak();
            return;
        }
        if (ctx.breakPos == null) {
            releaseBreak();
            return;
        }

        // OnlyWithTool：只在拿镐/铲/斧时暂停（拿别的继续连点）
        if (cfg.breakOnlyWithTool.isToggled() && !holdingTool()) {
            releaseBreak();
            return;
        }

        Block block = mc.theWorld.getBlockState(ctx.breakPos).getBlock();
        boolean isBreakable = (block != Blocks.air && !(block instanceof BlockLiquid));

        if (isBreakable && !isHoldingBlockBreak) {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            KeyBinding.setKeyBindState(key, true);
            ReflectionUtils.setButton(0, true);
            isHoldingBlockBreak = true;
        } else if (!isBreakable && isHoldingBlockBreak) {
            releaseBreak();
        }
    }

    private void releaseBreak() {
        if (isHoldingBlockBreak) {
            int key = mc.gameSettings.keyBindAttack.getKeyCode();
            KeyBinding.setKeyBindState(key, false);
            ReflectionUtils.setButton(0, false);
            isHoldingBlockBreak = false;
        }
    }

    // ================= Jitter（反检测：yaw/pitch 微抖）=================
    private void applyJitter(Context ctx) {
        if (ctx.target == null) return;  // 没对着目标不抖
        int level = cfg.getJitter();     // 1=Low, 2=High
        float amount = (level >= VapeEnums.Jitter.HIGH) ? 1.0F : 0.4F;

        if (rand.nextBoolean()) {
            mc.thePlayer.rotationYaw += (rand.nextFloat() - 0.5F) * 2 * amount;
        }
        if (rand.nextBoolean()) {
            mc.thePlayer.rotationPitch += (rand.nextFloat() - 0.5F) * 2 * amount;
        }
    }

    // ================= 工具判断 =================
    private boolean holdingWeapon() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return false;
        Item item = held.getItem();
        return item instanceof ItemSword || item instanceof ItemAxe;
    }

    private boolean holdingTool() {
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return false;
        Item item = held.getItem();
        return item instanceof ItemPickaxe || item instanceof ItemSpade || item instanceof ItemAxe;
    }

    private boolean matchesItemMode() {
        int mode = cfg.getTriggerMode();  // 0=Sword, 1=Any
        ItemStack held = mc.thePlayer.getHeldItem();
        if (held == null) return false;
        if (mode == VapeEnums.ItemMode.ANY) {
            return true;  // 只要拿东西就点
        }
        // SWORD：只有拿剑才点
        return held.getItem() instanceof ItemSword;
    }

    // ============ 状态机 ============
    private enum State {IDLE, AIMING, BURST, PAUSED}

    // ================= 感知数据快照 =================
    private static class Context {
        boolean leftDown;
        boolean usingItem;
        boolean inCreative;
        boolean inGame;
        EntityLivingBase target;
        BlockPos breakPos;
        boolean canEdit;
    }
}
