package za.co.neroland.neroruins.guardian;

/**
 * The guardian rules that need no world: when a spawner wakes and spawns, its budget, when the
 * Sentinel may shoot, and when an arc grate discharges. Kept free of Minecraft types so the rules are
 * unit-tested directly.
 */
public final class GuardianRules {
    /** Spawner activation radius in blocks, measured from the spawner's centre. */
    public static final int SPAWNER_RANGE = 8;
    /** Hostile mobs (from any source) allowed alive around a spawner before it holds off. */
    public static final int MAX_NEARBY = 4;
    /** Sentinel charged shot: wind-up, cooldown (ticks) and reach (blocks). */
    public static final int WIND_UP_TICKS = 20;
    public static final int SHOT_COOLDOWN_TICKS = 60;
    public static final double SHOT_MIN_RANGE = 4.0;
    public static final double SHOT_MAX_RANGE = 16.0;

    private GuardianRules() { }

    /** Lifetime guardians for a spawner of this tier (clamped to 1–3): 4, 6 or 8. */
    public static int initialBudget(int tier) {
        return 2 + 2 * Math.max(1, Math.min(SpawnTable.MAX_TIER, tier));
    }

    /** Whether a player wakes a spawner: survival or adventure, and within range. */
    public static boolean wakes(boolean spectator, boolean creative, double distanceSquared) {
        return !spectator && !creative && distanceSquared <= (double) SPAWNER_RANGE * SPAWNER_RANGE;
    }

    /** Whether a spawner should try to spawn now. */
    public static boolean shouldSpawn(int remaining, boolean peaceful, boolean playerInRange, int nearbyHostiles) {
        return remaining > 0 && !peaceful && playerInRange && nearbyHostiles < MAX_NEARBY;
    }

    /** Whether a Sentinel may start a charged shot at this squared distance. */
    public static boolean inShotRange(double distanceSquared) {
        return distanceSquared >= SHOT_MIN_RANGE * SHOT_MIN_RANGE && distanceSquared <= SHOT_MAX_RANGE * SHOT_MAX_RANGE;
    }

    /** Whether a grate discharges into whoever stepped on it: it must be charged and they a survival player. */
    public static boolean discharges(boolean charged, boolean player, boolean spectator, boolean creative) {
        return charged && player && !spectator && !creative;
    }
}
