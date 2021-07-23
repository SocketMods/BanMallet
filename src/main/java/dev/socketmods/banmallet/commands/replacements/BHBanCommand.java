package dev.socketmods.banmallet.commands.replacements;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import dev.socketmods.banmallet.PermissionLevel;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.server.management.BanList;
import net.minecraft.server.management.ProfileBanEntry;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Date;

import static dev.socketmods.banmallet.commands.arguments.DurationArgumentType.duration;
import static dev.socketmods.banmallet.commands.arguments.DurationArgumentType.getDuration;
import static net.minecraft.command.Commands.argument;
import static net.minecraft.command.Commands.literal;
import static net.minecraft.command.arguments.GameProfileArgument.gameProfile;
import static net.minecraft.command.arguments.GameProfileArgument.getGameProfiles;
import static net.minecraft.command.arguments.MessageArgument.getMessage;
import static net.minecraft.command.arguments.MessageArgument.message;
import static net.minecraft.util.text.TextComponentUtils.getDisplayName;

/**
 * BanMallet's replacement for {@link net.minecraft.command.impl.BanCommand}.
 */
public class BHBanCommand {
    private static final SimpleCommandExceptionType ERROR_ALREADY_BANNED = new SimpleCommandExceptionType(
            new TranslationTextComponent("commands.ban.failed"));

    public static LiteralArgumentBuilder<CommandSource> getNode() {
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

    private static int banPlayers(CommandContext<CommandSource> ctx, Collection<GameProfile> targets, Duration duration,
                                  @Nullable ITextComponent reason) throws CommandSyntaxException {
        final CommandSource source = ctx.getSource();
        final BanList banList = source.getServer().getPlayerList().getBans();
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
                ProfileBanEntry entry = new ProfileBanEntry(target, null, source.getTextName(), expiry,
                        reason == null ? null : reason.getString());
                banList.add(entry);
                ++successes;
                source.sendSuccess(new TranslationTextComponent("commands.banmallet.ban.success",
                        getDisplayName(target), durationString, entry.getReason()), true);
                ServerPlayerEntity player = source.getServer().getPlayerList().getPlayer(target.getId());
                if (player != null) {
                    player.connection.disconnect(
                            new TranslationTextComponent("multiplayer.disconnect.banned"));
                }
            }
        }

        if (successes == 0) {
            throw ERROR_ALREADY_BANNED.create();
        } else {
            return successes;
        }
    }
}
