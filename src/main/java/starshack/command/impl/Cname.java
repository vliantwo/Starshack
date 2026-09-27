package starshack.command.impl;

import starshack.command.Command;
import starshack.command.CommandInput;
import starshack.module.impl.other.NameHider;

public class Cname extends Command {
    public Cname() {
        super("namehider");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 0) {
            syntaxError();
            return;
        }

        NameHider.setFakeName(input.joinArguments(0));
        replyWithHeader("&7Name has been set to &b" + NameHider.fakeName + "&7.");
    }
}
