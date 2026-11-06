package za.co.neroland.neroruins.terminal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class TerminalAccessTest {
    @Test
    void acceptsOnlyTheArchiveButtonAtAValidTerminalWithAKnownServerLog() {
        assertTrue(TerminalAccess.mayArchive(0, true, "neroruins:earth.1"));
        assertFalse(TerminalAccess.mayArchive(1, true, "neroruins:earth.1"));
        assertFalse(TerminalAccess.mayArchive(0, false, "neroruins:earth.1"));
        assertFalse(TerminalAccess.mayArchive(0, true, "neroruins:earth.7"));
        assertFalse(TerminalAccess.mayArchive(0, true, null));
    }
}
