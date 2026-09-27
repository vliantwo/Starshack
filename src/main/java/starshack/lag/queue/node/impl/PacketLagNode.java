package starshack.lag.queue.node.impl;

import starshack.lag.api.EnumLagDirection;
import starshack.lag.handler.AbstractFastTrackProvider;
import starshack.lag.queue.node.api.AbstractLagNode;
import net.minecraft.network.Packet;
import org.jetbrains.annotations.NotNull;

public final class PacketLagNode extends AbstractLagNode {

    private final @NotNull Packet<?> packet;
    private final @NotNull EnumLagDirection direction;
    private final long queuedAtMs;

    public PacketLagNode(final @NotNull Packet<?> packet, final @NotNull EnumLagDirection direction) {
        this.packet = packet;
        this.direction = direction;
        this.queuedAtMs = System.currentTimeMillis();
    }

    public long getQueuedAtMs() {
        return queuedAtMs;
    }

    public void goThrough(final @NotNull AbstractFastTrackProvider fastTrack) {
        if (direction == EnumLagDirection.OUTBOUND) {
            fastTrack.forPacket(packet);
        }
        direction.passThroughChannel(packet);
    }

}
