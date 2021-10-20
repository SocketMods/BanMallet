package dev.socketmods.banmallet.commands;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.socketmods.banmallet.PermissionLevel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static dev.socketmods.banmallet.util.TranslationUtil.createTranslation;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.GameProfileArgument.gameProfile;
import static net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles;

public class OpQueryCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(literal("opquery")
                .requires(PermissionLevel.MODERATOR)
                .then(argument("targets", gameProfile())
                        .suggests(OpQueryCommand::getSuggestions)
                        .executes((ctx) -> queryOp(ctx, getGameProfiles(ctx, "targets")))));
    }

    static CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSourceStack> ctx,
                                                         final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerList().getOpNames(), builder);
    }

    static int queryOp(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> targets) {
        final CommandSourceStack source = ctx.getSource();
        final PlayerList playerList = source.getServer().getPlayerList();
        int successes = 0;

        for (GameProfile target : targets) {
            if (playerList.isOp(target)) {
                ++successes;
                final int permissionLevel = source.getServer().getProfilePermissions(target);

                source.sendSuccess(createTranslation(source, "commands.banmallet.opquery.op",
                        target.getName(), permissionLevel), false);
            } else {
                source.sendSuccess(createTranslation(source, "commands.banmallet.opquery.not_op",
                        target.getName()), false);
            }
        }

        return successes;
    }
}
