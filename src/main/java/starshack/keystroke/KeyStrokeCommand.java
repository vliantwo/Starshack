package starshack.keystroke;

import starshack.Stars;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;

public class KeyStrokeCommand extends CommandBase {
    public String getCommandName() {
        return "starshack";
    }

    public void processCommand(ICommandSender sender, String[] args) {
        Stars.toggleKeyStrokeConfigGui();
    }

    public String getCommandUsage(ICommandSender sender) {
        return "/starshack";
    }

    public int getRequiredPermissionLevel() {
        return 0;
    }

    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }
}
