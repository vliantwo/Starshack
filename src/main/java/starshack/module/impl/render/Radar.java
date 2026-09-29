package starshack.module.impl.render;

import starshack.clickgui.ClickGui;
import starshack.module.Module;
import starshack.module.impl.world.AntiBot;
import starshack.module.setting.impl.ButtonSetting;
import starshack.utility.RenderUtils;
import starshack.utility.Utils;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Radar extends Module {
    private ButtonSetting tracerLines;

    private int scale = 2;
    private int playerIndicatorX;
    private int playerIndicatorY;

    private static final int RECT_COLOR = new Color(0, 0, 0, 125).getRGB();

    private static final class Blip {
        final double x;
        final double y;

        Blip(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }

    private final List<Blip> blips = new ArrayList<>();

    public Radar() {
        super("Radar", category.visuals);
        this.registerSetting(tracerLines = new ButtonSetting("Show tracer lines", false));
    }

    @Override
    public void onUpdate() {
        this.scale = new ScaledResolution(mc).getScaleFactor();
        this.playerIndicatorX = 5 + 100 / 2 + 3;
        this.playerIndicatorY = 70 + 52;
        this.blips.clear();
        if (!Utils.nullCheck() || mc.theWorld == null) {
            return;
        }
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player != mc.thePlayer && player.deathTime == 0 && !AntiBot.isBot(player)) {
                double distanceSquared = player.getDistanceSqToEntity(mc.thePlayer);
                if (distanceSquared > 360.0) {
                    continue;
                }
                double playerAngle = (mc.thePlayer.rotationYaw + Math.atan2(player.posX - mc.thePlayer.posX, player.posZ - mc.thePlayer.posZ) * 57.295780181884766) % 360.0;
                double scaledDistance = distanceSquared / 5.0;
                double xOffset = scaledDistance * Math.sin(Math.toRadians(playerAngle));
                double zOffset = scaledDistance * Math.cos(Math.toRadians(playerAngle));
                this.blips.add(new Blip(xOffset, zOffset));
            }
        }
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !Utils.nullCheck()) {
            return;
        }
        if (mc.currentScreen instanceof ClickGui) {
            return;
        }
        if (mc.currentScreen != null || mc.gameSettings.showDebugInfo) {
            return;
        }
        int x = 5;
        int y = 70;
        int rightX = x + 100;
        int bottomY = y + 100;
        Gui.drawRect(x, y, rightX, bottomY, RECT_COLOR);
        Gui.drawRect(x - 1, y - 1, rightX + 1, y, -1);
        Gui.drawRect(x - 1, bottomY, rightX + 1, bottomY + 1, -1);
        Gui.drawRect(x - 1, y, x, bottomY, -1);
        Gui.drawRect(rightX, y, rightX + 1, bottomY, -1);
        RenderUtils.drawPolygon(this.playerIndicatorX, this.playerIndicatorY, 5.0, 3, -1);
        GL11.glPushMatrix();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x * this.scale, mc.displayHeight - this.scale * 170, rightX * this.scale - this.scale * 5, this.scale * 100);
        for (Blip blip : this.blips) {
            if (tracerLines.isToggled()) {
                GL11.glPushMatrix();
                GL11.glEnable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_LINE_SMOOTH);
                GL11.glDisable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_TEXTURE_2D);
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GL11.glLineWidth(0.5f);
                GL11.glColor3d(1.0, 1.0, 1.0);
                GL11.glBegin(GL11.GL_LINES);
                GL11.glVertex2d(this.playerIndicatorX, this.playerIndicatorY);
                GL11.glVertex2d((double) this.playerIndicatorX - blip.x, (double) this.playerIndicatorY - blip.y);
                GL11.glEnd();
                GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glEnable(GL11.GL_TEXTURE_2D);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDisable(GL11.GL_LINE_SMOOTH);
                GL11.glDisable(GL11.GL_BLEND);
                GL11.glPopMatrix();
            }
            RenderUtils.drawPolygon((double) this.playerIndicatorX - blip.x, (double) this.playerIndicatorY - blip.y, 3.0, 4, Color.red.getRGB());
        }
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glPopMatrix();
    }
}
