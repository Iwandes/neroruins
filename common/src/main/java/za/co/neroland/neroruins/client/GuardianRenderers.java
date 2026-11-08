package za.co.neroland.neroruins.client;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import za.co.neroland.neroruins.guardian.Sentinel;
import za.co.neroland.neroruins.registry.ModEntities;

/** The Sentinel renderers, named once; each loader passes its own registration call as a {@link Sink}. */
public final class GuardianRenderers {
    public interface Sink {
        <E extends Entity> void register(EntityType<? extends E> type, EntityRendererProvider<E> provider);
    }

    private GuardianRenderers() { }

    public static void registerAll(Sink sink) {
        sink.register(ModEntities.SENTINEL_MK1.get(), context -> new SentinelRenderer(context, Sentinel.Mark.MK1));
        sink.register(ModEntities.SENTINEL_MK2.get(), context -> new SentinelRenderer(context, Sentinel.Mark.MK2));
        sink.register(ModEntities.SENTINEL_MK3.get(), context -> new SentinelRenderer(context, Sentinel.Mark.MK3));
    }
}
