package starshack.module.impl.movement;

import starshack.event.PreUpdateEvent;
import starshack.event.PrePlayerInputEvent;
import starshack.event.SendPacketEvent;
import starshack.module.Module;
import starshack.utility.Utils;
import net.minecraft.network.play.client.C03PacketPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Stasis extends Module {
    public Stasis() {
        super("Stasis", category.movement);
    }

    @SubscribeEvent
    public void onPreUpdate(PreUpdateEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }

        mc.thePlayer.motionX = 0.0D;
        mc.thePlayer.motionY = 0.0D;
        mc.thePlayer.motionZ = 0.0D;
    }

    @SubscribeEvent
    public void onPrePlayerInput(PrePlayerInputEvent e) {
        e.setForward(0.0F);
        e.setStrafe(0.0F);
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (!(e.getPacket() instanceof C03PacketPlayer)) {
            return;
        }

        if (!(e.getPacket() instanceof C03PacketPlayer.C05PacketPlayerLook)) {
            e.setCanceled(true);
        }
    }
}
