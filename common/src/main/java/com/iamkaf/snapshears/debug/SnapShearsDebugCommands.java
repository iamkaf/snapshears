package com.iamkaf.snapshears.debug;

import com.iamkaf.amber.api.event.v1.events.common.CommandEvents;
import com.iamkaf.konfig.api.v1.ConfigValue;
import com.iamkaf.snapshears.Constants;
import com.iamkaf.snapshears.config.ShearsConfig;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Development-only commands for reading and changing the config without editing its file, so runtime tests can
 * exercise every setting. They are registered only in a development environment.
 *
 * <p>{@code /snapshears debug <setting> <value>} saves and reloads like a config screen edit. Under
 * {@code /snapshears debug unsaved} the value changes in memory only, until the next reload.</p>
 */
public final class SnapShearsDebugCommands {
    private SnapShearsDebugCommands() {
    }

    public static void init() {
        CommandEvents.EVENT.register((dispatcher, buildContext, selection) -> {
            LiteralArgumentBuilder<CommandSourceStack> debug = Commands.literal("debug")
                    .then(Commands.literal("get").executes(SnapShearsDebugCommands::get));
            settings(debug, true);
            LiteralArgumentBuilder<CommandSourceStack> unsaved = Commands.literal("unsaved");
            settings(unsaved, false);
            dispatcher.register(Commands.literal(Constants.MOD_ID).then(debug.then(unsaved)));
        });
    }

    private static void settings(LiteralArgumentBuilder<CommandSourceStack> parent, boolean persist) {
        parent.then(Commands.literal("radius")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 16))
                        .executes(context -> {
                            return change(context, persist, ShearsConfig.RADIUS, IntegerArgumentType.getInteger(context, "value"));
                        })));
        parent.then(Commands.literal("bonus_wool")
                .then(Commands.argument("value", IntegerArgumentType.integer(0, 3))
                        .executes(context -> {
                            return change(context, persist, ShearsConfig.BONUS_WOOL, IntegerArgumentType.getInteger(context, "value"));
                        })));
        parent.then(Commands.literal("only_same_color")
                .then(Commands.argument("value", BoolArgumentType.bool())
                        .executes(context -> {
                            return change(context, persist, ShearsConfig.ONLY_SAME_COLOR, BoolArgumentType.getBool(context, "value"));
                        })));
        parent.then(Commands.literal("shears")
                .then(Commands.argument("entries", StringArgumentType.greedyString())
                        .executes(context -> {
                            String raw = StringArgumentType.getString(context, "entries").trim();
                            List<String> entries = raw.isEmpty()
                                    ? List.of()
                                    : Arrays.stream(raw.split(",")).map(String::trim).toList();
                            return change(context, persist, ShearsConfig.SHEARS, entries);
                        })));
    }

    // A saved change goes through a reload that sees the file differ from memory, like a hand edit followed by
    // /snapshears reload. That is what refreshes the synchronized value the gameplay code reads.
    private static <T> int change(CommandContext<CommandSourceStack> context, boolean persist, ConfigValue<T> value,
            T next) {
        T previous = value.local();
        value.set(next);
        if (persist) {
            ShearsConfig.HANDLE.save();
            value.set(previous);
            ShearsConfig.HANDLE.reload();
        }
        return get(context);
    }

    private static int get(CommandContext<CommandSourceStack> context) {
        String message = "radius=" + ShearsConfig.RADIUS.get()
                + " bonus_wool=" + ShearsConfig.BONUS_WOOL.get()
                + " only_same_color=" + ShearsConfig.ONLY_SAME_COLOR.get()
                + " shears=" + String.join(",", ShearsConfig.SHEARS.get())
                + " | local radius=" + ShearsConfig.RADIUS.local()
                + " bonus_wool=" + ShearsConfig.BONUS_WOOL.local();
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return Command.SINGLE_SUCCESS;
    }
}
