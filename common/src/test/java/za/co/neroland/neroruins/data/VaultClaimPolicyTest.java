package za.co.neroland.neroruins.data;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class VaultClaimPolicyTest {
    @Test
    void perPlayerAndFirstComeHaveDifferentClaimScopes() {
        var personal = VaultClaimPolicy.parse("per_player").orElseThrow();
        assertTrue(personal.allows(true, null, 100));
        assertFalse(personal.allows(true, 50L, 100));
        var first = VaultClaimPolicy.parse("first_come").orElseThrow();
        assertTrue(first.allows(false, null, 100));
        assertFalse(first.allows(true, null, 100));
    }

    @Test
    void timedPolicyRequiresTheFullIntervalAndRejectsClockRollback() {
        var policy = VaultClaimPolicy.parse("timed(2)").orElseThrow();
        assertTrue(policy.allows(false, null, 0));
        assertFalse(policy.allows(true, 1000L, 7_200_999L));
        assertTrue(policy.allows(true, 1000L, 7_201_000L));
        assertFalse(policy.allows(true, 1000L, 999));
        assertFalse(policy.allows(false, null, -1));
    }

    @Test
    void malformedAndUnboundedPoliciesAreRejected() {
        for (String invalid : new String[] {"timed", "timed(0)", "timed(-1)", "timed(99999999)", "unknown", ""}) {
            assertTrue(VaultClaimPolicy.parse(invalid).isEmpty());
        }
        assertTrue(VaultClaimPolicy.parse(null).isEmpty());
    }
}
