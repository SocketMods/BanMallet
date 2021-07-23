package dev.socketmods.banmallet.util;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import net.minecraft.command.CommandSource;

import java.lang.reflect.Field;
import java.util.Map;

import static net.minecraftforge.fml.common.ObfuscationReflectionHelper.findField;

/**
 * Hacky reflection-based helper for manipulating {@link CommandNode}s.
 */
public final class CommandHelper {
    // The following three are from Brigadier, and are not obfuscated
    private static final Field CHILDREN_FIELD = findField(CommandNode.class, "children");
    private static final Field LITERALS_FIELD = findField(CommandNode.class, "literals");
    private static final Field ARGUMENTS_FIELD = findField(CommandNode.class, "arguments");
    private static final Field PERMISSION_LEVEL_FIELD = findField(CommandSource.class, "field_197044_f");

    private CommandHelper() { // Prevent instantiation
    }

    public static void init() {
    }

    public static int getPermissionLevel(CommandSource source) {
        return get(PERMISSION_LEVEL_FIELD, source);
    }

    public static <T> void replaceAndRegister(CommandDispatcher<T> dispatcher, LiteralArgumentBuilder<T> node, boolean alwaysRegister) {
        final RootCommandNode<T> root = dispatcher.getRoot();
        final String literal = node.getLiteral();
        final CommandNode<T> existingNode = root.getChild(literal);
        if (existingNode != null) {
            removeChild(root, existingNode);
        } else if (!alwaysRegister) {
            return;
        }
        dispatcher.register(node);
    }

    public static <T> void replaceAndRegister(CommandDispatcher<T> dispatcher, LiteralArgumentBuilder<T> node) {
        replaceAndRegister(dispatcher, node, true);
    }

    static void removeChild(CommandNode<?> parentNode, CommandNode<?> childNode) {
        // When adding a child node to a parent node, the parent node does the following: (see CommandNode#addChild)
        // 1) adds the node to the `children` name-to-node map
        // 2) if a literal node (LiteralCommandNode), add it to the `literals` name-to-node map
        // 3) if an argument node (ArgumentsCommandNode), add it to the `arguments` name-to-node map
        // 4) Re-sort the `children` name-to-node map, comparing by value, into a new LinkedHashMap<>()

        // To remove the child node, we remove the nodes from the maps (and we don't need to resort the map)

        // Step 1: remove the node from the `children` map
        final Map<String, CommandNode<?>> children = get(CHILDREN_FIELD, parentNode);
        children.remove(childNode.getName());

        // Step 2: Remove from the `literals` or `arguments` map
        Map<String, CommandNode<?>> typedMap = null;
        if (childNode instanceof LiteralCommandNode) {
            typedMap = get(LITERALS_FIELD, parentNode);
        } else if (childNode instanceof ArgumentCommandNode) {
            typedMap = get(ARGUMENTS_FIELD, parentNode);
        }
        if (typedMap != null) {
            typedMap.remove(childNode.getName());
        }

    }

    @SuppressWarnings("unchecked")
    public static <T> T get(Field field, Object instance) {
        return (T) LambdaUtil.uncheck(() -> field.get(instance));
    }
}
