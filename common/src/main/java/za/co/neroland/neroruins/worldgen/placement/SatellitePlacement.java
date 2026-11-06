package za.co.neroland.neroruins.worldgen.placement;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import za.co.neroland.neroruins.NeroRuinsCommon;

//? if <26.3 {
/*import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import za.co.neroland.neroruins.registry.ModWorldgen;
*///?}

//? if >=26.3 {
public final class SatellitePlacement implements StructurePlacement {
//?} else {
/*public final class SatellitePlacement extends StructurePlacement {
*///?}
    public static final MapCodec<SatellitePlacement> CODEC = RecordCodecBuilder.<SatellitePlacement>mapCodec(instance -> instance.group(
            Identifier.CODEC.fieldOf("hub_set").forGetter(SatellitePlacement::hubSet),
            Codec.STRING.fieldOf("family").forGetter(SatellitePlacement::family),
            Codec.intRange(0, 31).fieldOf("index").forGetter(SatellitePlacement::index),
            Codec.intRange(1, 32).fieldOf("min_radius").forGetter(SatellitePlacement::minRadius),
            Codec.intRange(1, 32).fieldOf("max_radius").forGetter(SatellitePlacement::maxRadius)
    ).apply(instance, SatellitePlacement::new)).validate(placement -> {
        try {
            SatelliteLayout.offset(0, 0, 0, placement.family, placement.index,
                    placement.minRadius, placement.maxRadius);
            return DataResult.success(placement);
        } catch (IllegalArgumentException failure) {
            return DataResult.error(() -> "Invalid satellite placement parameters");
        }
    });

    private final Identifier hubSet;
    private static final AtomicBoolean INVALID_HUB_REPORTED = new AtomicBoolean();
    private final String family;
    private final int index;
    private final int minRadius;
    private final int maxRadius;

    public SatellitePlacement(Identifier hubSet, String family, int index, int minRadius, int maxRadius) {
        //? if <26.3 {
        /*super(Vec3i.ZERO, FrequencyReductionMethod.DEFAULT, 1.0F, 0, Optional.empty());
        *///?}
        this.hubSet = hubSet;
        this.family = family;
        this.index = index;
        this.minRadius = minRadius;
        this.maxRadius = maxRadius;
    }

    public Identifier hubSet() { return hubSet; }
    public String family() { return family; }
    public int index() { return index; }
    public int minRadius() { return minRadius; }
    public int maxRadius() { return maxRadius; }

    public Optional<ChunkPos> findHub(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        ResourceKey<StructureSet> key = ResourceKey.create(Registries.STRUCTURE_SET, hubSet);
        Optional<Holder<StructureSet>> holder = state.possibleStructureSets().stream()
                .filter(candidate -> candidate.is(key)).findFirst();
        if (holder.isEmpty()) return Optional.empty();
        if (!(holder.get().value().placement() instanceof RandomSpreadStructurePlacement spread)
                || spread.spacing() < 16) {
            if (INVALID_HUB_REPORTED.compareAndSet(false, true)) {
                NeroRuinsCommon.LOGGER.warn("[NeroRuins] Satellite hub placement must use random spread with spacing of at least 16 chunks");
            }
            return Optional.empty();
        }
        long seed = state.getLevelSeed();
        return SatelliteLayout.findHub(seed, spread.spacing(), chunkX, chunkZ, family, index, minRadius, maxRadius,
                (x, z) -> {
                    ChunkPos hub = spread.getPotentialStructureChunk(seed, x, z);
                    return new SatelliteLayout.Chunk(hub.getMinBlockX() >> 4, hub.getMinBlockZ() >> 4);
                }, (x, z) -> spread.isStructureChunk(state, x, z))
                .map(hub -> new ChunkPos(hub.x(), hub.z()));
    }

    //? if >=26.3 {
    @Override
    public boolean isStructureChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        return findHub(state, chunkX, chunkZ).isPresent();
    }

    @Override
    public MapCodec<SatellitePlacement> codec() { return CODEC; }
    //?} else {
    /*@Override
    protected boolean isPlacementChunk(ChunkGeneratorStructureState state, int chunkX, int chunkZ) {
        return findHub(state, chunkX, chunkZ).isPresent();
    }

    @Override
    public StructurePlacementType<?> type() { return ModWorldgen.SATELLITE_PLACEMENT.get(); }
    *///?}
}
