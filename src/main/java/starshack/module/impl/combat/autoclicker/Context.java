package starshack.module.impl.combat.autoclicker;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;

/**
 * 感知层数据快照：StarAutoClicker 每帧采集一次的 Minecraft 上下文。
 * <p>
 * 从 StarAutoClicker 抽离为独立类，职责单一，便于测试与复用。
 * 字段均为包内可见，仅供同包的 StarAutoClicker 读写。
 */
public class Context {

    /** 左键是否按住。 */
    boolean leftDown;

    /** 当前是否正在使用物品（右键长按等）。 */
    boolean usingItem;

    /** 是否处于创造模式。 */
    boolean inCreative;

    /** 是否处于游戏内（未打开界面且窗口有焦点）。 */
    boolean inGame;

    /** 准星命中的实体；未命中实体时为 null。 */
    EntityLivingBase target;

    /** 准星命中的方块位置；未命中方块时为 null。 */
    BlockPos breakPos;

    /** 是否允许编辑世界（可破坏方块）。 */
    boolean canEdit;

    /** 清空全部字段，供复用前重置，避免跨帧数据残留（如未命中时 target/breakPos 保留旧值）。 */
    void reset() {
        leftDown = false;
        usingItem = false;
        inCreative = false;
        inGame = false;
        target = null;
        breakPos = null;
        canEdit = false;
    }
}
