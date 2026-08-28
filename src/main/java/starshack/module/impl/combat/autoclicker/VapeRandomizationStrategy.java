package starshack.module.impl.combat.autoclicker;

import java.util.Random;

/**
 * Vape V4 风格的随机化算法。
 * <p>
 * Normal : 均匀随机 (baseDelay ± 20%)
 * Extra  : 多层概率（突发 + 长停顿 + 微抖），等价于 Novoline-bS 的 nextDelay()
 * Extra+ : 高斯(正态) 分布 + 疲劳(越点越慢) + 漂移(长期均值偏移) + 爆发(短促连点)
 * <p>
 * 使用：nextDelay(rand, targetCPS) 返回毫秒延迟。
 * 调用方（VapeAutoClicker）会再做一次 clamp 到 [MIN_DELAY, MAX_DELAY]。
 */
public class VapeRandomizationStrategy implements ClickStrategy {

    private final int mode; // RandomizationMode.NORMAL / EXTRA / EXTRA_PLUS

    public VapeRandomizationStrategy(int mode) {
        this.mode = mode;
    }

    @Override
    public long nextDelay(Random rand, double targetCPS) {
        int target = Math.max(1, (int) targetCPS);
        int baseDelay = 1000 / target;

        int delay;
        switch (mode) {
            case RandomizationMode.EXTRA:
                delay = extra(baseDelay, rand);
                break;
            case RandomizationMode.EXTRA_PLUS:
                delay = extraPlus(baseDelay, rand);
                break;
            default: // NORMAL
                delay = normal(baseDelay, rand);
                break;
        }

        // 通用 clamp + 偶发硬边界（模拟偶尔的"卡一下"）
        delay = Math.max(20, Math.min(1000, delay));
        if (rand.nextInt(100) < 2) {        // 2% 概率极长停顿（像真人分神）
            delay += 50 + rand.nextInt(80);
            delay = Math.min(1000, delay);
        }
        return delay;
    }

    // ================= 三档算法 =================

    /**
     * Normal：均匀随机，baseDelay ± 20%
     */
    private int normal(int baseDelay, Random rand) {
        int spread = Math.max(1, baseDelay / 5);  // ±20%
        return baseDelay + rand.nextInt(spread * 2 + 1) - spread;
    }

    /**
     * Extra：多层概率分布（突发 + 长停顿 + 微抖）
     * 等价于 Novoline-bS nextDelay()，已验证有效。
     */
    private int extra(int baseDelay, Random rand) {
        int variation = rand.nextInt(baseDelay + 1) - (baseDelay / 2);
        int delay = baseDelay + variation;

        if (rand.nextInt(100) < 15) {           // 15% 突发：极短 或 较长
            if (rand.nextBoolean()) {
                delay = 25 + rand.nextInt(16);
            } else {
                delay = baseDelay + 50 + rand.nextInt(41);
            }
        }
        if (rand.nextInt(100) < 8) {             // 8% 长停顿（分心）
            int spikeMult = 50 + rand.nextInt(151);
            delay = (delay * spikeMult) / 100;
        }
        if (rand.nextInt(100) < 10) {            // 10% 微抖动
            delay += 10 + rand.nextInt(26);
        }
        return delay;
    }

    /**
     * Extra+：高斯 + 疲劳 + 漂移 + 爆发
     * 用 Box-Muller 产生正态分布，集中在 targetCPS 附近，更符合真人节奏。
     * 疲劳/漂移的累积由 VapeAutoClicker 外部处理（需跨点击保持状态）。
     */
    private int extraPlus(int baseDelay, Random rand) {
        // 1. 高斯分布（均值 = baseDelay，标准差 = baseDelay/4）
        double u = rand.nextDouble(), v = rand.nextDouble();
        // 避免 u/v 恰好为 0 导致 log(0) / 除零
        if (u <= 0.0) u = 1.0e-9;
        if (v <= 0.0) v = 1.0e-9;
        double z = Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
        double mean = baseDelay;
        double stddev = Math.max(2.0, baseDelay / 4.0);
        int delay = (int) (mean + z * stddev);

        // 2. 爆发：5% 概率极短点击（像连按）
        if (rand.nextInt(100) < 5) {
            delay = Math.max(20, baseDelay / 3);
        }
        return delay;
    }
}
