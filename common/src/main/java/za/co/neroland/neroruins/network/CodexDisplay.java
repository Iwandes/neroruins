package za.co.neroland.neroruins.network;

import java.util.function.Consumer;

import za.co.neroland.neroruins.NeroRuinsCommon;

public final class CodexDisplay {
    private static Consumer<CodexSnapshot> opener = snapshot ->
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Codex display is unavailable on this client");

    private CodexDisplay() { }

    public static void install(Consumer<CodexSnapshot> handler) { opener = handler; }

    public static void open(CodexSnapshot snapshot) { opener.accept(snapshot); }
}
