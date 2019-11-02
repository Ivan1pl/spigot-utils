package com.ivan1pl.spigot.annotations;

import java.lang.annotation.*;

/**
 * <p>Annotation used to mark method parameters as command options. Each parameter (except the first of type
 * {@code CommandSender}) should be marked with either CommandOption or {@link CommandParameter} annotation.</p>
 *
 * <p>If the annotated parameter is a {@code boolean}, no additional data needs to be passed to the command and the
 * value of the parameter will be set to true if the option is present. Otherwise, the word immediately following the
 * option will be used to set the value of the parameter.</p>
 *
 * <p>Supported parameter types are: {@code boolean} (and its wrapper class {@code Boolean}), {@code int} (and its
 * wrapper class {@code Integer}), {@code long} (and its wrapper class {@code Long}), {@code String}.</p>
 *
 * @see CommandParameter
 * @see Command
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CommandOption {
    /**
     * Option long name (used in command as {@code --name}, {@code --name value}).
     */
    String name() default "";

    /**
     * Option short name (used in command as {@code -s}, {@code -s value}).
     */
    char shortName() default '\0';

    /**
     * Option default value, used when the option was not passed to the command.
     * Applicable only to non-boolean parameters.
     */
    String defaultValue() default "";

    /**
     * Option description, used for command usage description.
     */
    String description() default "";
}
