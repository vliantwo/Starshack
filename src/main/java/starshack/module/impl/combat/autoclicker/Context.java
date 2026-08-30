package starshack.module.impl.combat.autoclicker;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;

/**
 * 感知层数据快照：某一帧采集的 Minecraft 上下文。
 * <p>
 * 从 VapeAutoClicker 抽离为独立类，职责单一，便于测试与复用。
 */
public class Context {
    boolean leftDown;
    boolean usingItem;
    boolean inCreative;
    boolean inGame;
    EntityLivingBase target;
    BlockPos breakPos;
    boolean canEdit;
}
