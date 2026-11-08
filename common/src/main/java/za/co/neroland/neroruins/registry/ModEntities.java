package za.co.neroland.neroruins.registry;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import za.co.neroland.nerolandcore.entity.EntityRegistrationSupport;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.guardian.Sentinel;
import za.co.neroland.neroruins.platform.Services;

/**
 * NeroRuins' entity types: the three Sentinel marks. Attributes ride Core's
 * {@link EntityRegistrationSupport} seam; {@link #registerEntitySupport()} must run after
 * {@link #init()} because Fabric applies attributes the moment they are declared.
 */
public final class ModEntities {
    private static final RegistrationProvider<EntityType<?>> TYPES =
            Services.REGISTRATION.provider(Registries.ENTITY_TYPE, NeroRuinsCommon.MOD_ID);

    public static final RegistrationProvider.RegistryEntry<EntityType<Sentinel>> SENTINEL_MK1 = sentinel("sentinel_mk1", Sentinel.Mark.MK1);
    public static final RegistrationProvider.RegistryEntry<EntityType<Sentinel>> SENTINEL_MK2 = sentinel("sentinel_mk2", Sentinel.Mark.MK2);
    public static final RegistrationProvider.RegistryEntry<EntityType<Sentinel>> SENTINEL_MK3 = sentinel("sentinel_mk3", Sentinel.Mark.MK3);

    /** Every Sentinel type, in mark order. */
    public static final List<RegistrationProvider.RegistryEntry<EntityType<Sentinel>>> SENTINELS =
            List.of(SENTINEL_MK1, SENTINEL_MK2, SENTINEL_MK3);

    private ModEntities() { }

    private static RegistrationProvider.RegistryEntry<EntityType<Sentinel>> sentinel(String name, Sentinel.Mark mark) {
        float scale = mark.scale();
        return TYPES.register(name, key -> EntityType.Builder.<Sentinel>of(
                        (type, level) -> new Sentinel(type, level, mark), MobCategory.MONSTER)
                .sized(0.9F * scale, 1.9F * scale).eyeHeight(1.6F * scale).clientTrackingRange(10).build(key));
    }

    public static void init() { }

    public static void registerEntitySupport() {
        EntityRegistrationSupport entities = EntityRegistrationSupport.get(NeroRuinsCommon.MOD_ID);
        entities.registerAttributes(SENTINEL_MK1, Sentinel.Mark.MK1::attributes);
        entities.registerAttributes(SENTINEL_MK2, Sentinel.Mark.MK2::attributes);
        entities.registerAttributes(SENTINEL_MK3, Sentinel.Mark.MK3::attributes);
    }
}
