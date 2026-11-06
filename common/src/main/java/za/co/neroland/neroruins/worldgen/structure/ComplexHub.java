package za.co.neroland.neroruins.worldgen.structure;

import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import za.co.neroland.neroruins.registry.ModWorldgen;
import za.co.neroland.neroruins.worldgen.placement.SatelliteLayout;

public final class ComplexHub extends Structure {
    public static final MapCodec<ComplexHub> CODEC = RecordCodecBuilder.<ComplexHub>mapCodec(instance -> instance.group(
            settingsCodec(instance),
            JigsawStructure.CODEC.codec().fieldOf("layout").forGetter(hub -> hub.layout),
            Codec.STRING.fieldOf("family").forGetter(ComplexHub::family),
            Codec.STRING.fieldOf("size_class").forGetter(ComplexHub::sizeClass),
            GroundCheck.CODEC.optionalFieldOf("ground_check").forGetter(hub -> hub.groundCheck)
    ).apply(instance, ComplexHub::new)).validate(hub -> {
        if (!SatelliteLayout.validFamily(hub.family)
                || !(hub.sizeClass.equals("outpost") || hub.sizeClass.equals("complex") || hub.sizeClass.equals("citadel"))) {
            return DataResult.error(() -> "Invalid complex family or size class");
        }
        return DataResult.success(hub);
    });

    private final JigsawStructure layout;
    private final String family;
    private final String sizeClass;
    private final Optional<GroundCheck> groundCheck;

    public ComplexHub(StructureSettings settings, JigsawStructure layout, String family, String sizeClass,
            Optional<GroundCheck> groundCheck) {
        super(settings);
        this.layout = layout;
        this.family = family;
        this.sizeClass = sizeClass;
        this.groundCheck = groundCheck;
    }

    public String family() { return family; }
    public String sizeClass() { return sizeClass; }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        if (groundCheck.isPresent() && !suitableGround(context, groundCheck.get())) return Optional.empty();
        return layout.findGenerationPoint(context);
    }

    private static boolean suitableGround(GenerationContext context, GroundCheck check) {
        int[] surface = new int[9];
        int[] floor = new int[9];
        int index = 0;
        for (int dx : new int[] { -check.radius(), 0, check.radius() }) {
            for (int dz : new int[] { -check.radius(), 0, check.radius() }) {
                int x = context.chunkPos().getMiddleBlockX() + dx;
                int z = context.chunkPos().getMiddleBlockZ() + dz;
                surface[index] = context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG,
                        context.heightAccessor(), context.randomState());
                floor[index] = check.requireDry()
                        ? context.chunkGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG,
                                context.heightAccessor(), context.randomState())
                        : surface[index];
                index++;
            }
        }
        //? if >=1.21.2 {
        return check.accepts(surface, floor, context.heightAccessor().getMinY(), context.heightAccessor().getHeight());
        //?} else {
        /*return check.accepts(surface, floor, context.heightAccessor().getMinBuildHeight(), context.heightAccessor().getHeight());
        *///?}
    }

    @Override
    public StructureType<?> type() { return ModWorldgen.COMPLEX_HUB.get(); }
}
