package starshack.command.impl;

import starshack.command.Command;
import starshack.command.CommandInput;
import starshack.module.Module;
import starshack.module.ModuleManager;

public class ShowAll extends Command {
    public ShowAll() {
        super("show", "showall");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() > 0 && !"all".equalsIgnoreCase(input.getArgument(0))) {
            syntaxError();
            return;
        }

        int count = 0;
        for (Module module : ModuleManager.modules) {
            if (module.isHidden()) {
                module.setHidden(false);
                count++;
            }
        }
        replyWithHeader("&7Made &a" + count + " &7module" + (count == 1 ? "" : "s") + " visible in HUD.");
    }
}
