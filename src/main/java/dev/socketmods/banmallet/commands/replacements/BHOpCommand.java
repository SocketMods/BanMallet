package dev.socketmods.banmallet.commands.replacements;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.socketmods.banmallet.CommandHelper;
import dev.socketmods.banmallet.PermissionLevel;
import net.minecraft.command.CommandSource;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.command.impl.OpCommand;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.OpEntry;
import net.minecraft.server.management.OpList;
import net.minecraft.server.management.PlayerList;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static com.mojang.brigadier.arguments.IntegerArgumentType.getInteger;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;
import static net.minecraft.command.arguments.GameProfileArgument.gameProfile;
import static net.minecraft.command.arguments.GameProfileArgument.getGameProfiles;

/**
 * BanMallet's replacement for {@link OpCommand}.
 */
public class BHOpCommand {
    private static final SimpleCommandExceptionType ERROR_ALREADY_OP = new SimpleCommandExceptionType(
            new TranslationTextComponent("commands.op.failed"));
    private static final DynamicCommandExceptionType ERROR_INSUFFICIENT_PERMISSION = new DynamicCommandExceptionType(
            s -> new TranslationTextComponent("commands.banmallet.op.insufficient_permission", s));

    public static LiteralArgumentBuilder<CommandSource> getNode() {
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

    static CompletableFuture<Suggestions> getSuggestions(final CommandContext<CommandSource> ctx,
                                                         final SuggestionsBuilder builder) {
        final PlayerList playerlist = ctx.getSource().getServer().getPlayerList();
        final Stream<String> players = playerlist.getPlayers().stream()
                .filter((player) -> !playerlist.isOp(player.getGameProfile()))
                .map((player) -> player.getGameProfile().getName());

        return ISuggestionProvider.suggest(players, builder);
    }

    private static int opPlayers(CommandContext<CommandSource> ctx, Collection<GameProfile> targets)
            throws CommandSyntaxException {
        final int permissionLevel = CommandHelper.getPermissionLevel(ctx.getSource());
        // One less the current operator's permission level
        return opPlayers(ctx, targets, permissionLevel - 1);
    }

    private static int opPlayers(CommandContext<CommandSource> ctx, Collection<GameProfile> targets,
                                 int permissionLevel) throws CommandSyntaxException {
        final CommandSource source = ctx.getSource();
        final MinecraftServer server = source.getServer();
        final PlayerList playerList = server.getPlayerList();
        int successes = 0;

        final PermissionLevel thisPermissionLevel = PermissionLevel.forLevel(CommandHelper.getPermissionLevel(source));
        if (!thisPermissionLevel.canAffect(PermissionLevel.forLevel(permissionLevel))) {
            throw ERROR_INSUFFICIENT_PERMISSION.create(permissionLevel);
        }

        for (GameProfile target : targets) {
            if (!playerList.isOp(target)) {
                op(playerList, target, permissionLevel);
                ++successes;
                source.sendSuccess(new TranslationTextComponent(
                        "commands.op.success", target.getName(), permissionLevel), true);
            }
        }

        if (successes == 0) {
            throw ERROR_ALREADY_OP.create();
        } else {
            return successes;
        }
    }

    private static void op(PlayerList playerList, GameProfile target, int permissionLevel) {
        final OpList opList = playerList.getOps();

        opList.add(new OpEntry(target, permissionLevel, opList.canBypassPlayerLimit(target)));
        ServerPlayerEntity player = playerList.getPlayer(target.getId());
        if (player != null) {
            playerList.sendPlayerPermissionLevel(player);
        }
    }
}
