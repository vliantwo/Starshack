package starshack.command.impl;

import starshack.command.Command;
import starshack.command.CommandInput;
import starshack.helper.PingHelper;

public class Ping extends Command {
    public Ping() {
        super("ping");
    }

    @Override
    public void execute(CommandInput input) {
        PingHelper.checkPing();
    }
}
