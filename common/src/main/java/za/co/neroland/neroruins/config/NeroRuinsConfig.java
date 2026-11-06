package za.co.neroland.neroruins.config;

import za.co.neroland.nerolandcore.config.ConfigManager;
import za.co.neroland.nerolandcore.config.ConfigSchema;
import za.co.neroland.nerolandcore.config.ConfigValue;

import za.co.neroland.neroruins.NeroRuinsCommon;

/**
 * NeroRuins config schema, built on Neroland Core's config framework (file
 * {@code config/neroruins.properties}, hot-reloadable via {@code /neroland config reload}).
 * Registered once from {@link NeroRuinsCommon#init()}, before anything else — every other
 * subsystem reads it, including telemetry's opt-out flag.
 *
 * <p>Gameplay defaults are declared here ahead of their systems; they take effect when those
 * systems are implemented.
 *
 * <p><b>POPIA/GDPR:</b> {@code telemetryEnabled} is deliberately <b>not</b> server-authoritative —
 * anonymous crash reporting is a per-client opt-out that a server must never force on or off.
 */
public final class NeroRuinsConfig {

    public static final ConfigSchema SCHEMA =
            ConfigSchema.create(NeroRuinsCommon.MOD_ID, "NeroRuins configuration.");

    // --- Crash telemetry (client-local opt-out) -----------------------------

    private static final ConfigValue<Boolean> TELEMETRY = SCHEMA.bool(
            "telemetryEnabled", true, false,
            "send anonymous, NeroRuins-only crash reports (Sentry, EU servers) - ON by default. Sends the "
                    + "stack trace, mod/MC/loader/OS/Java versions and your other installed mods' ids "
                    + "and versions; never your IP, username, UUID, world data or chat; file paths are "
                    + "scrubbed of your account name. Set false to opt out of all of it (takes effect "
                    + "on restart). See PRIVACY.md");

    public static final ConfigValue<Boolean> LATTICE_TEMPLES = SCHEMA.bool(
            "worldgen.latticeTemple.enabled", true, true, "Generate Lattice temples.");
    public static final ConfigValue<Boolean> HALCYON_ARKS = SCHEMA.bool(
            "worldgen.halcyonArk.enabled", true, true, "Generate Halcyon arks.");
    public static final ConfigValue<Boolean> SURVEY_LABS = SCHEMA.bool(
            "worldgen.surveyLab.enabled", true, true, "Generate survey labs.");
    public static final ConfigValue<Boolean> COLONY_FIELDS = SCHEMA.bool(
            "worldgen.colonyField.enabled", true, true, "Generate colony fields.");
    public static final ConfigValue<Boolean> CRASH_SITES = SCHEMA.bool(
            "worldgen.crashSite.enabled", true, true, "Generate crash sites.");
    public static final ConfigValue<Boolean> BUNKERS = SCHEMA.bool(
            "worldgen.bunker.enabled", true, true, "Generate bunkers.");
    public static final ConfigValue<Double> RELIC_RARITY = SCHEMA.doubleRange(
            "loot.relicRarity", 1.0, 0.25, 4.0, true, "Multiplier for relic drop chances.");
    public static final ConfigValue<Boolean> VAULTS_ENABLED = SCHEMA.bool(
            "vaults.enabled", true, true, "Enable vault interactions.");
    public static final ConfigValue<String> VAULT_MODE = SCHEMA.string(
            "vaults.mode", "per_player", true, "Vault claim policy: per_player, first_come, or timed(hours).");
    public static final ConfigValue<Boolean> GUARDIANS_ENABLED = SCHEMA.bool(
            "guardians.enabled", true, true, "Enable ruin guardians.");
    public static final ConfigValue<Boolean> TRAPS_ENABLED = SCHEMA.bool(
            "traps.enabled", true, true, "Enable ruin traps.");
    public static final ConfigValue<Integer> EXPEDITION_RETENTION_DAYS = SCHEMA.intRange(
            "privacy.expeditionRetentionDays", 365, 0, 36500, true,
            "Days to retain discovery positions; zero keeps positions until erasure.");
    public static final ConfigValue<Integer> CLAIM_RETENTION_DAYS = SCHEMA.intRange(
            "privacy.claimRetentionDays", 0, 0, 36500, true,
            "Days to retain vault claims; zero keeps claims until erasure.");
    public static final ConfigValue<String> ARCHIVE_RETENTION = SCHEMA.string(
            "privacy.archiveRetention", "until_erasure", true, "Archive retention policy.");
    public static final ConfigValue<Boolean> RECORD_POSITIONS = SCHEMA.bool(
            "privacy.recordDiscoveryPositions", true, true, "Keep a discoverer's own ruin positions.");
    public static final ConfigValue<Boolean> PUSH_NOTIFICATIONS = SCHEMA.bool(
            "pushNotifications.enabled", false, true, "Allow ruin chapter notifications.");
    public static final ConfigValue<Boolean> KEEP_SHARDS = SCHEMA.bool(
            "archive.keepShards", false, true, "Keep data shards after archiving their contents.");

    private NeroRuinsConfig() {
    }

    /**
     * Whether anonymous NeroRuins-only crash reporting is on (default true, opt-out). Read once at
     * bootstrap by {@code NeroRuinsTelemetry.init()}; changes take effect on restart.
     */
    public static boolean isTelemetryEnabled() {
        return TELEMETRY.get();
    }

    /** Registers the schema with Core's ConfigManager. Called once from common init. */
    public static void init() {
        ConfigManager.register(SCHEMA);
    }
}
