package za.co.neroland.neroruins.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class CodexSnapshotTest {
    @Test
    void includesOnlyKnownArchivedLogsInStoryOrder() {
        var snapshot = CodexSnapshot.fromArchive(Set.of("neroruins:luna.1", "neroruins:earth.2", "other:private"));
        assertEquals(List.of("neroruins:earth.2", "neroruins:luna.1"), snapshot.logs());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.logs().clear());
    }

    @Test
    void rejectsUnknownAndDuplicateLogs() {
        assertThrows(IllegalArgumentException.class, () -> new CodexSnapshot(List.of("neroruins:earth.7")));
        assertThrows(IllegalArgumentException.class,
                () -> new CodexSnapshot(List.of("neroruins:earth.1", "neroruins:earth.1")));
        assertThrows(IllegalArgumentException.class,
                () -> new CodexSnapshot(java.util.Collections.nCopies(55, "neroruins:earth.1")));
    }
}
