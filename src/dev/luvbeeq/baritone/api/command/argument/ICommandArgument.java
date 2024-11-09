package dev.luvbeeq.baritone.api.command.argument;

import dev.luvbeeq.baritone.api.command.argparser.IArgParser;
import dev.luvbeeq.baritone.api.command.exception.CommandInvalidTypeException;
import net.minecraft.util.Direction;

/**
 * A {@link ICommandArgument} is an immutable object representing one command argument. It contains data on the index of
 * that argument, its value, and the rest of the string that argument was found in
 * <p>
 * You're recommended to use {@link IArgConsumer}}s to handle these.
 *
 * @author Brady
 * @since 10/2/2019
 */
public interface ICommandArgument {

    /**
     * @return The index of this command argument in the list of command arguments generated
     */
    int getIndex();

    /**
     * @return The raw value of just this argument
     */
    String getValue();

    /**
     * @return The raw value of the remaining arguments after this one was captured
     */
    String getRawRest();

    /**
     * Gets an enum value from the enum class with the same name as this argument's value
     * <p>
     * For example if you getEnum as an {@link Direction}, and this argument's value is "up", it will return {@link
     * Direction#UP}
     *
     * @param enumClass The enum class to search
     * @return An enum constant of that class with the same name as this argument's value
     * @throws CommandInvalidTypeException If the constant couldn't be found
     * @see IArgConsumer#peekEnum(Class)
     * @see IArgConsumer#peekEnum(Class, int)
     * @see IArgConsumer#peekEnumOrNull(Class)
     * @see IArgConsumer#peekEnumOrNull(Class, int)
     * @see IArgConsumer#getEnum(Class)
     * @see IArgConsumer#getEnumOrNull(Class)
     */
    <E extends Enum<?>> E getEnum(Class<E> enumClass) throws CommandInvalidTypeException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse this argument into the specified class
     *
     * @param type The class to parse this argument into
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException If the parsing failed
     */
    <T> T getAs(Class<T> type) throws CommandInvalidTypeException;

    /**
     * Tries to use a <b>stateless</b> {@link IArgParser} to parse this argument into the specified class
     *
     * @param type The class to parse this argument into
     * @return If the parser succeeded
     */
    <T> boolean is(Class<T> type);

    /**
     * Tries to use a <b>stated</b> {@link IArgParser} to parse this argument into the specified class
     *
     * @param type The class to parse this argument into
     * @return An instance of the specified type
     * @throws CommandInvalidTypeException If the parsing failed
     */
    <T, S> T getAs(Class<T> type, Class<S> stateType, S state) throws CommandInvalidTypeException;

    /**
     * Tries to use a <b>stated</b> {@link IArgParser} to parse this argument into the specified class
     *
     * @param type The class to parse this argument into
     * @return If the parser succeeded
     */
    <T, S> boolean is(Class<T> type, Class<S> stateType, S state);
}