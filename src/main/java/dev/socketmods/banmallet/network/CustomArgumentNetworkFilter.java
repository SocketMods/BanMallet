package dev.socketmods.banmallet.network;

import com.google.common.collect.ImmutableMap;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import dev.socketmods.banmallet.commands.arguments.DurationArgumentType;
import io.netty.channel.ChannelHandler;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fmllegacy.network.NetworkHooks;
import net.minecraftforge.network.NetworkFilters;
import net.minecraftforge.network.VanillaConnectionNetworkFilter;
import net.minecraftforge.network.VanillaPacketFilter;

import java.util.List;
import java.util.function.BiConsumer;

import static com.mojang.brigadier.builder.RequiredArgumentBuilder.argument;

/**
 * Packet filter to convert custom argument types such as {@link DurationArgumentType} into
 * {@link StringArgumentType#word()}.
 *
 * <p>In a normal Forge installation, the {@link VanillaConnectionNetworkFilter} is injected into the network pipeline
 * (by {@link NetworkFilters}), for the purpose of allowing compatibility between Forge servers and vanilla clients by
 * removing custom argument types (those not under the {@code minecraft} or {@code brigadier} namespaces) from the
 * {@linkplain ClientboundCommandsPacket command tree sync packet}. </p>
 *
 * <p>The reason for this is because of custom registered argument types. {@link ArgumentType}s are registered at the
 * {@link ArgumentTypes} with a {@linkplain ResourceLocation resource location} and an {@linkplain ArgumentSerializer},
 * to allow argument types to be synchronized over the network from server to client. However, if the client receives an
 * unrecognized argument type name (such as a custom argument type registered on the Forge server), it will fail to
 * deserialize the packet and disconnect. </p>
 *
 * <p>However, this means that autocompletion for commands with these stripped custom arguments will not work, since the
 * client has an incomplete command tree (however, executing the command will still work, as the full command is sent
 * over to the server for the actual execution). </p>
 *
 * <p>To remedy this, this packet filter changes selected custom argument types (such as {@link DurationArgumentType}
 * into the alternative, vanilla-compatible argument type of {@link StringArgumentType#word()}. This allows the full
 * command tree to be sent over to vanilla clients with only reduced functionality (namely, vanilla clients cannot parse
 * and validate the argument without sending it over to the server), while not requiring developers to workaround it by
 * having to use only vanilla-compatible argument types. </p>
 */
@ChannelHandler.Sharable
public class CustomArgumentNetworkFilter extends VanillaPacketFilter {
    public CustomArgumentNetworkFilter() {
        super(ImmutableMap.<Class<? extends Packet<?>>, BiConsumer<Packet<?>, List<? super Packet<?>>>>builder()
                .put(handler(ClientboundCommandsPacket.class, CustomArgumentNetworkFilter::filterCustomArguments))
                .build());
    }

    @Override
    protected boolean isNecessary(Connection manager) {
        return NetworkHooks.isVanillaConnection(manager);
    }

    private static ClientboundCommandsPacket filterCustomArguments(ClientboundCommandsPacket packet) {
        final RootCommandNode<SharedSuggestionProvider> rootNode = packet.getRoot();
        final RootCommandNode<SharedSuggestionProvider> modifiedRoot = CommandTreeProcessor.processCommandTree(rootNode,
                CustomArgumentNetworkFilter::filterNode, CustomArgumentNetworkFilter::modifyBuilder);

        return new ClientboundCommandsPacket(modifiedRoot);
    }

    private static <S> boolean filterNode(CommandNode<S> node) {
        return node instanceof ArgumentCommandNode<?, ?>
                && ((ArgumentCommandNode<?, ?>) node).getType() instanceof DurationArgumentType;
    }

    @SuppressWarnings("unchecked")
    private static <S> ArgumentBuilder<S, ?> modifyBuilder(ArgumentBuilder<S, ?> builder) {
        if (builder instanceof RequiredArgumentBuilder) {
            RequiredArgumentBuilder<S, ?> original = (RequiredArgumentBuilder<S, ?>) builder;
            RequiredArgumentBuilder<S, ?> modified = argument(original.getName(), StringArgumentType.word());

            CommandTreeProcessor.copyBuilder(original, modified);

            return modified;
        }

        return builder;
    }
}
