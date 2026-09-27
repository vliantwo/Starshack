package starshack.module.impl.render;

import starshack.font.StarsFonts;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * Drag handles for the independently positioned HUD panels.
 */
public final class HudEditor extends GuiScreen {
    private Element dragged;
    private int lastMouseX;
    private int lastMouseY;

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawRect(0, 0, width, height, 0xA0000000);
        HudRenderer.drawEditorPreview();
        NotificationManager.drawEditorPreview();
        drawHandle("MODULE LIST", HUD.posX, HUD.posY, 120, 44, Element.MODULES);
        drawHandle("INVENTORY", (float) HUD.starsInventoryX.getInput(), (float) HUD.starsInventoryY.getInput(),
                HudRenderer.getInventoryWidth(), HudRenderer.getInventoryHeight(), Element.INVENTORY);
        drawHandle("TARGETS", (float) HUD.starsTargetsX.getInput(), (float) HUD.starsTargetsY.getInput() - 13,
                HudRenderer.getTargetsWidth(), HudRenderer.getTargetsHeight() + 13, Element.TARGETS);
        drawHandle("NOTIFICATION", NotificationManager.getEditorX(), NotificationManager.getEditorY(),
                NotificationManager.getEditorWidth(), NotificationManager.getEditorHeight(), Element.NOTIFICATION);
        StarsFonts.bold(18).drawString("STARSHACK HUD EDITOR", 12, 12, 0xFFFFFFFF, true);
        StarsFonts.thin(16).drawString("Drag a highlighted panel. ESC closes the editor.", 12, 31, 0xFFB9BBBE, false);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton != 0) return;
        dragged = at(mouseX, mouseY);
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long elapsed) {
        super.mouseClickMove(mouseX, mouseY, button, elapsed);
        if (button != 0 || dragged == null) return;
        int deltaX = mouseX - lastMouseX;
        int deltaY = mouseY - lastMouseY;
        switch (dragged) {
            case MODULES:
                HUD.setAbsolutePosition(HUD.posX + deltaX, HUD.posY + deltaY);
                break;
            case INVENTORY:
                HUD.starsInventoryX.setValue(HUD.starsInventoryX.getInput() + deltaX);
                HUD.starsInventoryY.setValue(HUD.starsInventoryY.getInput() + deltaY);
                break;
            case TARGETS:
                HUD.starsTargetsX.setValue(HUD.starsTargetsX.getInput() + deltaX);
                HUD.starsTargetsY.setValue(HUD.starsTargetsY.getInput() + deltaY);
                break;
            case NOTIFICATION:
                // Stored as right/bottom offsets so the chosen anchor survives resolution changes.
                HUD.starsNotificationsX.setValue(HUD.starsNotificationsX.getInput() - deltaX);
                HUD.starsNotificationsY.setValue(HUD.starsNotificationsY.getInput() - deltaY);
                break;
            default:
                break;
        }
        lastMouseX = mouseX;
        lastMouseY = mouseY;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) dragged = null;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void drawHandle(String label, float x, float y, float elementWidth, float elementHeight, Element element) {
        int color = dragged == element ? 0xFFFFFFFF : 0xFF8A8AFF;
        drawRect((int) x - 2, (int) y - 2, (int) (x + elementWidth) + 2, (int) (y + elementHeight) + 2, color);
        StarsFonts.thin(14).drawString(label, x + 3, y - 12, color, true);
    }

    private Element at(int mouseX, int mouseY) {
        if (contains(mouseX, mouseY, HUD.posX, HUD.posY, 120, 44)) return Element.MODULES;
        if (contains(mouseX, mouseY, HUD.starsInventoryX.getInput(), HUD.starsInventoryY.getInput(),
                HudRenderer.getInventoryWidth(), HudRenderer.getInventoryHeight()))
            return Element.INVENTORY;
        if (contains(mouseX, mouseY, HUD.starsTargetsX.getInput(), HUD.starsTargetsY.getInput() - 13,
                HudRenderer.getTargetsWidth(), HudRenderer.getTargetsHeight() + 13))
            return Element.TARGETS;
        if (contains(mouseX, mouseY, NotificationManager.getEditorX(), NotificationManager.getEditorY(),
                NotificationManager.getEditorWidth(), NotificationManager.getEditorHeight()))
            return Element.NOTIFICATION;
        return null;
    }

    private static boolean contains(int mouseX, int mouseY, double x, double y, double width, double height) {
        return mouseX >= x - 2 && mouseX <= x + width + 2 && mouseY >= y - 2 && mouseY <= y + height + 2;
    }

    private enum Element {MODULES, INVENTORY, TARGETS, NOTIFICATION}
}
