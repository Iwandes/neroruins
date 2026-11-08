package za.co.neroland.neroruins.command;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.structure.templatesystem.JigsawReplacementProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.lattice.GlyphStoneBlock;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.puzzle.BallastPlateBlock;
import za.co.neroland.neroruins.puzzle.BallastPuzzle;
import za.co.neroland.neroruins.puzzle.PuzzleSeedProcessor;
import za.co.neroland.neroruins.puzzle.SealedDoorBlock;
import za.co.neroland.neroruins.registry.ModBlocks;
import za.co.neroland.neroruins.registry.ModEntities;
import za.co.neroland.neroruins.registry.ModItems;
import za.co.neroland.neroruins.vault.VaultCacheBlockEntity;

/**
 * {@code /neroruins gallery [clear]} (operator, creative, player only): builds a labelled showcase next to the
 * player of everything NeroRuins adds — every block floating for inspection, the eight Ballast Plate targets,
 * every item in frames (including the nine relics and a readable data shard), and every structure template
 * placed with the generation processors, so each bunker hub holds a working sealed vault and plate puzzle and
 * the archive room a working Log Terminal. {@code clear} empties the same footprint (blocks and non-player
 * entities) so a rebuild never stacks. Showcase vaults are detached from the world's per-structure counts.
 */
public final class GalleryCommands {
    private static final int FLOAT_ABOVE = 3;

    private GalleryCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("neroruins").then(
                LiteralArgumentBuilder.<CommandSourceStack>literal("gallery")
                        .requires(source -> source.getPlayer() != null && operator().test(source))
                        .executes(context -> safely(context.getSource(), "gallery", () -> build(context.getSource())))
                        .then(LiteralArgumentBuilder.<CommandSourceStack>literal("clear")
                                .executes(context -> safely(context.getSource(), "gallery clear",
                                        () -> clear(context.getSource()))))));
    }

    private static Predicate<CommandSourceStack> operator() {
        //? if >=1.21.11 {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
        //?} else {
        /*return source -> source.hasPermission(Commands.LEVEL_GAMEMASTERS);
        *///?}
    }

    @FunctionalInterface
    private interface Body { int run(); }

    private static int safely(CommandSourceStack source, String name, Body body) {
        try {
            return body.run();
        } catch (RuntimeException failure) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] /neroruins {} failed ({})", name, failure.getClass().getName());
            source.sendFailure(Component.translatable("command.neroruins.gallery.failed"));
            return 0;
        }
    }

    private record Showcase(List<Block> blocks, List<ItemStack> items, List<Identifier> templateIds,
            List<StructureTemplate> templates, GalleryLayout.Plan plan) { }

    private static Showcase showcase(ServerLevel level) {
        List<Block> blocks = new ArrayList<>();
        for (Block block : BuiltInRegistries.BLOCK) {
            if (NeroRuinsCommon.MOD_ID.equals(BuiltInRegistries.BLOCK.getKey(block).getNamespace())) blocks.add(block);
        }
        blocks.sort(Comparator.comparing(block -> BuiltInRegistries.BLOCK.getKey(block).toString()));
        List<ItemStack> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (!NeroRuinsCommon.MOD_ID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())) continue;
            ItemStack stack = new ItemStack(item);
            if (item == ModItems.DATA_SHARD.get() && !LogCatalog.ids().isEmpty()) {
                stack.set(ModItems.LOG_ID.get(), LogCatalog.ids().get(0));
            }
            items.add(stack);
        }
        items.sort(Comparator.comparing(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()));
        //? if >=26.3 {
        var manager = level.getStructureTemplateManager();
        //?} else {
        /*var manager = level.getStructureManager();
        *///?}
        List<Identifier> ids = manager.listTemplates()
                .filter(id -> NeroRuinsCommon.MOD_ID.equals(id.getNamespace()))
                .sorted(Comparator.comparing(Identifier::toString)).toList();
        List<StructureTemplate> templates = new ArrayList<>();
        List<GalleryLayout.Piece> pieces = new ArrayList<>();
        List<Identifier> placed = new ArrayList<>();
        for (Identifier id : ids) {
            var template = manager.get(id);
            if (template.isEmpty()) continue;
            var size = template.get().getSize();
            if (size.getX() < 1 || size.getY() < 1 || size.getZ() < 1) continue;
            templates.add(template.get());
            placed.add(id);
            pieces.add(new GalleryLayout.Piece(id.getPath(), size.getX(), size.getY(), size.getZ()));
        }
        int plates = BallastPuzzle.MAX - BallastPuzzle.MIN + 1;
        return new Showcase(blocks, items, placed, templates,
                GalleryLayout.plan(blocks.size(), plates, GlyphStoneBlock.GLYPHS, items.size(), pieces));
    }

    private static ServerPlayer creativePlayer(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            source.sendFailure(Component.translatable("command.neroruins.gallery.creative_only"));
            return null;
        }
        return player;
    }

    private static int build(CommandSourceStack source) {
        ServerPlayer player = creativePlayer(source);
        if (player == null || !(player.level() instanceof ServerLevel level)) return 0;
        BlockPos origin = player.blockPosition();
        Showcase showcase = showcase(level);
        GalleryLayout.Box box = showcase.plan().bounds();
        clearBox(level, origin, box);

        BlockState floor = Blocks.POLISHED_ANDESITE.defaultBlockState();
        for (int x = box.minX(); x <= box.maxX(); x++) {
            for (int z = box.minZ(); z <= box.maxZ(); z++) {
                level.setBlock(origin.offset(x, 0, z), floor, Block.UPDATE_CLIENTS);
            }
        }

        // Every block, floating so all faces are visible.
        for (int i = 0; i < showcase.blocks().size(); i++) {
            BlockPos at = origin.offset(GalleryLayout.START_X + i * GalleryLayout.BLOCK_SPACING, FLOAT_ABOVE,
                    GalleryLayout.BLOCK_ROW_Z);
            BlockState state = showcase.blocks().get(i).defaultBlockState();
            level.setBlock(at, state, Block.UPDATE_CLIENTS);
            if (state.getBlock() instanceof SealedDoorBlock) {
                level.setBlock(at.above(), state.setValue(SealedDoorBlock.HALF, DoubleBlockHalf.UPPER), Block.UPDATE_CLIENTS);
            }
        }
        label(level, origin.offset(GalleryLayout.START_X, FLOAT_ABOVE + 1, GalleryLayout.BLOCK_ROW_Z),
                Component.translatable("command.neroruins.gallery.label.blocks"));

        // The eight Ballast Plate targets, as a puzzle solver would read them.
        for (int target = BallastPuzzle.MIN; target <= BallastPuzzle.MAX; target++) {
            BlockPos at = origin.offset(GalleryLayout.START_X + (target - 1) * GalleryLayout.PLATE_SPACING, 1,
                    GalleryLayout.PLATE_ROW_Z);
            level.setBlock(at, ModBlocks.BALLAST_PLATE.get().defaultBlockState().setValue(BallastPlateBlock.TARGET, target),
                    Block.UPDATE_CLIENTS);
            label(level, at.above(), Component.translatable("command.neroruins.gallery.label.plate", target));
        }
        label(level, origin.offset(GalleryLayout.START_X - 1, 3, GalleryLayout.PLATE_ROW_Z),
                Component.translatable("command.neroruins.gallery.label.plates"));

        // One of each Sentinel mark, inert, beside the plates.
        for (int i = 0; i < ModEntities.SENTINELS.size(); i++) {
            Entity entity = ModEntities.SENTINELS.get(i).get().create(level, EntitySpawnReason.COMMAND);
            if (!(entity instanceof Mob sentinel)) continue;
            BlockPos at = origin.offset(GalleryLayout.SENTINEL_START_X + i * GalleryLayout.PLATE_SPACING, 1,
                    GalleryLayout.PLATE_ROW_Z);
            sentinel.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, 180.0F, 0.0F);
            sentinel.setNoAi(true);
            sentinel.setPersistenceRequired();
            level.addFreshEntity(sentinel);
        }
        label(level, origin.offset(GalleryLayout.SENTINEL_START_X, 4, GalleryLayout.PLATE_ROW_Z),
                Component.translatable("command.neroruins.gallery.label.sentinels"));

        // All sixteen Architect glyphs, the alphabet later puzzles draw their wall clues from.
        for (int glyph = 0; glyph < GlyphStoneBlock.GLYPHS; glyph++) {
            level.setBlock(origin.offset(GalleryLayout.START_X + glyph * GalleryLayout.GLYPH_SPACING, 2,
                    GalleryLayout.GLYPH_ROW_Z), ModBlocks.GLYPH_STONE.get().defaultBlockState()
                    .setValue(GlyphStoneBlock.GLYPH, glyph), Block.UPDATE_CLIENTS);
        }
        label(level, origin.offset(GalleryLayout.START_X - 1, 3, GalleryLayout.GLYPH_ROW_Z),
                Component.translatable("command.neroruins.gallery.label.glyphs"));

        // Every item in frames on a short wall.
        int columns = showcase.plan().frameColumns();
        for (int column = 0; column < columns; column++) {
            for (int row = 0; row < GalleryLayout.FRAME_ROWS; row++) {
                level.setBlock(origin.offset(GalleryLayout.START_X + column, 1 + row, GalleryLayout.ITEM_WALL_Z),
                        ModBlocks.SURVEY_HULL.get().defaultBlockState(), Block.UPDATE_CLIENTS);
            }
        }
        for (int i = 0; i < showcase.items().size(); i++) {
            BlockPos at = origin.offset(GalleryLayout.START_X + i / GalleryLayout.FRAME_ROWS,
                    GalleryLayout.FRAME_ROWS - i % GalleryLayout.FRAME_ROWS, GalleryLayout.ITEM_WALL_Z + 1);
            ItemFrame frame = new ItemFrame(level, at, Direction.SOUTH);
            frame.setItem(showcase.items().get(i).copy());
            level.addFreshEntity(frame);
        }
        label(level, origin.offset(GalleryLayout.START_X, GalleryLayout.FRAME_ROWS + 1, GalleryLayout.ITEM_WALL_Z + 1),
                Component.translatable("command.neroruins.gallery.label.items"));

        // Every structure piece, processed as generation does: jigsaw blocks resolved, puzzles seeded.
        StructurePlaceSettings settings = new StructurePlaceSettings()
                .addProcessor(JigsawReplacementProcessor.INSTANCE).addProcessor(PuzzleSeedProcessor.INSTANCE);
        List<GalleryLayout.Placement> placements = showcase.plan().templates();
        for (int i = 0; i < placements.size(); i++) {
            GalleryLayout.Placement placement = placements.get(i);
            BlockPos at = origin.offset(placement.x(), placement.y(), placement.z());
            showcase.templates().get(i).placeInWorld(level, at, at, settings, level.getRandom(), Block.UPDATE_CLIENTS);
            GalleryLayout.Piece piece = placement.piece();
            for (BlockPos pos : BlockPos.betweenClosed(at, at.offset(piece.sizeX() - 1, piece.sizeY() - 1, piece.sizeZ() - 1))) {
                if (level.getBlockEntity(pos) instanceof VaultCacheBlockEntity cache) cache.demonstrationOnly();
            }
            label(level, at.offset(piece.sizeX() / 2, piece.sizeY() + 1, -1),
                    Component.literal(showcase.templateIds().get(i).getPath()));
        }

        source.sendSuccess(() -> Component.translatable("command.neroruins.gallery.built", showcase.blocks().size(),
                showcase.items().size(), placements.size()), false);
        return Command.SINGLE_SUCCESS;
    }

    private static int clear(CommandSourceStack source) {
        ServerPlayer player = creativePlayer(source);
        if (player == null || !(player.level() instanceof ServerLevel level)) return 0;
        int[] counts = clearBox(level, player.blockPosition(), showcase(level).plan().bounds());
        source.sendSuccess(() -> Component.translatable("command.neroruins.gallery.cleared", counts[0], counts[1]), false);
        return Command.SINGLE_SUCCESS;
    }

    /** Removes every entity but players, then every block, inside the gallery box. Returns {blocks, entities}. */
    private static int[] clearBox(ServerLevel level, BlockPos origin, GalleryLayout.Box box) {
        AABB area = new AABB(origin.getX() + box.minX(), origin.getY() + box.minY() - 1, origin.getZ() + box.minZ(),
                origin.getX() + box.maxX() + 1, origin.getY() + box.maxY() + 3, origin.getZ() + box.maxZ() + 1);
        int entities = 0;
        for (Entity entity : level.getEntitiesOfClass(Entity.class, area, entity -> !(entity instanceof Player))) {
            entity.discard();
            entities++;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        int blocks = 0;
        for (BlockPos pos : BlockPos.betweenClosed(origin.offset(box.minX(), box.minY(), box.minZ()),
                origin.offset(box.maxX(), box.maxY(), box.maxZ()))) {
            if (!level.getBlockState(pos).isAir()) {
                level.setBlock(pos, air, Block.UPDATE_CLIENTS);
                blocks++;
            }
        }
        return new int[] { blocks, entities };
    }

    private static void label(ServerLevel level, BlockPos pos, Component name) {
        ArmorStand stand = new ArmorStand(level, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
        stand.setCustomName(name);
        stand.setCustomNameVisible(true);
        stand.setInvisible(true);
        stand.setNoGravity(true);
        //? if >=26.3 {
        stand.setPermanentlyInvulnerable(true);
        //?} else {
        /*stand.setInvulnerable(true);
        *///?}
        level.addFreshEntity(stand);
    }
}
