package za.co.neroland.neroruins.registry;

import net.minecraft.core.registries.Registries;
import za.co.neroland.nerolandcore.registry.RegistrationProvider;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.platform.Services;
import za.co.neroland.neroruins.puzzle.PuzzleSeedProcessor;

//? if >=26.2 {
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
//?} else {
/*import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
*///?}

public final class ModProcessors {
    //? if >=26.2 {
    private static final RegistrationProvider<MapCodec<? extends StructureProcessor>> PROCESSORS =
            Services.REGISTRATION.provider(Registries.STRUCTURE_PROCESSOR, NeroRuinsCommon.MOD_ID);
    public static final RegistrationProvider.RegistryEntry<MapCodec<PuzzleSeedProcessor>> PUZZLE_SEED =
            PROCESSORS.register("puzzle_seed", key -> PuzzleSeedProcessor.CODEC);
    //?} else {
    /*private static final RegistrationProvider<StructureProcessorType<?>> PROCESSORS =
            Services.REGISTRATION.provider(Registries.STRUCTURE_PROCESSOR, NeroRuinsCommon.MOD_ID);
    public static final RegistrationProvider.RegistryEntry<StructureProcessorType<PuzzleSeedProcessor>> PUZZLE_SEED =
            PROCESSORS.register("puzzle_seed", key -> () -> PuzzleSeedProcessor.CODEC);
    *///?}

    private ModProcessors() { }

    public static void init() { }
}
