package za.co.neroland.neroruins.map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.Test;
import za.co.neroland.nerolandcore.map.MapBounds;
import za.co.neroland.nerolandcore.map.MapLayerConformance;
import za.co.neroland.nerolandcore.map.MapLayerRegistry;
import za.co.neroland.nerolandcore.map.MapMarker;
import za.co.neroland.nerolandcore.map.MapQueryResult;
import za.co.neroland.nerolandcore.map.MapViewer;
import za.co.neroland.neroruins.data.ClaimLedger;
import za.co.neroland.neroruins.data.ExpeditionRecord;

/**
 * The ruins layer shows only the viewer's own discovered hubs: Core's conformance fixture with two viewers, a
 * third player's records, an absent dimension and far bounds, plus the claimed state and the omissions.
 * NeroRuins has no link module, so there is no map-versus-link parity test.
 */
class RuinsMapLayerProviderTest {
    private static final String OVERWORLD_ID = "minecraft:overworld";
    private static final ResourceKey<Level> OVERWORLD = dim("minecraft", "overworld");
    private static final ResourceKey<Level> MOON = dim("maptest", "moon");

    private static final UUID ENTITLED = UUID.randomUUID();
    private static final UUID STRANGER = UUID.randomUUID();
    private static final UUID THIRD = UUID.randomUUID();

    private static ResourceKey<Level> dim(String namespace, String path) {
        return ResourceKey.create(Registries.DIMENSION, Identifier.fromNamespaceAndPath(namespace, path));
    }

    private static ExpeditionRecord.Discovery hub(String dimension, int x, int z) {
        return new ExpeditionRecord.Discovery("neroruins:bunker_earth", "bunker", "outpost", dimension,
                Optional.of(new ExpeditionRecord.Hub(x, 60, z)), 1L);
    }

    private static ExpeditionRecord.Discovery hubless() {
        return new ExpeditionRecord.Discovery("neroruins:survey_lab_earth", "survey_lab", "complex", OVERWORLD_ID,
                Optional.empty(), 2L);
    }

    /** Fake per-player stores; only ever read by the viewer's own id. */
    private static final class Store implements RuinsMapSource {
        final Map<UUID, List<ExpeditionRecord.Discovery>> discoveries = new HashMap<>();
        final Map<UUID, List<ClaimLedger.Location>> claims = new HashMap<>();

        @Override
        public List<ExpeditionRecord.Discovery> discoveries(MapViewer viewer) {
            return discoveries.getOrDefault(viewer.id(), List.of());
        }

        @Override
        public Collection<ClaimLedger.Location> claims(MapViewer viewer) {
            return claims.getOrDefault(viewer.id(), List.of());
        }
    }

    private static Store seeded() {
        Store store = new Store();
        store.discoveries.put(ENTITLED, List.of(hub(OVERWORLD_ID, 40, 40), hub(OVERWORLD_ID, 100, -50), hubless(),
                hub("maptest:other", 10, 10)));
        store.claims.put(ENTITLED, List.of(new ClaimLedger.Location(OVERWORLD_ID, 110, 50, -45)));
        store.discoveries.put(STRANGER, List.of(hub(OVERWORLD_ID, -3000, 3000)));
        store.discoveries.put(THIRD, List.of(hub(OVERWORLD_ID, 60, 60)));
        // The third player claimed a vault at the entitled viewer's first ruin: that must not mark it claimed.
        store.claims.put(THIRD, List.of(new ClaimLedger.Location(OVERWORLD_ID, 41, 50, 41)));
        return store;
    }

    private static MapViewer online(UUID id, int x, int z) {
        return MapViewer.forTest(id, null, OVERWORLD, new BlockPos(x, 64, z));
    }

    @Test
    void passesCoreConformance() {
        MapLayerConformance.create(new RuinsMapLayerProvider(seeded()))
                .viewers(online(ENTITLED, 0, 0), online(STRANGER, 4, 0))
                .dimensions(OVERWORLD, MOON)
                .bounds(MapBounds.around(0, 0, 512))
                .farBounds(MapBounds.around(20_000, 20_000, 256))
                .forbiddenIds(THIRD)
                .forbiddenText("Trudy")
                .expectEntitledSees(RuinsMapLayers.RUINS, 2)
                .verify();
    }

    private static MapQueryResult query(Store store, MapViewer viewer, ResourceKey<Level> dimension, MapBounds bounds) {
        MapLayerRegistry.register(new RuinsMapLayerProvider(store));
        return MapLayerRegistry.query("neroruins", viewer, dimension, bounds);
    }

    @Test
    void showsOnlyPositionedHubsInThisWorldWithTheClaimedState() {
        MapQueryResult result = query(seeded(), online(ENTITLED, 0, 0), OVERWORLD, MapBounds.around(0, 0, 512));
        assertEquals(2, result.markers().size(), "hub-less and other-world discoveries are omitted");
        MapMarker first = result.markers().get(0);
        MapMarker second = result.markers().get(1);
        assertEquals(40, first.x(), "nearest first");
        assertTrue(first.icon() == null || RuinsMapLayers.ICON_RUIN.equals(first.icon()));
        assertEquals(1, first.details().size(), "another player's claim does not mark this ruin claimed");
        assertEquals(RuinsMapLayers.ICON_CLAIMED, second.icon(), "the viewer's own claim marks the ruin claimed");
        assertEquals("family.neroruins.bunker", first.label().text());
    }

    @Test
    void aClaimWithoutADiscoveredHubDrawsNothing() {
        Store store = new Store();
        store.claims.put(ENTITLED, List.of(new ClaimLedger.Location(OVERWORLD_ID, 5, 50, 5)));
        store.discoveries.put(ENTITLED, List.of(hubless()));
        assertTrue(query(store, online(ENTITLED, 0, 0), OVERWORLD, MapBounds.around(0, 0, 512)).markers().isEmpty());
    }

    @Test
    void boundsOnlyNarrowAndAnOfflineViewerStillSeesTheirOwnRecord() {
        Store store = seeded();
        assertTrue(query(store, online(ENTITLED, 0, 0), OVERWORLD, MapBounds.around(20_000, 20_000, 256))
                .markers().isEmpty());
        MapViewer offline = MapViewer.forTest(ENTITLED, null, null, null);
        assertEquals(2, query(store, offline, OVERWORLD, MapBounds.around(0, 0, 512)).markers().size());
    }
}
