package za.co.neroland.neroruins.command;

import java.io.IOException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.data.ArchiveExport;
import za.co.neroland.neroruins.data.PlayerArchive;
import za.co.neroland.neroruins.lore.LogCatalog;
import za.co.neroland.neroruins.network.CodexSnapshot;
import za.co.neroland.neroruins.platform.Services;

public final class CodexCommands {
    private CodexCommands() { }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        GalleryCommands.register(dispatcher);
        BenchCommands.register(dispatcher);
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("neroruins").then(
                LiteralArgumentBuilder.<CommandSourceStack>literal("codex")
                .executes(context -> list(context.getSource().getPlayerOrException()))
                .then(LiteralArgumentBuilder.<CommandSourceStack>literal("chapter").then(
                        RequiredArgumentBuilder.<CommandSourceStack, String>argument("chapter", StringArgumentType.word())
                        .executes(context -> chapter(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "chapter")))))
                .then(RequiredArgumentBuilder.<CommandSourceStack, String>argument("log", StringArgumentType.greedyString())
                        .executes(context -> read(context.getSource().getPlayerOrException(),
                                StringArgumentType.getString(context, "log"))))));
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("neroruins").then(
                LiteralArgumentBuilder.<CommandSourceStack>literal("privacy").then(
                        LiteralArgumentBuilder.<CommandSourceStack>literal("export")
                        .executes(context -> export(context.getSource().getPlayerOrException())))));
    }

    private static int export(ServerPlayer player) {
        var server = player.level().getServer();
        try {
            var path = ArchiveExport.write(server.getWorldPath(LevelResource.ROOT), player.getUUID(),
                    PlayerArchive.get(server).export(player.getUUID()),
                    za.co.neroland.neroruins.data.ClaimLedger.get(server).export(player.getUUID()),
                    za.co.neroland.neroruins.data.ClaimLedger.get(server).exportReservations(player.getUUID()),
                    za.co.neroland.neroruins.data.ExpeditionRecord.get(server).export(player.getUUID()));
            message(player, Component.translatable("message.neroruins.export.saved", path.toString()));
            return 1;
        } catch (IOException failure) {
            NeroRuinsCommon.LOGGER.warn("[NeroRuins] Personal data export failed ({})", failure.getClass().getName());
            message(player, Component.translatable("message.neroruins.export.failed"));
            return 0;
        }
    }

    private static int list(ServerPlayer player) {
        var saved = PlayerArchive.get(player.level().getServer()).export(player.getUUID());
        return openCodex(saved.keySet(), snapshot -> Services.NETWORK.sendToPlayer(player, snapshot),
                text -> message(player, text));
    }

    static int openCodex(Set<String> saved, Predicate<CodexSnapshot> send, Consumer<Component> feedback) {
        if (send.test(CodexSnapshot.fromArchive(saved))) return 1;
        feedback.accept(Component.literal("The Codex screen requires NeroRuins on your client. "
                + "Your archive can still be exported with /neroruins privacy export."));
        return 0;
    }

    private static int chapter(ServerPlayer player, String chapter) {
        var saved = PlayerArchive.get(player.level().getServer()).export(player.getUUID());
        if (!LogCatalog.chapterComplete(chapter, saved.keySet())) {
            message(player, Component.translatable("message.neroruins.codex.chapter_locked"));
            return 0;
        }
        message(player, Component.translatable("codex.neroruins." + chapter + ".title"));
        message(player, Component.translatable("codex.neroruins." + chapter + ".body"));
        return 1;
    }

    private static int read(ServerPlayer player, String id) {
        var prefix = LogCatalog.translationPrefix(id);
        if (prefix.isEmpty() || !PlayerArchive.get(player.level().getServer()).contains(player.getUUID(), id)) {
            message(player, Component.translatable("message.neroruins.codex.unavailable"));
            return 0;
        }
        message(player, Component.translatable(prefix.get() + ".title"));
        message(player, Component.translatable(prefix.get() + ".speaker"));
        message(player, Component.translatable(prefix.get() + ".body"));
        return 1;
    }

    private static void message(ServerPlayer player, Component text) {
        //? if >=26.1 {
        player.sendSystemMessage(text);
        //?} else {
        /*player.displayClientMessage(text, false);
        *///?}
    }
}
