package za.co.neroland.neroruins.forge;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.Channel;
import net.minecraftforge.network.ChannelBuilder;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.payload.PayloadFlow;

import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.network.RuinsNetwork;
import za.co.neroland.neroruins.platform.NetworkPlatform;

/**
 * Forge side of the cross-loader packet seam, on NeroRuins' own {@code neroruins:main} channel.
 * Registered as the {@link NetworkPlatform} implementation via {@code META-INF/services}.
 *
 * <p>Forge seals a channel at {@code build()}, so every payload must be declared before then — which
 * is why NeroRuins owns this channel rather than adding to Neroland Core's, and why
 * {@link #register()} must run <b>after</b> {@code NeroRuinsCommon.init()}, where the payloads are
 * declared. {@code optional()} keeps a NeroRuins-less client connectable. While no payload is
 * declared the channel is not built; attempting to send then raises an error.
 */
public final class ForgeRuinsNetwork implements NetworkPlatform {

    private static Channel<CustomPacketPayload> channel;

    public static void register() {
        if (!RuinsNetwork.hasPayloads()) {
            NeroRuinsCommon.LOGGER.debug("[NeroRuins] no payloads declared; neroruins:main channel not built");
            return;
        }
        PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play =
                ChannelBuilder.named(Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID, RuinsNetwork.CHANNEL))
                        .optional()
                        .payloadChannel()
                        .play()
                        .bidirectional();
        for (RuinsNetwork.Clientbound<?> cb : RuinsNetwork.clientbound()) {
            registerClientbound(play, cb);
        }
        for (RuinsNetwork.Serverbound<?> sb : RuinsNetwork.serverbound()) {
            registerServerbound(play, sb);
        }
        channel = play.build();
    }

    private static <T extends CustomPacketPayload> void registerClientbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play,
            RuinsNetwork.Clientbound<T> cb) {
        play.addMain(cb.type(), registryCodec(cb.codec()),
                (payload, context) -> cb.handler().accept(payload));
    }

    /** Both directions share the one bidirectional flow; the sender is what distinguishes them. */
    private static <T extends CustomPacketPayload> void registerServerbound(
            PayloadFlow<RegistryFriendlyByteBuf, CustomPacketPayload> play,
            RuinsNetwork.Serverbound<T> sb) {
        play.addMain(sb.type(), registryCodec(sb.codec()), (payload, context) -> {
            if (context.getSender() instanceof ServerPlayer serverPlayer) {
                sb.handler().accept(payload, serverPlayer);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private static <T extends CustomPacketPayload> StreamCodec<RegistryFriendlyByteBuf, T> registryCodec(
            StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return (StreamCodec<RegistryFriendlyByteBuf, T>) codec;
    }

    @Override
    public boolean sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        if (channel == null) throw new IllegalStateException("NeroRuins network channel is not registered");
        if (!channel.isRemotePresent(player.connection.getConnection())) return false;
        channel.send(payload, PacketDistributor.PLAYER.with(player));
        return true;
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        if (channel == null) throw new IllegalStateException("NeroRuins network channel is not registered");
        channel.send(payload, PacketDistributor.SERVER.noArg());
    }
}
