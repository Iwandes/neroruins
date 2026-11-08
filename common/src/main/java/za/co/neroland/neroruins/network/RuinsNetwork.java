package za.co.neroland.neroruins.network;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.concurrent.atomic.AtomicBoolean;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class RuinsNetwork {
    public record Clientbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) { }
    public record Serverbound<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) { }

    public static final String CHANNEL = "main";
    public static final String PROTOCOL_VERSION = "1";
    private static final List<Clientbound<?>> CLIENTBOUND = new ArrayList<>();
    private static final List<Serverbound<?>> SERVERBOUND = new ArrayList<>();
    private static final AtomicBoolean DECLARED = new AtomicBoolean();

    private RuinsNetwork() { }

    public static <T extends CustomPacketPayload> void clientbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, Consumer<T> handler) {
        CLIENTBOUND.add(new Clientbound<>(type, codec, handler));
    }

    public static <T extends CustomPacketPayload> void serverbound(CustomPacketPayload.Type<T> type,
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec, BiConsumer<T, ServerPlayer> handler) {
        SERVERBOUND.add(new Serverbound<>(type, codec, handler));
    }

    public static List<Clientbound<?>> clientbound() { return Collections.unmodifiableList(CLIENTBOUND); }
    public static List<Serverbound<?>> serverbound() { return Collections.unmodifiableList(SERVERBOUND); }
    public static boolean hasPayloads() { return !CLIENTBOUND.isEmpty() || !SERVERBOUND.isEmpty(); }

    public static void init() {
        if (DECLARED.compareAndSet(false, true)) {
            clientbound(CodexSnapshot.TYPE, CodexSnapshot.CODEC, CodexDisplay::open);
            clientbound(za.co.neroland.neroruins.expedition.ExpeditionSnapshot.TYPE,
                    za.co.neroland.neroruins.expedition.ExpeditionSnapshot.CODEC,
                    za.co.neroland.neroruins.expedition.ExpeditionMapDisplay::open);
        }
    }
}
