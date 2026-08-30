package starshack.module.impl.combat.autoclicker;

/**
 * AutoClicker 状态机（精简版）。
 * <p>
 * 优化点：移除原 VapeAutoClicker 中定义但未使用的 AIMING / PAUSED，
 * 只保留两个真实生效的状态，符合最小状态机原则。
 */
public enum State {
    /**
     * 空闲：未满足点击条件（未按住、不在游戏、被 KillAura 让位等）
     */
    IDLE,
    /**
     * 连点中：满足所有条件，按 CPS 节奏执行点击
     */
    BURST
}
