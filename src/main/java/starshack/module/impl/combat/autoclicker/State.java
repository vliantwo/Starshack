package starshack.module.impl.combat.autoclicker;

/**
 * AutoClicker 状态机。
 * <p>
 * 只保留两个真实生效的状态（历史上的 AIMING / PAUSED 从未被使用，已移除），
 * 符合最小状态机原则；由同包的 StarAutoClicker 驱动。
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
