package za.co.neroland.neroruins.expedition;

import java.util.function.Consumer;

import za.co.neroland.neroruins.NeroRuinsCommon;

/** Client hook for the Expedition Map screen; the client entry point installs the real handler. */
public final class ExpeditionMapDisplay {
    private static Consumer<ExpeditionSnapshot> opener = snapshot ->
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Expedition Map display is unavailable on this client");

    private ExpeditionMapDisplay() { }

    public static void install(Consumer<ExpeditionSnapshot> handler) { opener = handler; }

    public static void open(ExpeditionSnapshot snapshot) { opener.accept(snapshot); }
}
