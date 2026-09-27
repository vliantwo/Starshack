package starshack.command.impl;

import starshack.command.Command;
import starshack.command.CommandInput;
import starshack.Stars;
import starshack.command.CommandManager;

import java.util.Arrays;
import java.util.List;

public class Prefix extends Command {
    private static final List<String> EXAMPLE_PREFIXES = Arrays.asList(".", ",", ";", "/", "-", "=", "[", "]", "\\", "'", "`");

    public Prefix() {
        super("prefix");
    }

    @Override
    public void execute(CommandInput input) {
        if (input.argumentCount() == 0) {
            replyWithHeader("&7Current prefix: &b" + Stars.commandManager.getPrefix());
            return;
        }

        if (input.argumentCount() != 1) {
            syntaxError();
            return;
        }

        String prefix = input.getArgument(0);
        if (!CommandManager.isValidPrefix(prefix)) {
            replyWithHeader("&7Prefix must be a single non-space character.");
            return;
        }

        Stars.commandManager.setPrefix(prefix);
        replyWithHeader("&7Chat command prefix set to &b" + Stars.commandManager.getPrefix() + "&7.");
    }

    @Override
    public List<String> suggest(CommandInput input) {
        return filterSuggestions(input, EXAMPLE_PREFIXES);
    }
}
