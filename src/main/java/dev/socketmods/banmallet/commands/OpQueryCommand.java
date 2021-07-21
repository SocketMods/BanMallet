package dev.socketmods.banmallet.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.socketmods.banmallet.PermissionLevel;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;
import static net.minecraft.command.arguments.GameProfileArgument.gameProfile;
import static net.minecraft.command.arguments.GameProfileArgument.getGameProfiles;

public class OpQueryCommand {
    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(literal("opquery")
                .requires(PermissionLevel.MODERATOR)
                .then(argument("targets", gameProfile())
                        .suggests(OpQueryCommand::getSuggestions)
                        .executes((ctx) -> queryOp(ctx, getGameProfiles(ctx, "targets")))));
    }

    static CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSource> ctx,
                                                         final SuggestionsBuilder builder) {
        return ISuggestionProvider.suggest(ctx.getSource().getServer().getPlayerList().getOpNames(), builder);
    }

    static int queryOp(CommandContext<CommandSource> ctx, Collection<GameProfile> targets) {
        final CommandSource source = ctx.getSource();
        final PlayerList playerList = source.getServer().getPlayerList();
        int successes = 0;

        for (GameProfile target : targets) {
            if (playerList.isOp(target)) {
                ++successes;
                final int permissionLevel = source.getServer().getProfilePermissions(target);
                source.sendSuccess(new TranslationTextComponent("commands.banmallet.opquery.op",
                        target.getName(), permissionLevel), false);
            } else {
                source.sendSuccess(new TranslationTextComponent("commands.banmallet.opquery.not_op",
                        target.getName()), false);
            }
        }

        return successes;
    }
}
