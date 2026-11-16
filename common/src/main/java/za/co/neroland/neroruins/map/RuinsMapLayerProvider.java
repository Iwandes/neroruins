package za.co.neroland.neroruins.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import za.co.neroland.nerolandcore.map.MapBounds;
import za.co.neroland.nerolandcore.map.MapLabel;
import za.co.neroland.nerolandcore.map.MapLayerInfo;
import za.co.neroland.nerolandcore.map.MapLayerProvider;
import za.co.neroland.nerolandcore.map.MapLayerSink;
import za.co.neroland.nerolandcore.map.MapMarker;
import za.co.neroland.nerolandcore.map.MapScope;
import za.co.neroland.nerolandcore.map.MapViewer;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.data.ClaimLedger;
import za.co.neroland.neroruins.data.ExpeditionRecord;
import za.co.neroland.neroruins.expedition.RuinLocator;

/**
 * {@code neroruins:ruins} ({@link MapScope#DISCOVERED}): one marker per ruin hub in the viewer's own Expedition
 * Record that still carries a position. Hub-less discoveries (position recording off, or dropped by retention)
 * are omitted, never estimated. A hub within {@link RuinLocator#SAME_SITE_BLOCKS} of a vault the viewer claimed
 * is drawn in its claimed state; claims alone never produce a marker, and other claimers never appear.
 *
 * <p>Labels are lang keys (ruin family, size class), never a player's name. Markers are emitted nearest to the
 * viewer first, so a capped result is stable between refreshes.
 */
final class RuinsMapLayerProvider implements MapLayerProvider {

    private static final List<MapLayerInfo> LAYERS = List.of(MapLayerInfo.of(RuinsMapLayers.RUINS,
            "map.layer.neroruins.ruins", RuinsMapLayers.ICON_RUIN, MapScope.DISCOVERED));

    private final RuinsMapSource source;

    RuinsMapLayerProvider(RuinsMapSource source) {
        this.source = source;
    }

    @Override
    public String modId() {
        return NeroRuinsCommon.MOD_ID;
    }

    @Override
    public List<MapLayerInfo> layers() {
        return LAYERS;
    }

    @Override
    public void query(MapViewer viewer, ResourceKey<Level> dimension, MapBounds bounds, MapLayerSink sink) {
        if (viewer == null || dimension == null || bounds == null || sink.isFull(RuinsMapLayers.RUINS)) return;
        String dim = dimension.identifier().toString();
        List<ExpeditionRecord.Discovery> hubs = new ArrayList<>();
        for (ExpeditionRecord.Discovery discovery : source.discoveries(viewer)) {
            if (!discovery.dimension().equals(dim) || discovery.hub().isEmpty()) continue;
            ExpeditionRecord.Hub hub = discovery.hub().get();
            if (!bounds.contains(hub.x(), hub.z())) continue;
            hubs.add(discovery);
        }
        if (hubs.isEmpty()) return;
        Collection<ClaimLedger.Location> claims = source.claims(viewer);
        int originX = viewer.position().map(BlockPos::getX).orElse(bounds.centreX());
        int originZ = viewer.position().map(BlockPos::getZ).orElse(bounds.centreZ());
        hubs.sort(Comparator.comparingLong(d -> distanceSq(d.hub().get(), originX, originZ)));
        for (ExpeditionRecord.Discovery discovery : hubs) {
            if (sink.isFull(RuinsMapLayers.RUINS)) return;
            ExpeditionRecord.Hub hub = discovery.hub().get();
            boolean claimed = claimedAt(claims, dim, hub);
            MapMarker.Builder marker = MapMarker.builder(markerId(hub), RuinsMapLayers.RUINS, dimension,
                    hub.x(), hub.y(), hub.z())
                    .label(MapLabel.translatable("family.neroruins." + discovery.family()))
                    .detail(MapLabel.translatable("size.neroruins." + discovery.sizeClass()));
            if (claimed) {
                marker.icon(RuinsMapLayers.ICON_CLAIMED).detail(MapLabel.translatable("map.neroruins.ruin.claimed"));
            }
            sink.marker(marker.build());
        }
    }

    /** Stable within the layer and free of identity: the hub's own block position. */
    static String markerId(ExpeditionRecord.Hub hub) {
        return "ruin/" + hub.x() + "_" + hub.y() + "_" + hub.z();
    }

    /** Whether one of the viewer's own claims lies at this ruin (same world, within the same-site distance). */
    static boolean claimedAt(Collection<ClaimLedger.Location> claims, String dimension, ExpeditionRecord.Hub hub) {
        long limit = (long) RuinLocator.SAME_SITE_BLOCKS * RuinLocator.SAME_SITE_BLOCKS;
        for (ClaimLedger.Location claim : claims) {
            if (!claim.dimension().equals(dimension)) continue;
            long dx = (long) claim.x() - hub.x();
            long dz = (long) claim.z() - hub.z();
            if (dx * dx + dz * dz <= limit) return true;
        }
        return false;
    }

    private static long distanceSq(ExpeditionRecord.Hub hub, int x, int z) {
        long dx = (long) hub.x() - x;
        long dz = (long) hub.z() - z;
        return dx * dx + dz * dz;
    }
}
