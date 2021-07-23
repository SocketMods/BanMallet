package dev.socketmods.banmallet.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.command.ISuggestionProvider;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class DurationArgumentType implements ArgumentType<Duration> {
    private static final SimpleCommandExceptionType EXPECTED_UNIT = new SimpleCommandExceptionType(
            new TranslationTextComponent("parsing.banmallet.time_duration.expected"));
    private static final DynamicCommandExceptionType INVALID_UNIT = new DynamicCommandExceptionType(
            s -> new TranslationTextComponent("parsing.banmallet.time_duration.invalid", s));
    private static final DynamicCommandExceptionType HIGHER_UNIT_THAN_PREVIOUS = new DynamicCommandExceptionType(
            s -> new TranslationTextComponent("parsing.banmallet.time_duration.higher", s));

    private static final Collection<String> SUGGESTED_UNITS = Arrays.asList("y", "M", "d", "h", "m", "s");
    private static final Collection<String> EXAMPLES = Arrays.asList(
            "1s", "1m", "1h", "1d", "1M", "1y", "forever",
            "1m1s", "1h1m1s", "1d1h1m1s", "1M1d1h1m1s", "1y1M1d1h1m1s"
    );

    public static DurationArgumentType duration() {
        return new DurationArgumentType();
    }

    public static Duration getDuration(CommandContext<?> context, String name) {
        return context.getArgument(name, Duration.class);
    }

    private DurationArgumentType() {
    }

    @Override
    public Duration parse(StringReader reader) throws CommandSyntaxException {
        final int startCursor = reader.getCursor();
        final String string = reader.readUnquotedString();
        reader.setCursor(startCursor);

        if (string.equals("forever")) {
            return ChronoUnit.FOREVER.getDuration();
        }

        Duration totalDuration = Duration.ZERO;
        Duration lastDuration = ChronoUnit.FOREVER.getDuration();
        do {
            try {
                final int amountCursor = reader.getCursor();
                final int amount = reader.readInt();

                if (!reader.canRead()) {
                    reader.setCursor(amountCursor);
                    throw EXPECTED_UNIT.createWithContext(reader);
                }

                final int unitCursor = reader.getCursor();
                final char unitChar = reader.read();
                final Duration durationUnit = parseDurationUnit(unitChar);

                if (durationUnit == null) {
                    reader.setCursor(unitCursor);
                    throw INVALID_UNIT.createWithContext(reader, unitChar);
                }

                if (lastDuration.compareTo(durationUnit) < 0) {
                    reader.setCursor(unitCursor);
                    throw HIGHER_UNIT_THAN_PREVIOUS.createWithContext(reader, unitChar);
                }
                lastDuration = durationUnit;

                totalDuration = totalDuration.plus(durationUnit.multipliedBy(amount));
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw e;
            }
        } while (reader.canRead() && (Character.isLetter(reader.peek()) || StringReader.isAllowedNumber(reader.peek())));

        return totalDuration;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        final String beforeRemaining = builder.getInput().substring(0, builder.getStart());
        if (Character.isDigit(beforeRemaining.charAt(beforeRemaining.length() - 1))) {
            // This is a number, so suggest a unit
            return ISuggestionProvider.suggest(SUGGESTED_UNITS, builder);
        }
        if (Character.isWhitespace(beforeRemaining.charAt(beforeRemaining.length() - 1))) {
            // Nothing yet, so suggest 'forever'
            return ISuggestionProvider.suggest(Collections.singleton("forever"), builder);
        }
        return Suggestions.empty();
    }

    @Nullable
    private static Duration parseDurationUnit(final char unit) {
        switch (unit) {
            case 'Y':
            case 'y':
                return Duration.ofDays(365); // Years
            case 'M':
                return Duration.ofDays(30); // Months
            case 'W':
            case 'w':
                return Duration.ofDays(7); // Weeks
            case 'D':
            case 'd':
                return Duration.ofDays(1); // Days
            case 'H':
            case 'h':
                return Duration.ofHours(1); // Hours
            case 'm':
                return Duration.ofMinutes(1); // Minutes
            case 'S':
            case 's':
                return Duration.ofSeconds(1); // Seconds
        }
        return null;
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }
}
