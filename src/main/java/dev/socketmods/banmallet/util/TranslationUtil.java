package dev.socketmods.banmallet.util;

import net.minecraft.command.CommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.fml.network.NetworkHooks;

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
     * Creates a {@link TextComponent} from the given translation key, depending on the {@code lazyTranslate} parameter.
     * <p>
     * If {@code lazyTranslate} is {@code false}, then the returned value is a {@link StringTextComponent} with the message
     * specified by the translation key being eagerly evaluated now. This text component is safe to send to clients, as it does
     * not use a translation key.
     * <p>
     * If {@code lazyTranslate} is {@code true}, then the returned value is a {@link TranslationTextComponent} with the
     * translation key and given arguments passed into it, and the contents of the text component is lazily evaluated (on first
     * use of the text component).
     *
     * @param lazyTranslate Whether to lazily translate the message
     * @param translation   The translation key
     * @param args          Extra arguments to the message
     * @return a {@link TextComponent} with the specified message
     */
    public static TextComponent createTranslation(boolean lazyTranslate, final String translation, final Object... args) {
        TranslationTextComponent text = new TranslationTextComponent(translation, args);
        return lazyTranslate ? text : eagerTranslate(text);
    }

    public static TextComponent createTranslation(@Nullable ServerPlayerEntity entity, String translationKey, Object... args) {
        return createTranslation(entity == null || !NetworkHooks.isVanillaConnection(entity.connection.connection),
                translationKey, args);
    }

    public static TextComponent createTranslation(CommandSource source, String translationKey, Object... args) {
        final Entity entity = source.getEntity();
        return createTranslation(!(entity instanceof ServerPlayerEntity)
                        || !NetworkHooks.isVanillaConnection(((ServerPlayerEntity) entity).connection.connection),
                translationKey, args);
    }

    public static TextComponent eagerTranslate(final TranslationTextComponent component) {
        Object[] oldArgs = component.getArgs();
        Object[] newArgs = new Object[oldArgs.length];

        for (int i = 0; i < oldArgs.length; i++) {
            Object obj = oldArgs[i];
            if (obj instanceof TranslationTextComponent) {
                newArgs[i] = eagerTranslate((TranslationTextComponent) obj);
            } else if (obj instanceof IFormattableTextComponent) {
                newArgs[i] = eagerCheckStyle((IFormattableTextComponent) obj);
            } else {
                newArgs[i] = oldArgs[i];
            }
        }

        TranslationTextComponent result =
                new TranslationTextComponent(LanguageMap.getInstance().getOrDefault(component.getKey()), newArgs);
        result.setStyle(component.getStyle());

        for (ITextComponent sibling : component.getSiblings()) {
            if (sibling instanceof TranslationTextComponent) {
                result.append(eagerTranslate((TranslationTextComponent) sibling));
            } else if (sibling instanceof IFormattableTextComponent) {
                result.append(eagerCheckStyle((IFormattableTextComponent) sibling));
            } else {
                result.append(sibling);
            }
        }

        return eagerCheckStyle(result);
    }

    public static <Text extends IFormattableTextComponent> Text eagerCheckStyle(Text component) {
        Style style = component.getStyle();
        HoverEvent hover = style.getHoverEvent();
        if (hover != null && hover.getAction() == HoverEvent.Action.SHOW_TEXT) {
            ITextComponent hoverText = hover.getValue(HoverEvent.Action.SHOW_TEXT);
            if (hoverText instanceof TranslationTextComponent) {
                style = style.withHoverEvent(
                        new HoverEvent(HoverEvent.Action.SHOW_TEXT, eagerTranslate((TranslationTextComponent) hoverText))
                );
            }
        }
        component.setStyle(style);
        return component;
    }
}
