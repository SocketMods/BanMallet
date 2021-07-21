package dev.socketmods.banmallet.commands.replacements;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.socketmods.banmallet.CommandHelper;
import dev.socketmods.banmallet.PermissionLevel;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.impl.DeOpCommand;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;
import static net.minecraft.command.arguments.GameProfileArgument.gameProfile;
import static net.minecraft.command.arguments.GameProfileArgument.getGameProfiles;

/**
 * BanMallet's replacement for {@link DeOpCommand}.
 */
public class BHDeOpCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_OP = new SimpleCommandExceptionType(
            new TranslationTextComponent("commands.deop.failed"));

    public static LiteralArgumentBuilder<CommandSource> getNode() {
        return literal("deop")
                .requires(PermissionLevel.MODERATOR)
                .then(argument("targets", gameProfile())
                        .suggests(BHDeOpCommand::getSuggestions)
                        .executes((ctx) -> deopPlayers(ctx, getGameProfiles(ctx, "targets")))
                );
    }

    static CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSource> ctx,
                                                         final SuggestionsBuilder builder) {
        return ISuggestionProvider.suggest(ctx.getSource().getServer().getPlayerList().getOpNames(), builder);
    }

    private static int deopPlayers(CommandContext<CommandSource> ctx, Collection<GameProfile> targets)
            throws CommandSyntaxException {
        final CommandSource source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final PlayerList playerList = server.getPlayerList();
        int successes = 0;

        for (GameProfile target : targets) {
            if (playerList.isOp(target)) {
                if (canDeop(server, source, target)) {
                    playerList.deop(target);
                    ++successes;
                    source.sendSuccess(new TranslationTextComponent("commands.deop.success",
                            target.getName()), true);
                } else {
                    source.sendFailure(new TranslationTextComponent(
                            "commands.banmallet.deop.insufficient_permission"));
                }
            }
        }

        if (successes == 0) {
            throw ERROR_NOT_OP.create();
        } else {
            server.kickUnlistedPlayers(source);
            return successes;
        }
    }

    private static boolean canDeop(MinecraftServer server, CommandSource source, GameProfile target) {
        final PermissionLevel targetLevel = PermissionLevel.forLevel(server.getProfilePermissions(target));
        final PermissionLevel sourceLevel = PermissionLevel.forLevel(CommandHelper.getPermissionLevel(source));

        return sourceLevel.canAffect(targetLevel);
    }
}
