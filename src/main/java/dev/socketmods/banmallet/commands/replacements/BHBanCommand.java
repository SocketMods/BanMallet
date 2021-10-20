package dev.socketmods.banmallet.commands.replacements;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.socketmods.banmallet.PermissionLevel;
import dev.socketmods.banmallet.commands.exception.TranslatedCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;

import static dev.socketmods.banmallet.commands.arguments.DurationArgumentType.duration;
import static dev.socketmods.banmallet.commands.arguments.DurationArgumentType.getDuration;
import static dev.socketmods.banmallet.commands.exception.TranslatedCommandExceptionType.translatedExceptionType;
import static dev.socketmods.banmallet.util.TranslationUtil.createTranslation;
import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;
import static net.minecraft.commands.arguments.GameProfileArgument.gameProfile;
import static net.minecraft.commands.arguments.GameProfileArgument.getGameProfiles;
import static net.minecraft.commands.arguments.MessageArgument.getMessage;
import static net.minecraft.commands.arguments.MessageArgument.message;
import static net.minecraft.network.chat.ComponentUtils.getDisplayName;

/**
 * BanManet.minecraft.network.chat.ComponentUtilst.command.impl.BanCommand}.
 */
public class BHBanCommand {
    private static final TranslatedCommandExceptionType ERROR_ALREADY_BANNED = translatedExceptionType("commands.ban.failed");

    public static LiteralArgumentBuilder<CommandSourceStack> getNode() {
        return literal("ban").requires(PermissionLevel.MODERATOR)
                .then(argument("targets", gameProfile())
                        .then(argument("duration", duration())
                                .then(argument("reason", message())
                                        .executes(ctx -> banPlayers(ctx,
                                                getGameProfiles(ctx, "targets"),
                                                getDuration(ctx, "duration"),
                                                getMessage(ctx, "reason")))
                                )
                                .executes(ctx -> banPlayers(ctx, getGameProfiles(ctx, "targets"),
                                        getDuration(ctx, "duration"), null))
                        )
                        .executes(ctx -> banPlayers(ctx, getGameProfiles(ctx, "targets"),
                                ChronoUnit.FOREVER.getDuration(), null))
                );
    }

    private static int banPlayers(CommandContext<CommandSourceStack> ctx, Collection<GameProfile> targets, Duration duration,
                                  @Nullable Component reason) throws CommandSyntaxException {
        final CommandSourceStack source = ctx.getSource();
        final UserBanList banList = source.getServer().getPlayerList().getBans();
        int successes = 0;

        final Date expiry;
        final String durationString;
        if (ChronoUnit.FOREVER.getDuration().equals(duration)) {
            expiry = null;
            durationString = "forever";
        } else {
            expiry = new Date(Instant.now().plus(duration).toEpochMilli());
            durationString = DurationFormatUtils.formatDurationWords(duration.toMillis(), true, true);
        }
        for (GameProfile target : targets) {
            if (!banList.isBanned(target)) {
                UserBanListEntry entry = new UserBanListEntry(target, null, source.getTextName(), expiry,
                        reason == null ? null : reason.getString());
                banList.add(entry);
                ++successes;
                source.sendSuccess(createTranslation(source, "commands.banmallet.ban.success",
                        getDisplayName(target), durationString, entry.getReason()), true);
                ServerPlayer player = source.getServer().getPlayerList().getPlayer(target.getId());
                if (player != null) {
                    player.connection.disconnect(
                            new TranslatableComponent("multiplayer.disconnect.banned"));
                }
            }
        }

        if (successes == 0) {
            throw ERROR_ALREADY_BANNED.create(source);
        } else {
            return successes;
        }
    }
}
