package dev.socketmods.banmallet;

import net.minecraft.command.CommandSource;
import net.minecraft.command.impl.*;
import net.minecraft.server.dedicated.ServerProperties;
import net.minecraft.tileentity.CommandBlockLogic;

import java.util.NoSuchElementException;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * A permission level denotes the access that a player or other entity has to various server commands.
 * <p>
 * A higher permission level implies all the permissions of the lower levels.
 */
public enum PermissionLevel implements IntSupplier, Predicate<CommandSource> {
    /**
     * Default permission level which implies no permissions. All players are part of this permission level by default.
     */
    NONE(0),
    /**
     * Permission level for server builders, which permits them to bypass the spawn protection, allowing them to build
     * with the world spawn area.
     */
    BUILDER(1),
    /**
     * Permission level for server gamemasters, with access to world-altering commands such as {@link TimeCommand
     * /time}, {@link GameModeCommand /gamemode}, {@link ExecuteCommand /execute}, and others.
     * <p>
     * {@linkplain CommandBlockLogic Command blocks} and {@linkplain ServerProperties#functionPermissionLevel functions}
     * have this permission level by default.
     */
    GAMEMASTER(2),
    /**
     * Permission level for server moderators, with access to multiplayer management commands such as {@link BanCommand
     * /ban}, {@link OpCommand /op}, and others.
     */
    MODERATOR(3),
    /**
     * Permission level for server administrators, with access to server management commands such as {@link StopCommand
     * /stop}, {@link SaveAllCommand /save-all}, and others.
     */
    ADMIN(4);

    private final int permissionLevel;

    PermissionLevel(int permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    @Override
    public int getAsInt() {
        return permissionLevel;
    }

    public int asInt() {
        return permissionLevel;
    }

    @Override
    public boolean test(CommandSource source) {
        return source.hasPermission(permissionLevel);
    }

    public boolean canAffect(PermissionLevel level) {
        // A level can affect the same level or lower
        return this.asInt() >= level.asInt();
    }

    public static PermissionLevel forLevel(int permissionLevel) {
        for (PermissionLevel level : values()) {
            if (level.asInt() == permissionLevel) {
                return level;
            }
        }
        throw new NoSuchElementException("No permission level for " + permissionLevel);
    }
}
