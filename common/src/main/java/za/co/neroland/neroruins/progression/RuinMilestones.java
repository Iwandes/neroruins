package za.co.neroland.neroruins.progression;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.event.ThresholdEvents;
import za.co.neroland.nerolandcore.event.ThresholdEvents.ThresholdCrossing;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.lore.LogCatalog;

/**
 * The progression signals NeroRuins publishes through Neroland Core, in one place, so future structure
 * code only has to call a method here and the quest pack that ships with this jar needs no further code.
 *
 * <h2>Personal milestones — player-scope record gates</h2>
 *
 * <p>Each gate is a data file under {@code data/neroruins/neroland_gates/} with {@code "scope": "player"}
 * and no {@code requires}. They <em>record</em> that a player did something; they gate nothing and are
 * never checked for access (vault locks and puzzle doors keep their own logic).
 *
 * <table>
 *   <tr><th>Gate</th><th>Call</th><th>When</th></tr>
 *   <tr><td>{@code neroruins:first_log}</td><td>{@link #firstLog}</td>
 *       <td>a player reads a data log for the first time</td></tr>
 *   <tr><td>{@code neroruins:first_vault}</td><td>{@link #firstVault}</td>
 *       <td>a player opens a locked vault</td></tr>
 *   <tr><td>{@code neroruins:puzzle_solver}</td><td>{@link #puzzleSolver}</td>
 *       <td>a puzzle door opens for the player who completed it</td></tr>
 *   <tr><td>{@code neroruins:codex/<chapter>}</td><td>{@link #chapterComplete}</td>
 *       <td>a player archives the sixth log of a chapter (one gate per chapter)</td></tr>
 *   <tr><td>{@code neroruins:temple_cleared}</td><td>{@link #templeCleared}</td>
 *       <td>the last guardian of a temple dies — call once per player who damaged it</td></tr>
 * </table>
 *
 * <p>Opening an already-open gate is a no-op in Core, so callers need not de-duplicate.
 *
 * <h2>World milestone — the {@code neroruins:vault_claimed} channel</h2>
 *
 * <p>{@link #vaultClaimed} counts claimed vaults per structure <em>type</em> in the {@link VaultLedger}
 * and fires a {@link ThresholdCrossing} on Core's bus each time that type's count reaches a rung
 * ({@link #RUNGS}: 1, 10, 50). The crossing's scope is the structure type id (for example
 * {@code neroruins:lava_temple}); its value is the type's new count. A crossing can never credit one
 * player, which is exactly why the personal milestones above are gates instead.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the channel scope is a structure type, never a player name, a UUID
 * or coordinates (a ruin's position plus a time can locate a player). The player reference goes only
 * into {@code ProgressionGates.open}. Nothing here logs who.
 *
 * <p>Every method is safe to call from gameplay code: a Core failure is logged (without the player) and
 * swallowed, so a progression hiccup never breaks a vault or a door.
 */
public final class RuinMilestones {

    /** Record gate: a player read their first data log. */
    public static final Identifier FIRST_LOG = id("first_log");
    /** Record gate: a player opened their first locked vault. */
    public static final Identifier FIRST_VAULT = id("first_vault");
    /** Record gate: a player solved a puzzle door. */
    public static final Identifier PUZZLE_SOLVER = id("puzzle_solver");
    /** Record gate: a player helped clear a temple of its guardians. */
    public static final Identifier TEMPLE_CLEARED = id("temple_cleared");

    /** Threshold channel: vaults claimed in this world, per structure type (scope = structure type id). */
    public static final Identifier VAULT_CLAIMED = id("vault_claimed");

    /** The per-type claimed counts at which {@link #VAULT_CLAIMED} fires. */
    public static final long[] RUNGS = {1L, 10L, 50L};

    private RuinMilestones() {
    }

    /** Records that {@code player} read a data log (opens {@link #FIRST_LOG}). */
    public static void firstLog(ServerPlayer player) {
        open(player, FIRST_LOG);
    }

    /** Records that {@code player} opened a locked vault (opens {@link #FIRST_VAULT}). */
    public static void firstVault(ServerPlayer player) {
        open(player, FIRST_VAULT);
    }

    /** Records that {@code player} solved a puzzle door (opens {@link #PUZZLE_SOLVER}). */
    public static void puzzleSolver(ServerPlayer player) {
        open(player, PUZZLE_SOLVER);
    }

    /**
     * The record gate for a completed Codex chapter: {@code neroruins:codex/<chapter>}.
     *
     * @throws IllegalArgumentException if {@code chapter} is not one of {@link LogCatalog#chapters()}
     */
    public static Identifier codexGate(String chapter) {
        if (!LogCatalog.chapters().contains(chapter)) {
            throw new IllegalArgumentException("Unknown Codex chapter");
        }
        return id("codex/" + chapter);
    }

    /** Records that {@code player} archived every log of {@code chapter} (opens its {@link #codexGate}). */
    public static void chapterComplete(ServerPlayer player, String chapter) {
        open(player, codexGate(chapter));
    }

    /** Records that {@code player} helped clear a temple (opens {@link #TEMPLE_CLEARED}). */
    public static void templeCleared(ServerPlayer player) {
        open(player, TEMPLE_CLEARED);
    }

    /**
     * Records that a vault of {@code structureTypeId} was claimed anywhere in the world, persists the
     * new per-type count and fires {@link #VAULT_CLAIMED} for every rung the count just reached.
     *
     * @param server          the running server (the ledger lives in its overworld)
     * @param structureTypeId the structure <em>type</em>, e.g. {@code neroruins:lava_temple} — never a
     *                        position
     * @return the type's claimed count after this call, or {@code 0} if nothing could be recorded
     */
    public static long vaultClaimed(MinecraftServer server, Identifier structureTypeId) {
        if (server == null || structureTypeId == null) {
            return 0L;
        }
        try {
            VaultLedger ledger = VaultLedger.get(server);
            long before = ledger.count(structureTypeId);
            long after = ledger.increment(structureTypeId);
            for (long rung : RUNGS) {
                if (before < rung && after >= rung) {
                    ThresholdEvents.fire(NeroRuinsCommon.MOD_ID, new ThresholdCrossing(
                            VAULT_CLAIMED, structureTypeId.toString(), after, rung, true));
                }
            }
            return after;
        } catch (RuntimeException | LinkageError e) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Could not record a claimed vault of {}.", structureTypeId, e);
            return 0L;
        }
    }

    private static void open(ServerPlayer player, Identifier gate) {
        if (player == null) {
            return;
        }
        try {
            ProgressionGates.open(player, gate);
        } catch (RuntimeException | LinkageError e) {
            // The gate id only — never the player (POPIA/GDPR).
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Could not record the milestone {}.", gate, e);
        }
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, path);
    }
}
