package dev.socketmods.banmallet.util;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;

/**
 * Eager text translation utilities.
 *
 * @author sciwhiz12
 */
public final class TranslationUtil {
    private TranslationUtil() {
    } // Prevent instantiation

    /* Copied from net.minecraftforge.server.command.TextComponentHelper, and modified to suit our purpose */

    /**
     * Creates a {@link BaseComponent} from the given translation key, depending on the {@code lazyTranslate} parameter.
     * <p>
     * If {@code lazyTranslate} is {@code false}, then the returned value is a {@link TextComponent} with the message
     * specified by the translation key being eagerly evaluated now. This text component is safe to send to clients, as it does
     * not use a translation key.
     * <p>
     * If {@code lazyTranslate} is {@code true}, then the returned value is a {@link TranslatableComponent} with the
     * translation key and given arguments passed into it, and the contents of the text component is lazily evaluated (on first
     * use of the text component).
     *
     * @param lazyTranslate Whether to lazily translate the message
     * @param translation   The translation key
     * @param args          Extra arguments to the message
     * @return a {@link BaseComponent} with the specified message
     */
    public static BaseComponent createTranslation(boolean lazyTranslate, final String translation, final Object... args) {
        TranslatableComponent text = new TranslatableComponent(translation, args);
        return lazyTranslate ? text : eagerTranslate(text);
    }

    public static BaseComponent createTranslation(@Nullable ServerPlayer entity, String translationKey, Object... args) {
        return createTranslation(entity == null || !NetworkHooks.isVanillaConnection(entity.connection.connection),
                translationKey, args);
    }

    public static BaseComponent createTranslation(CommandSourceStack source, String translationKey, Object... args) {
        final Entity entity = source.getEntity();
        return createTranslation(!(entity instanceof ServerPlayer)
                        || !NetworkHooks.isVanillaConnection(((ServerPlayer) entity).connection.connection),
                translationKey, args);
    }

    public static BaseComponent eagerTranslate(final TranslatableComponent component) {
        Object[] oldArgs = component.getArgs();
        Object[] newArgs = new Object[oldArgs.length];

        for (int i = 0; i < oldArgs.length; i++) {
            Object obj = oldArgs[i];
            if (obj instanceof TranslatableComponent translatable) {
                newArgs[i] = eagerTranslate(translatable);
            } else if (obj instanceof MutableComponent mutable) {
                newArgs[i] = eagerCheckStyle(mutable);
            } else {
                newArgs[i] = oldArgs[i];
            }
        }

        TranslatableComponent result =
                new TranslatableComponent(Language.getInstance().getOrDefault(component.getKey()), newArgs);
        result.setStyle(component.getStyle());

        for (Component sibling : component.getSiblings()) {
            if (sibling instanceof TranslatableComponent translatableSibling) {
                result.append(eagerTranslate(translatableSibling));
            } else if (sibling instanceof MutableComponent mutableSibling) {
                result.append(eagerCheckStyle(mutableSibling));
            } else {
                result.append(sibling);
            }
        }

        return eagerCheckStyle(result);
    }

    public static <Text extends MutableComponent> Text eagerCheckStyle(Text component) {
        Style style = component.getStyle();
        HoverEvent hover = style.getHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_TEXT) {
            Component hoverText = hover.getValue(HoverEvent.Action.SHOW_TEXT);
            if (hoverText instanceof TranslatableComponent) {
                style = style.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, eagerTranslate((TranslatableComponent) hoverText))
                );
            }
        }
        component.setStyle(style);
        return component;
    }
}
