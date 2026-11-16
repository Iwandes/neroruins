package za.co.neroland.neroruins.map;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import za.co.neroland.nerolandcore.map.MapLayerRegistry;
import za.co.neroland.neroruins.NeroRuinsCommon;

/**
 * NeroRuins' map layers for any Neroland map consumer (Neroland Core's map API). One layer, {@code neroruins:ruins}:
 * the hubs of ruin complexes the viewer has entered themself, from their own Expedition Record, shown only while
 * that record still holds the hub position. A ruin where the viewer has claimed a vault is drawn in its claimed
 * state. Nothing is shown to anyone else, and NeroRuins stores nothing new for it.
 */
public final class RuinsMapLayers {

    private static final Logger LOGGER = LoggerFactory.getLogger("NeroRuins/Map");

    public static final Identifier RUINS = id("ruins");
    static final Identifier ICON_RUIN = id("ruin");
    static final Identifier ICON_CLAIMED = id("vault_claimed");

    private RuinsMapLayers() {
    }

    /** Registers the provider with Core; never throws. Called once from {@code NeroRuinsCommon.init()}. */
    public static void register() {
        try {
            MapLayerRegistry.register(new RuinsMapLayerProvider(RuinsMapSource.SERVER));
        } catch (RuntimeException | LinkageError e) {
            LOGGER.warn("NeroRuins map layers not registered ({})", e.getClass().getSimpleName());
        }
    }

    static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, path);
    }
}
