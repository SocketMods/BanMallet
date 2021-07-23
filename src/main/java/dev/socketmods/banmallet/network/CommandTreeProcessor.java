package dev.socketmods.banmallet.network;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.RootCommandNode;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Package-private helper class to process and modify command trees.
 */
class CommandTreeProcessor {
    public static <S> RootCommandNode<S> processCommandTree(RootCommandNode<S> root, Predicate<CommandNode<S>> nodeFilter,
                                                            Function<ArgumentBuilder<S, ?>, ArgumentBuilder<S, ?>> modifier) {
        return (RootCommandNode<S>) processCommandNode(root, nodeFilter, modifier, new HashMap<>());
    }

    private static <S> CommandNode<S> processCommandNode(CommandNode<S> node, Predicate<CommandNode<S>> nodeFilter,
                                                         Function<ArgumentBuilder<S, ?>, ArgumentBuilder<S, ?>> modifier,
                                                         Map<CommandNode<S>, CommandNode<S>> newNodes) {
        final CommandNode<S> existingNode = newNodes.get(node);
        if (existingNode == null) {
            final CommandNode<S> newNode = cloneNode(node, nodeFilter, modifier, newNodes);
            newNodes.put(node, newNode);
            node.getChildren().forEach(child -> newNode.addChild(processCommandNode(child, nodeFilter, modifier, newNodes)));
            return newNode;
        } else {
            return existingNode;
        }
    }

    private static <S> CommandNode<S> cloneNode(CommandNode<S> node, Predicate<CommandNode<S>> nodeFilter,
                                                Function<ArgumentBuilder<S, ?>, ArgumentBuilder<S, ?>> modifier,
                                                Map<CommandNode<S>, CommandNode<S>> newNodes) {
        if (node instanceof RootCommandNode<?>) {
            return new RootCommandNode<>();
        } else {
            ArgumentBuilder<S, ?> builder = node.createBuilder();

            if (nodeFilter.test(node)) {
                if (node.getRedirect() != null) {
                    builder.forward(processCommandNode(node.getRedirect(), nodeFilter, modifier, newNodes),
                            node.getRedirectModifier(), node.isFork());
                }

                builder = modifier.apply(builder);
            }

            return builder.build();
        }
    }

    @SuppressWarnings("unchecked")
    public static <S> void copyBuilder(ArgumentBuilder<S, ?> from, ArgumentBuilder<S, ?> to) {
        to.executes(from.getCommand());
        to.requires(from.getRequirement());
        to.forward(from.getRedirect(), from.getRedirectModifier(), from.isFork());
        if (from instanceof RequiredArgumentBuilder && to instanceof RequiredArgumentBuilder) {
            final RequiredArgumentBuilder<S, ?> fromRequired = (RequiredArgumentBuilder<S, ?>) from;
            final RequiredArgumentBuilder<S, ?> toRequired = (RequiredArgumentBuilder<S, ?>) to;

            toRequired.suggests(fromRequired.getSuggestionsProvider());
        }
    }
}
