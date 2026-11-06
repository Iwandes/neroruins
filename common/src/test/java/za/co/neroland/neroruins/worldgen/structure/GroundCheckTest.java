package za.co.neroland.neroruins.worldgen.structure;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class GroundCheckTest {
    @Test
    void acceptsDryGentleGroundAndRejectsWaterOrSteepSlopes() {
        GroundCheck check = new GroundCheck(32, 5, true, 8, 4);
        assertTrue(check.accepts(new int[] { 72, 74, 76 }, new int[] { 72, 74, 76 }, -64, 384));
        assertTrue(check.accepts(new int[] { 3, 3, 3 }, new int[] { 3, 3, 3 }, -64, 384));
        assertFalse(check.accepts(new int[] { -60, -60, -60 }, new int[] { -60, -60, -60 }, -64, 384));
        assertFalse(check.accepts(new int[] { 318 }, new int[] { 318 }, -64, 384));
        assertFalse(check.accepts(new int[] { 63, 74, 76 }, new int[] { 55, 74, 76 }, -64, 384));
        assertFalse(check.accepts(new int[] { 72, 74, 80 }, new int[] { 72, 74, 80 }, -64, 384));
        assertFalse(check.accepts(new int[] { 72, 74, 76 }, new int[] { 71, 74, 76 }, -64, 384));
    }

    @Test
    void rejectsMissingOrMismatchedSamples() {
        GroundCheck check = new GroundCheck(16, 4, true, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> check.accepts(null, new int[] { 1 }, 0, 384));
        assertThrows(IllegalArgumentException.class, () -> check.accepts(new int[] { 1 }, new int[0], 0, 384));
        assertThrows(IllegalArgumentException.class, () -> check.accepts(new int[] { 1 }, new int[] { 1 }, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new GroundCheck(65, 4, true, 0, 0));
    }

    @Test
    void rejectsOutOfRangeDatapackConstraints() {
        var invalid = JsonParser.parseString("{\"radius\":65,\"max_height_delta\":5}");
        assertTrue(GroundCheck.CODEC.parse(JsonOps.INSTANCE, invalid).error().isPresent());
        var valid = JsonParser.parseString("{\"radius\":32,\"max_height_delta\":5,\"min_depth\":8}");
        assertTrue(GroundCheck.CODEC.parse(JsonOps.INSTANCE, valid).result().isPresent());
    }
}
