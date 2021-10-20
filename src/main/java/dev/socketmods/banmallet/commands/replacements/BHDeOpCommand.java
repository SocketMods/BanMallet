package dev.socketmods.banmallet.commands.replacements;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.socketmods.banmallet.PermissionLevel;
import dev.socketmods.banmallet.commands.exception.TranslatedCommandExceptionType;
import dev.socketmods.banmallet.util.CommandHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.commands.DeOpCommands;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static dev.socketmods.banmallet.commands.exception.TranslatedCommandExceptionType.translatedExceptionType;
import static dev.socketmods.banmallet.util.TranslationUtil.createTranslation;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.GameProfileArgument.gameProfile;
import static net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles;

/**
 * BanMallet's replacement for {@link DeOpCommands}.
 */
public class BHDeOpCommand {
    private static final TranslatedCommandExceptionType ERROR_NOT_OP = translatedExceptionType("commands.deop.failed");

    public static LiteralArgumentBuilder<CommandSourceStack> getNode() {
        return literal("deop")
                .requires(PermissionLevel.MODERATOR)
                .then(argument("targets", gameProfile())
                        .suggests(BHDeOpCommand::getSuggestions)
                        .executes((ctx) -> deopPlayers(ctx, getGameProfiles(ctx, "targets")))
                );
    }

    static CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSourceStack> ctx,
                                                         final SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(ctx.getSource().getServer().getPlayerList().getOpNames(), builder);
    }

    private static int deopPlayers(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> targets)
            throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final PlayerList playerList = server.getPlayerList();
        int successes = 0;

        for (GameProfile target : targets) {
            if (playerList.isOp(target)) {
                if (canDeop(server, source, target)) {
                    playerList.deop(target);
                    ++successes;
                    source.sendSuccess(createTranslation(source, "commands.deop.success", target.getName()),
                            true);
                } else {
                    source.sendFailure(createTranslation(source, "commands.banmallet.deop.insufficient_permission",
                            target.getName()));
                }
            }
        }

        if (successes == 0) {
            throw ERROR_NOT_OP.create(source);
        } else {
            server.kickUnlistedPlayers(source);
            return successes;
        }
    }

    private static boolean canDeop(MinecraftServer server, CommandSourceStack source, GameProfile target) {
        final PermissionLevel targetLevel = PermissionLevel.forLevel(server.getProfilePermissions(target));
        final PermissionLevel sourceLevel = PermissionLevel.forLevel(CommandHelper.getPermissionLevel(source));

        return sourceLevel.canAffect(targetLevel);
    }
}
