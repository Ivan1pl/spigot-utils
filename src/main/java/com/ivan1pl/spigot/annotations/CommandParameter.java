package com.ivan1pl.spigot.annotations;

import java.lang.annotation.*;

/**
 * <p>Annotation used to mark method parameters as command parameters. Each parameter (except the first of type
 * {@code CommandSender}) should be marked with either CommandParameter or {@link CommandOption} annotation.</p>
 *
 * <p>Supported parameter types are: {@code boolean} (and its wrapper class {@code Boolean}), {@code int} (and its
 * wrapper class {@code Integer}), {@code long} (and its wrapper class {@code Long}), {@code String}.</p>
 *
 * @see CommandOption
 * @see Command
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandParameter {
    /**
     * Denotes whether the parameter is optional (if it is, defaultValue will be used when not provided).
     */
    boolean optional() default false;

    /**
     * Parameter name (used in usage description).
     */
    String name();

    /**
     * Parameter default value.
     */
    String defaultValue() default "";

    /**
     * Parameter description, used for command usage description.
     */
    String description() default "";
}
