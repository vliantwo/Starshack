package starshack.command.impl;

import starshack.command.Command;
import starshack.command.CommandInput;
import starshack.module.ModuleManager;

public class IRCCommand extends Command {
    public IRCCommand() {
        super("irc");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 0) {
            replyWithHeader("&7Usage: &b" + prefixed("irc") + " <message>");
            return;
        }

        if (ModuleManager.irc == null || !ModuleManager.irc.isEnabled()) {
            replyWithHeader("&cIRC module is disabled.");
            return;
        }

        ModuleManager.irc.sendChatMessage(input.joinArguments(0));
    }
}
