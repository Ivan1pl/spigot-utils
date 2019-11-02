package com.ivan1pl.spigot.annotations;

import java.lang.annotation.*;

/**
 * Annotation used to mark the plugin main class with packages to scan for commands.
 *
 * This annotation can be repeated.
 *
 * @see Command
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(CommandPackage.List.class)
public @interface CommandPackage {
    /**
     * Path of the package containing command definitions.
     */
    String value();

    /**
     * Annotation used as a wrapper for repeating {@link CommandPackage} annotation.
     *
     * Should not be used directly. Just use multiple {@link CommandPackage} annotations, they will be wrapped
     * automatically.
     *
     * @see CommandPackage
     */
    @Target({ElementType.TYPE})
    @Retention(RetentionPolicy.RUNTIME)
    @interface List {
        /**
         * Array of {@link CommandPackage} annotations.
         */
        CommandPackage[] value();
    }
}
