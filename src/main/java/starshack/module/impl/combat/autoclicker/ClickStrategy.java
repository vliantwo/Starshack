package starshack.module.impl.combat.autoclicker;

import java.util.Random;

/**
 * 策略接口：决定"下一次点击的延迟是多少"。
 * 可插拔：Normal / Extra / Extra+ 都实现这个接口。
 */
public interface ClickStrategy {
    /**
     * @param rand      随机数生成器
     * @param targetCPS 目标 CPS（clicks per second）
     * @return 下一次点击的延迟（毫秒）
     */
    long nextDelay(Random rand, double targetCPS);
}
