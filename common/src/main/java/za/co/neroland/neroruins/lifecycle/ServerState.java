package za.co.neroland.neroruins.lifecycle;

import net.minecraft.server.MinecraftServer;

/**
 * The one place NeroRuins learns that a server started or stopped, invoked from each loader's own
 * lifecycle hook (the same shape the sibling Neroland mods use).
 *
 * <p>Neroland Core's gate events hand a listener a player UUID string and nothing else — no server,
 * no level — so code reacting to them needs somewhere to find the running server. {@link #serverStarted}
 * records it and {@link #serverStopped} forgets it, so a lookup before the first world is loaded (or
 * after the last one closed) honestly answers "nothing" instead of guessing from a stale reference.
 * In single-player the JVM outlives the world, which is why the reference must be cleared.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> nothing here holds, logs or clears player data.
 */
public final class ServerState {

    /**
     * The running server, or {@code null} between worlds. Written on the server thread from the
     * loader hooks; {@code volatile} so an integrated-server restart in the same JVM is seen at once.
     */
    private static volatile MinecraftServer currentServer;

    private ServerState() {
    }

    /** Records the server that has just finished starting. Called once per world load. */
    public static void serverStarted(MinecraftServer server) {
        za.co.neroland.neroruins.expedition.ExpeditionTracker.applyRetention(server);
        currentServer = server;
    }

    /** The running server, or {@code null} before the first world load / after the last one closed. */
    public static MinecraftServer currentServer() {
        return currentServer;
    }

    /** Forgets the server. Loaders fire it on the server thread after stop. */
    public static void serverStopped() {
        currentServer = null;
    }
}
