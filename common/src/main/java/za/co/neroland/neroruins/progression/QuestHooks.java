package za.co.neroland.neroruins.progression;

import java.util.List;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import za.co.neroland.nerolandcore.event.CoreEvents;
import za.co.neroland.nerolandcore.progression.GateEvents.GateChange;
import za.co.neroland.nerolandcore.progression.GateScope;
import za.co.neroland.nerolandcore.progression.ProgressionGates;

import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.lifecycle.ServerState;

/**
 * The two <b>hook gates</b> a quest pack may name as a {@code mod_hook} reward, and what happens when
 * one opens.
 *
 * <p>A quest mod that wants to say "this player finished a quest of yours" opens the record gate
 * {@code neroruins:hook/<id>} for that player through Neroland Core — online or offline — and this
 * class listens on Core's gate events, applies the effect to that player, and closes the gate again to
 * re-arm it. Nothing here imports a quest mod; the pack is inert data and the only contract is Core's.
 *
 * <ul>
 *   <li>{@link #REVEAL_RUIN} — will give the player a one-use survey map to the nearest unclaimed ruin
 *       in their current dimension (shown only to them).</li>
 *   <li>{@link #UNLOCK_LOG} — will unlock one lore entry in the player's data-log archive.</li>
 * </ul>
 *
 * <p><b>Placeholder effects.</b> NeroRuins has no structures, survey maps or data-log archive yet, so
 * each hook currently sends the player one translatable chat line ({@code hook.neroruins.<id>.placeholder})
 * and re-arms. The real effects land with the content; {@link #apply} is the single seam to replace.
 *
 * <p>A hook that opens while the player is offline stays open; {@link #onJoin} delivers it the next time
 * they log in. Hook gates gate nothing and are never checked for access. Effects are idempotent per
 * opening and bounded: a quest completion is not a payment rail.
 *
 * <p><b>Privacy (POPIA/GDPR):</b> the change's target is a player UUID, used only to find the online
 * player and to re-arm the gate through Core; it is never logged.
 */
public final class QuestHooks {

    /** Hook gate: a survey map to the nearest unclaimed ruin for the completing player. */
    public static final Identifier REVEAL_RUIN =
            Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, "hook/reveal_ruin");

    /** Hook gate: one lore entry unlocked in the completing player's data-log archive. */
    public static final Identifier UNLOCK_LOG =
            Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, "hook/unlock_log");

    private static final List<Identifier> HOOKS = List.of(REVEAL_RUIN, UNLOCK_LOG);

    private QuestHooks() {
    }

    /** Registers the gate listener. Called once from common init; safe when Core's bus is quiet. */
    public static void init() {
        try {
            CoreEvents.onProgression(QuestHooks::onChange);
        } catch (RuntimeException | LinkageError e) {
            NeroRuinsCommon.LOGGER.warn(
                    "[NeroRuins] Could not register the quest-hook listener; quest rewards are off.", e);
        }
    }

    private static void onChange(GateChange change) {
        if (!change.open() || change.scope() != GateScope.PLAYER || !HOOKS.contains(change.gate())) {
            return;
        }
        MinecraftServer server = ServerState.currentServer();
        if (server == null) {
            return;
        }
        UUID id;
        try {
            id = UUID.fromString(change.target());
        } catch (IllegalArgumentException e) {
            return;
        }
        ServerPlayer player = server.getPlayerList().getPlayer(id);
        if (player == null) {
            return; // offline: the gate stays open and onJoin delivers it
        }
        deliver(player, change.gate());
    }

    /**
     * Called from each loader's player-join hook: delivers any hook that opened while the player was
     * offline. The {@code isOpen} reads are the whole cost on the common path.
     */
    public static void onJoin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            for (Identifier hook : HOOKS) {
                if (ProgressionGates.isOpen(player, hook)) {
                    deliver(player, hook);
                }
            }
        } catch (RuntimeException | LinkageError e) {
            warn(e);
        }
    }

    /** Applies the hook to the player and re-arms the gate. */
    private static void deliver(ServerPlayer player, Identifier hook) {
        try {
            apply(player, hook);
            // Re-arm. Closing fires a change with open=false, which onChange ignores.
            ProgressionGates.close(player, hook);
        } catch (RuntimeException | LinkageError e) {
            warn(e);
        }
    }

    /**
     * The effect itself. Placeholder until structures exist: one chat line to the player. Replace the
     * body per hook when survey maps ({@link #REVEAL_RUIN}) and the data-log archive ({@link #UNLOCK_LOG})
     * land; keep each effect bounded and idempotent per opening.
     */
    private static void apply(ServerPlayer player, Identifier hook) {
        String key = REVEAL_RUIN.equals(hook)
                ? "hook.neroruins.reveal_ruin.placeholder"
                : "hook.neroruins.unlock_log.placeholder";
        //? if >=26.1 {
        player.sendSystemMessage(Component.translatable(key));
        //?} else {
        /*player.displayClientMessage(Component.translatable(key), false);
        *///?}
    }

    private static void warn(Throwable e) {
        // Never who (POPIA/GDPR).
        NeroRuinsCommon.LOGGER.warn("[NeroRuins] A quest-hook reward failed; play is unaffected.", e);
    }
}
