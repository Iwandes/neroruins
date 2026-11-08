package za.co.neroland.neroruins.vault;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;
import za.co.neroland.neroruins.data.ClaimLedger;
import za.co.neroland.neroruins.data.VaultClaimPolicy;

/** {@code first_come} after a puzzle unseals a vault: the solver's held roll keeps everyone else out. */
class FirstComeTest {
    private static final ClaimLedger.Location VAULT = new ClaimLedger.Location("minecraft:overworld", 1, 2, 3);
    private static final VaultClaimPolicy FIRST_COME = VaultClaimPolicy.parse("first_come").orElseThrow();
    private static final VaultClaimPolicy PER_PLAYER = VaultClaimPolicy.parse("per_player").orElseThrow();

    private static ClaimLedger.Reservation roll(long at) {
        CompoundTag item = new CompoundTag();
        item.putString("id", "minecraft:diamond");
        return new ClaimLedger.Reservation("neroruins:chests/bunker_vault", at, null, List.of(item));
    }

    @Test
    void theSolversHeldRollBeatsSomeoneWaitingAtTheVault() {
        UUID solver = UUID.randomUUID();
        UUID waiting = UUID.randomUUID();
        var ledger = new ClaimLedger();
        assertTrue(ledger.reserve(VAULT, solver, FIRST_COME, 10, roll(10)), "the solver's roll is held on unseal");
        assertFalse(ledger.mayClaim(VAULT, waiting, FIRST_COME, 11));
        assertFalse(ledger.reserve(VAULT, waiting, FIRST_COME, 11, roll(11)));
        assertEquals(VaultRewardService.Result.TAKEN,
                VaultRewardService.refusal(FIRST_COME, ledger.lastClaim(VAULT, waiting) != null));
        assertTrue(ledger.complete(VAULT, solver, 12).isPresent(), "the solver still collects");
        assertFalse(ledger.mayClaim(VAULT, waiting, FIRST_COME, 13), "and the vault stays taken");
    }

    @Test
    void refusalsDistinguishBeingBeatenFromHavingClaimed() {
        assertEquals(VaultRewardService.Result.ALREADY_CLAIMED, VaultRewardService.refusal(FIRST_COME, true));
        assertEquals(VaultRewardService.Result.ALREADY_CLAIMED, VaultRewardService.refusal(PER_PLAYER, true));
        assertEquals(VaultRewardService.Result.ALREADY_CLAIMED, VaultRewardService.refusal(PER_PLAYER, false));
    }

    @Test
    void perPlayerVaultsStayOpenToEveryoneAfterUnsealing() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        var ledger = new ClaimLedger();
        assertTrue(ledger.reserve(VAULT, first, PER_PLAYER, 10, roll(10)));
        assertTrue(ledger.mayClaim(VAULT, second, PER_PLAYER, 11));
    }
}
