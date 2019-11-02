package com.ivan1pl.spigot.annotations;

import java.lang.annotation.*;

/**
 * Annotation used to mark methods as executable commands.
 *
 * This annotation can be repeated.
 *
 * @see CommandOption
 * @see CommandParameter
 * @see CommandPackage
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Command.List.class)
@Documented
public @interface Command {
    /**
     * Command name.
     */
    String command();

    /**
     * Aliases for this command.
     */
    String[] aliases() default {};

    /**
     * Command description.
     */
    String description() default "";

    /**
     * The most basic permission node required to use the command.
     */
    String permission() default "";

    /**
     * A no-permission message which is displayed to a user if they do not have the required permission to use this
     * command.
     */
    String permissionMessage() default "";

    /**
     * Annotation used as a wrapper for repeating {@link Command} annotation.
     *
     * Should not be used directly. Just use multiple {@link Command} annotations, they will be wrapped
     * automatically.
     *
     * @see Command
     */
    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @interface List {
        /**
         * Array of {@link Command} annotations.
         */
        Command[] value();
    }
}
