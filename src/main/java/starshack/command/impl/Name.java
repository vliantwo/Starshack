package starshack.command.impl;

import starshack.command.Command;
import starshack.command.CommandInput;
import starshack.utility.Utils;
import starshack.utility.system.SystemUtils;

public class Name extends Command {
    public Name() {
        super("name", "ign", "name");
    }

    @Override
    public void execute(CommandInput input) {
        if (!Utils.nullCheck()) {
            return;
        }

        SystemUtils.addToClipboard(mc.thePlayer.getName());
        replyWithHeader("&7Copied &b" + mc.thePlayer.getName() + " &7to clipboard");
    }
}
