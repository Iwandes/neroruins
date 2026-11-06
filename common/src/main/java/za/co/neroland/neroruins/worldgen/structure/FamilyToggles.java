package za.co.neroland.neroruins.worldgen.structure;

import java.util.function.BooleanSupplier;

import za.co.neroland.neroruins.config.NeroRuinsConfig;

/**
 * The per-family {@code worldgen.<family>.enabled} kill switches. A disabled family's hubs find no start,
 * and because a satellite only generates beside a valid hub, its satellites stop too. Already generated
 * chunks are unaffected.
 */
public final class FamilyToggles {
    private FamilyToggles() { }

    public static boolean enabled(String family) {
        BooleanSupplier toggle = switch (family) {
            case "lattice_temple" -> NeroRuinsConfig.LATTICE_TEMPLES::get;
            case "halcyon_ark" -> NeroRuinsConfig.HALCYON_ARKS::get;
            case "survey_lab" -> NeroRuinsConfig.SURVEY_LABS::get;
            case "colony_field" -> NeroRuinsConfig.COLONY_FIELDS::get;
            case "crash_site" -> NeroRuinsConfig.CRASH_SITES::get;
            case "bunker" -> NeroRuinsConfig.BUNKERS::get;
            default -> null;
        };
        return toggle != null && toggle.getAsBoolean();
    }
}
