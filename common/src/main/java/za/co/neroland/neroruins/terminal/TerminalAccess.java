package za.co.neroland.neroruins.terminal;

import za.co.neroland.neroruins.lore.LogCatalog;

public final class TerminalAccess {
    private TerminalAccess() { }

    public static boolean mayArchive(int button, boolean validTerminal, String serverLogId) {
        return button == 0 && validTerminal && LogCatalog.translationPrefix(serverLogId).isPresent();
    }
}
