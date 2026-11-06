package za.co.neroland.neroruins.worldgen.structure;

import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import za.co.neroland.neroruins.registry.ModWorldgen;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.worldgen.placement.SatellitePlacement;

public final class SatelliteStructure extends Structure {
    public static final MapCodec<SatelliteStructure> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            settingsCodec(instance),
            JigsawStructure.CODEC.codec().fieldOf("layout").forGetter(satellite -> satellite.layout),
            SatellitePlacement.CODEC.codec().fieldOf("placement").forGetter(satellite -> satellite.placement)
    ).apply(instance, SatelliteStructure::new));

    private final JigsawStructure layout;
    private final SatellitePlacement placement;
    private static final AtomicBoolean INVALID_HUB_REPORTED = new AtomicBoolean();
    /**
     * One structure state per dimension's {@link RandomState}, the same scope vanilla keeps its own in.
     * Building a state filters every structure set against the biome source, which is too costly to repeat
     * for each satellite candidate. Weak keys release it with the dimension.
     */
    private static final Map<RandomState, ChunkGeneratorStructureState> STATES = new WeakHashMap<>();

    public SatelliteStructure(StructureSettings settings, JigsawStructure layout, SatellitePlacement placement) {
        super(settings);
        this.layout = layout;
        this.placement = placement;
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        var sets = context.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET);
        ChunkGeneratorStructureState state;
        synchronized (STATES) {
            state = STATES.computeIfAbsent(context.randomState(),
                    randomState -> context.chunkGenerator().createState(sets, randomState, context.seed()));
        }
        int x = context.chunkPos().getMinBlockX() >> 4;
        int z = context.chunkPos().getMinBlockZ() >> 4;
        Optional<ChunkPos> anchor = placement.findHub(state, x, z);
        if (anchor.isEmpty()) return Optional.empty();
        Optional<Holder<StructureSet>> set = state.possibleStructureSets().stream()
                .filter(candidate -> candidate.is(placement.hubSet())).findFirst();
        if (set.isEmpty()) return Optional.empty();
        if (set.get().value().structures().size() != 1) {
            reportInvalidHub();
            return Optional.empty();
        }
        Structure structure = set.get().value().structures().get(0).structure().value();
        if (!(structure instanceof ComplexHub hub) || !hub.family().equals(placement.family())) {
            reportInvalidHub();
            return Optional.empty();
        }

        //? if >=26.3 {
        GenerationContext hubContext = new GenerationContext(context.registryAccess(), context.chunkGenerator(),
                context.biomeSource(), context.climateSampler(), context.randomState(), context.structureTemplateManager(),
                context.seed(), anchor.get(), context.heightAccessor(), hub.biomes()::contains);
        //?} else {
        /*GenerationContext hubContext = new GenerationContext(context.registryAccess(), context.chunkGenerator(),
                context.biomeSource(), context.randomState(), context.structureTemplateManager(),
                context.seed(), anchor.get(), context.heightAccessor(), hub.biomes()::contains);
        *///?}
        Optional<GenerationStub> start = hub.findValidGenerationPoint(hubContext);
        if (start.isEmpty() || start.get().getPiecesBuilder().isEmpty()) return Optional.empty();
        return layout.findGenerationPoint(context);
    }

    @Override
    public StructureType<?> type() { return ModWorldgen.SATELLITE_STRUCTURE.get(); }

    private static void reportInvalidHub() {
        if (INVALID_HUB_REPORTED.compareAndSet(false, true)) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] A satellite hub set must contain exactly one matching complex hub");
        }
    }
}
