package dev.socketmods.banmallet.commands.exception;

import com.mojang.brigadier.exceptions.CommandExceptionType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;

import static dev.socketmods.banmallet.util.TranslationUtil.createTranslation;

public class TranslatedCommandExceptionType implements CommandExceptionType {
    private final String translationKey;

    public static TranslatedCommandExceptionType translatedExceptionType(String translationKey) {
        return new TranslatedCommandExceptionType(translationKey);
    }

    protected TranslatedCommandExceptionType(String translationKey) {
        this.translationKey = translationKey;
    }

    public CommandSyntaxException create(CommandSourceStack source, Object... args) {
        return new CommandSyntaxException(this, createTranslation(source, translationKey, args));
    }
}
