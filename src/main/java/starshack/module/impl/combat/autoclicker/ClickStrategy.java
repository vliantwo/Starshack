package starshack.module.impl.combat.autoclicker;

import java.util.Random;

/**
 * 策略接口：决定"下一次点击的延迟是多少"。
 * 可插拔：Normal / Extra / Extra+ 都实现这个接口。
 */
public interface ClickStrategy {
    /**
     * @param targetCPS 目标 CPS（clicks per second）
     */
    long nextDelay(Random rand, double targetCPS);
}
