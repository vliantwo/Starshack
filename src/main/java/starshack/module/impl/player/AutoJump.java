package starshack.module.impl.player;

import starshack.event.PrePlayerInputEvent;
import starshack.module.Module;
import starshack.script.model.SimulatedPlayer;
import starshack.utility.Utils;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class AutoJump extends Module {
    public AutoJump() {
        super("Auto Jump", category.player);
    }

    @SubscribeEvent
    public void onPrePlayerInput(PrePlayerInputEvent e) {
        if (!Utils.nullCheck() || mc.currentScreen != null || mc.thePlayer.capabilities.isFlying) return;
        if (!mc.thePlayer.onGround) return;

        SimulatedPlayer sim = SimulatedPlayer.fromClientPlayer(mc.thePlayer.movementInput);
        sim.tick();

        if (!sim.onGround && !e.isJump()) {
            e.setJump(true);
        }
    }
}
