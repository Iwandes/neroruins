package za.co.neroland.neroruins.command;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Set;
import net.minecraft.network.chat.Component;
import za.co.neroland.neroruins.network.CodexSnapshot;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

class CodexCommandsTest {
    @Test
    void acceptsNamespacedLogAndChapterArguments() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CodexCommands.register(dispatcher);
        assertFalse(dispatcher.parse("neroruins codex neroruins:earth.1", null).getReader().canRead());
        assertFalse(dispatcher.parse("neroruins codex chapter earth", null).getReader().canRead());
        assertFalse(dispatcher.parse("neroruins privacy export", null).getReader().canRead());
        assertTrue(dispatcher.parse("neroruins privacy export other", null).getReader().canRead());
        assertTrue(dispatcher.parse("neroruins codex chapter earth", null).getContext().getNodes().stream()
                .anyMatch(node -> node.getNode().getName().equals("chapter")));
    }

    @Test
    void unsupportedClientGetsFeedbackInsteadOfASuccessfulScreenOpen() {
        var sent = new ArrayList<CodexSnapshot>();
        var feedback = new ArrayList<Component>();
        Set<String> saved = Set.of("neroruins:earth.2", "invalid:entry");
        assertEquals(1, CodexCommands.openCodex(saved, snapshot -> {
            sent.add(snapshot);
            return true;
        }, feedback::add));
        assertEquals(java.util.List.of("neroruins:earth.2"), sent.get(0).logs());
        assertTrue(feedback.isEmpty());
        assertEquals(0, CodexCommands.openCodex(saved, snapshot -> false, feedback::add));
        assertEquals(1, feedback.size());
        assertTrue(feedback.get(0).getString().contains("requires NeroRuins"));
    }
}
