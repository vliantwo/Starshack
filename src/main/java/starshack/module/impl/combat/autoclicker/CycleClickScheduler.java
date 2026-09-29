package starshack.module.impl.combat.autoclicker;

import java.util.ArrayDeque;
import java.util.Random;

/**
 * 周期点击调度层（按预生成周期序列锁定平均 CPS 的调度思路）。
 * <p>
 * 把"每次点击现算一个随机 delay"改成"预先规划一个周期（约 1000ms）内的点击排队序列，再逐次取用"。
 * 周期内点击次数 ≈ 目标 CPS，逐个间隔仍由 RandomizationStrategy 给出（保留 Normal/Extra/Extra+ 分布），
 * 最后按总时长归一化，使一个周期的均值落在目标 CPS 上——比逐个独立随机更稳、节奏更像人手。
 * <p>
 * 只会缓存队列、不提前推进任何模块状态（fatigue/drift/双击等仍在真正点击时才在模块侧结算）。
 */
final class CycleClickScheduler {

    private final Random rand;
    private final ArrayDeque<Long> pending = new ArrayDeque<>();

    /** 当前排队序列所属的随机化档位；换档或队列耗尽时重新规划。 */
    private int plannedTier = -1;

    CycleClickScheduler(Random rand) {
        this.rand = rand;
    }

    /** 取下一个基础点击间隔（ms）；序列耗尽时按目标 CPS + 策略生成新周期。 */
    long pollDelay(int tier, int cps, ClickStrategy strategy) {
        if (plannedTier != tier || pending.isEmpty()) {
            refill(tier, cps, strategy);
        }
        return pending.poll();
    }

    private void refill(int tier, int cps, ClickStrategy strategy) {
        int k = Math.max(1, Math.min(20, Math.max(1, cps)));   // 本周期点击次数 ≈ 目标 CPS
        long[] raw = new long[k];
        long total = 0;
        for (int i = 0; i < k; i++) {
            raw[i] = clamp(strategy.nextDelay(rand, cps));
            total += raw[i];
        }
        // 归一化：一周期总时长 ≈ 1000ms，从而锁定平均 CPS
        double scale = 1000.0 / Math.max(1L, total);
        pending.clear();
        for (int i = 0; i < k; i++) {
            pending.add(clamp(Math.round(raw[i] * scale)));
        }
        plannedTier = tier;
    }

    private static long clamp(long v) {
        return Math.max(20L, Math.min(1000L, v));   // 对齐 Module 的 MIN_DELAY / MAX_DELAY
    }
}