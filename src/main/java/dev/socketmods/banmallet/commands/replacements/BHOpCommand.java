package dev.socketmods.banmallet.commands.replacements;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
import net.minecraft.server.commands.OpCommand;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.ServerOpList;
import net.minecraft.server.players.ServerOpListEntry;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static dev.socketmods.banmallet.commands.exception.TranslatedCommandExceptionType.translatedExceptionType;
import static dev.socketmods.banmallet.util.TranslationUtil.createTranslation;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.GameProfileArgument.gameProfile;
import static net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles;

/**
 * BanMallet's replacement for {@link OpCommand}.
 */
public class BHOpCommand {
    private static final TranslatedCommandExceptionType ERROR_ALREADY_OP = translatedExceptionType("commands.op.failed");
    private static final TranslatedCommandExceptionType ERROR_INSUFFICIENT_PERMISSION =
            translatedExceptionType("commands.banmallet.op.insufficient_permission");

    public static LiteralArgumentBuilder<CommandSourceStack> getNode() {
        return literal("op")
                .requires(PermissionLevel.MODERATOR)
                .then(argument("targets", gameProfile())
                        .suggests(BHOpCommand::getSuggestions)
                        .then(argument("level", IntegerArgumentType.integer(1, 4))
                                .executes((ctx) -> opPlayers(ctx, getGameProfiles(ctx, "targets"),
                                        getInteger(ctx, "level")))
                        )
                        .executes((ctx) -> opPlayers(ctx, getGameProfiles(ctx, "targets")))
                );
    }

    static CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSourceStack> ctx,
                                                         final SuggestionsBuilder builder) {
        final PlayerList playerlist = ctx.getSource().getServer().getPlayerList();
        final Stream<String> players = playerlist.getPlayers().stream()
                .filter((player) -> !playerlist.isOp(player.getGameProfile()))
                .map((player) -> player.getGameProfile().getName());

        return SharedSuggestionProvider.suggest(players, builder);
    }

    private static int opPlayers(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> targets)
            throws CommandSyntaxException {
        final int permissionLevel = CommandHelper.getPermissionLevel(ctx.getSource());
        // One less the current operator's permission level
        return opPlayers(ctx, targets, permissionLevel - 1);
    }

    private static int opPlayers(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> targets,
                                 int permissionLevel) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final PlayerList playerList = server.getPlayerList();
        int successes = 0;

        final PermissionLevel thisPermissionLevel = PermissionLevel.forLevel(CommandHelper.getPermissionLevel(source));
        if (!thisPermissionLevel.canAffect(PermissionLevel.forLevel(permissionLevel))) {
            throw ERROR_INSUFFICIENT_PERMISSION.create(source, permissionLevel);
        }

        for (GameProfile target : targets) {
            if (!playerList.isOp(target)) {
                op(playerList, target, permissionLevel);
                ++successes;
                source.sendSuccess(createTranslation(source, "commands.op.success",
                        target.getName(), permissionLevel), true);
            }
        }

        if (successes == 0) {
            throw ERROR_ALREADY_OP.create(source);
        } else {
            return successes;
        }
    }

    private static void op(PlayerList playerList, GameProfile target, int permissionLevel) {
        final ServerOpList opList = playerList.getOps();

        opList.add(new ServerOpListEntry(target, permissionLevel, opList.canBypassPlayerLimit(target)));
        ServerPlayer player = playerList.getPlayer(target.getId());
        if (player != null) {
            playerList.sendPlayerPermissionLevel(player);
        }
    }
}
