package com.kaiki.minechatcorrect.fabric;

import com.kaiki.minechatcorrect.MineChatCorrectClient;
import com.kaiki.minechatcorrect.config.DictionaryWordParser;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.network.chat.Component;

import java.io.UncheckedIOException;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

/** Client-only commands for the persistent personal dictionary. */
public final class ChatCorrectCommands {
    private ChatCorrectCommands() {}

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) ->
                dispatcher.register(literal("chatcorrect")
                        .requires(FabricClientCommandSource::attended)
                        .then(literal("add").then(argument("word", StringArgumentType.greedyString())
                                .executes(context -> update(context.getSource(),
                                        StringArgumentType.getString(context, "word"), true))))
                        .then(literal("del").then(argument("word", StringArgumentType.greedyString())
                                .executes(context -> update(context.getSource(),
                                        StringArgumentType.getString(context, "word"), false))))));
    }

    private static int update(FabricClientCommandSource source, String input, boolean add) {
        String word = DictionaryWordParser.normalize(input);
        if (!word.matches("[A-Za-zА-Яа-яЁё]+(?:'[A-Za-zА-Яа-яЁё]+)*")) {
            source.sendError(Component.literal("Укажите одно слово русскими или английскими буквами."));
            return 0;
        }
        var checker = MineChatCorrectClient.spellChecker();
        boolean exists = checker.dictionaryManager().extraWords().contains(word);
        if (add == exists) {
            source.sendFeedback(Component.literal(add
                    ? "Слово уже есть в пользовательском словаре: " + word
                    : "Слова нет в пользовательском словаре: " + word));
            return 0;
        }
        try {
            if (add) {
                checker.addWord(word);
            } else {
                checker.removeWord(word);
            }
        } catch (UncheckedIOException exception) {
            source.sendError(Component.literal("Не удалось сохранить пользовательский словарь."));
            return 0;
        }
        source.sendFeedback(Component.literal(add
                ? "Добавлено в пользовательский словарь: " + word
                : "Удалено из пользовательского словаря: " + word));
        return 1;
    }
}
