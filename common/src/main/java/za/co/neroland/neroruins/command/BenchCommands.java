package za.co.neroland.neroruins.command;

import java.util.Locale;
import java.util.function.Predicate;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
//? if >=26.3 {
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.densityfunction.SamplerContext;
//?}
import za.co.neroland.neroruins.NeroRuinsCommon;

/**
 * {@code /neroruins bench <structure> [samples]} (operator): measures how long a NeroRuins structure takes to
 * generate — its generation point (ground and biome checks) plus jigsaw assembly — at {@code samples} fixed chunks
 * around the caller in the current dimension. The structure set's spacing is bypassed so every trial attempts
 * generation; nothing is placed and no chunk is loaded (generation reads the noise generator directly). The run
 * blocks the server thread, so the sample count is capped at {@link BenchStats#MAX_SAMPLES}.
 */
public final class BenchCommands {
    private BenchCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("neroruins").then(
                LiteralArgumentBuilder.<CommandSourceStack>literal("bench")
                        // Resolved per check, not at registration: the permission classes need a bootstrapped game.
                        .requires(source -> operator().test(source))
                        .then(RequiredArgumentBuilder.<CommandSourceStack, ResourceKey<Structure>>argument(
                                        "structure", ResourceKeyArgument.key(Registries.STRUCTURE))
                                .suggests((context, builder) -> SharedSuggestionProvider.suggestResource(
                                        ruinStructureIds(context.getSource().getLevel()), builder))
                                .executes(context -> safely(context, BenchStats.DEFAULT_SAMPLES))
                                .then(RequiredArgumentBuilder.<CommandSourceStack, Integer>argument("samples",
                                                IntegerArgumentType.integer(1, BenchStats.MAX_SAMPLES))
                                        .executes(context -> safely(context,
                                                IntegerArgumentType.getInteger(context, "samples")))))));
    }

    private static Predicate<CommandSourceStack> operator() {
        //? if >=1.21.11 {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
        //?} else {
        /*return source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS);
        *///?}
    }

    private static Iterable<Identifier> ruinStructureIds(ServerLevel level) {
        //? if >=1.21.2 {
        var ids = level.registryAccess().lookupOrThrow(Registries.STRUCTURE).keySet();
        //?} else {
        /*var ids = level.registryAccess().registryOrThrow(Registries.STRUCTURE).keySet();
        *///?}
        return ids.stream().filter(id -> NeroRuinsCommon.MOD_ID.equals(id.getNamespace())).sorted().toList();
    }

    private static int safely(CommandContext<CommandSourceStack> context, int samples) throws CommandSyntaxException {
        Holder.Reference<Structure> structure = ResourceKeyArgument.getStructure(context, "structure");
        CommandSourceStack source = context.getSource();
        if (!NeroRuinsCommon.MOD_ID.equals(structure.key().identifier().getNamespace())) {
            source.sendFailure(Component.translatable("command.neroruins.bench.not_ruin"));
            return 0;
        }
        try {
            return run(source, structure, samples);
        } catch (RuntimeException failure) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] /neroruins bench failed ({})", failure.getClass().getName());
            source.sendFailure(Component.translatable("command.neroruins.bench.failed"));
            return 0;
        }
    }

    private static int run(CommandSourceStack source, Holder.Reference<Structure> structure, int samples) {
        ServerLevel level = source.getLevel();
        String id = structure.key().identifier().toString();
        BlockPos origin = BlockPos.containing(source.getPosition());
        int originX = origin.getX() >> 4;
        int originZ = origin.getZ() >> 4;
        ChunkGenerator generator = level.getChunkSource().getGenerator();
        RandomState randomState = level.getChunkSource().randomState();
        //? if >=26.3 {
        var templates = level.getStructureTemplateManager();
        Climate.Sampler sampler = randomState.createClimateSampler(SamplerContext.EMPTY_UNCACHED);
        //?} else {
        /*var templates = level.getStructureManager();
        *///?}
        Predicate<Holder<Biome>> biomes = structure.value().biomes()::contains;
        long seed = level.getSeed();

        BenchStats stats = new BenchStats(samples);
        for (int i = 0; i < samples; i++) {
            int[] offset = BenchStats.offset(i, samples);
            ChunkPos chunk = new ChunkPos(originX + offset[0], originZ + offset[1]);
            long began = System.nanoTime();
            //? if >=26.3 {
            StructureStart start = structure.value().generate(structure, level.dimension(), level.registryAccess(),
                    generator, generator.getBiomeSource(), sampler, randomState, templates, seed, chunk, 0, level, biomes);
            //?} else if >=1.21.2 {
            /*StructureStart start = structure.value().generate(structure, level.dimension(), level.registryAccess(),
                    generator, generator.getBiomeSource(), randomState, templates, seed, chunk, 0, level, biomes);
            *///?} else {
            /*StructureStart start = structure.value().generate(level.registryAccess(), generator,
                    generator.getBiomeSource(), randomState, templates, seed, chunk, 0, level, biomes);
            *///?}
            stats.attempt(System.nanoTime() - began);
            if (start.isValid()) {
                BoundingBox box = start.getBoundingBox();
                stats.start(start.getPieces().size(), box.getXSpan(), box.getYSpan(), box.getZSpan());
            }
        }

        BenchStats.Result result = stats.result();
        source.sendSuccess(() -> Component.translatable("command.neroruins.bench.timing", id, result.attempts(),
                result.starts(), format(result.meanMillis()), format(result.p95Millis()), format(result.maxMillis())),
                false);
        if (result.starts() == 0) {
            source.sendSuccess(() -> Component.translatable("command.neroruins.bench.no_starts"), false);
        } else {
            source.sendSuccess(() -> Component.translatable("command.neroruins.bench.size", format(result.meanPieces()),
                    result.maxPieces(), result.maxSpanX(), result.maxSpanY(), result.maxSpanZ()), false);
        }
        return result.starts();
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
