package za.co.neroland.neroruins.map;

import java.util.Collection;
import java.util.List;

import net.minecraft.server.MinecraftServer;
import za.co.neroland.nerolandcore.map.MapViewer;
import za.co.neroland.neroruins.config.NeroRuinsConfig;
import za.co.neroland.neroruins.data.ClaimLedger;
import za.co.neroland.neroruins.data.ExpeditionRecord;

/**
 * Where the provider reads one viewer's own records. It is only ever asked about the viewer's own UUID and has
 * no way to list another player's data. The server implementation reads the saved data already in memory.
 */
interface RuinsMapSource {

    /** The viewer's own discoveries. */
    List<ExpeditionRecord.Discovery> discoveries(MapViewer viewer);

    /** Locations of vaults the viewer has claimed. */
    Collection<ClaimLedger.Location> claims(MapViewer viewer);

    RuinsMapSource SERVER = new RuinsMapSource() {
        @Override
        public List<ExpeditionRecord.Discovery> discoveries(MapViewer viewer) {
            MinecraftServer server = viewer.server();
            // A server that has turned position recording off shows no positions at all, even before the
            // hourly sweep has dropped the ones already stored.
            if (server == null || !NeroRuinsConfig.RECORD_POSITIONS.get()) return List.of();
            return ExpeditionRecord.get(server).export(viewer.id());
        }

        @Override
        public Collection<ClaimLedger.Location> claims(MapViewer viewer) {
            MinecraftServer server = viewer.server();
            if (server == null) return List.of();
            return ClaimLedger.get(server).export(viewer.id()).keySet();
        }
    };
}
