package za.co.neroland.neroruins.forge;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.neroruins.platform.RegistrationPlatform;

public final class ForgeRegistrationPlatform implements RegistrationPlatform {
    public <T> RegistrationProvider<T> provider(ResourceKey<? extends Registry<T>> registry, String modId) {
        return RegistrationProvider.get(registry, modId);
    }

    public void attach(Object modBus) {
        RegistrationProvider.attach(modBus);
    }
}