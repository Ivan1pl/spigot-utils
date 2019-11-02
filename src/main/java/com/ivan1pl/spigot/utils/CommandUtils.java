package com.ivan1pl.spigot.utils;

import com.google.common.collect.Maps;
import com.ivan1pl.spigot.annotations.Command;
import com.ivan1pl.spigot.annotations.CommandOption;
import com.ivan1pl.spigot.annotations.CommandPackage;
import com.ivan1pl.spigot.annotations.CommandParameter;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class used for preparing {@link Command} annotation processor and command executor.
 */
public class CommandUtils {

    private CommandUtils() {}

    static private Set<Method> getCommandMethods(JavaPlugin plugin) {
        Objects.requireNonNull(plugin);
        Set<Method> commandMethods = new HashSet<>();
        CommandPackage[] packages = plugin.getClass().getAnnotationsByType(CommandPackage.class);
        for (CommandPackage commandPackage : packages) {
            commandMethods.addAll(new Reflections(commandPackage.value(), new MethodAnnotationsScanner()).getMethodsAnnotatedWith(Command.class));
        }
        return commandMethods;
    }

    static private List<Command> getMethodCommands(Method m) {
        Command c = m.getDeclaredAnnotation(Command.class);
        Command.List list = m.getDeclaredAnnotation(Command.List.class);
        List<Command> result = new ArrayList<>();
        if (c != null) {
            result.add(c);
        }
        if (list != null) {
            result.addAll(Arrays.asList(list.value()));
        }
        return result;
    }

    /**
     * Process command-related annotation and set executor for each declared command.
     * @param plugin plugin instance
     */
    public static void initCommands(JavaPlugin plugin) {
        Set<Method> methods = getCommandMethods(plugin);
        Map<Method, List<Command>> methodCommands = Maps.asMap(methods, CommandUtils::getMethodCommands);
        Set<String> commandNames = methodCommands.values().stream()
                .flatMap(v -> v.stream().map(Command::command))
                .collect(Collectors.toSet());
        CommandExecutor executor = new CommandExecutor(plugin, methodCommands);
        for (String commandName : commandNames) {
            plugin.getCommand(commandName).setExecutor(executor);
            plugin.getLogger().info("Registered command: " + commandName);
        }
    }

    static ArgumentParser getArgumentParser(String command, String description, Annotation[][] paramAnnotations, Class<?>[] paramTypes, Boolean[] paramFlags) {
        ArgumentParser parser = ArgumentParsers.newFor("/" + command).addHelp(false).build()
                .description(description);
        parser.addArgument("-h", "--help")
                .action(new HelpArgumentAction())
                .help("show this help message and exit")
                .setDefault(Arguments.SUPPRESS);
        for (int i = 0; i < paramAnnotations.length; ++i) {
            Class<?> paramType = paramTypes != null && paramTypes.length > i ? paramTypes[i] : null;
            if (paramType != null && !isSupportedType(paramType) && !paramType.isAssignableFrom(CommandSender.class)) {
                throw new UnsupportedOperationException(String.format("The type %s is not supported.",
                        paramType.getCanonicalName()));
            } else if (paramType == null || isSupportedType(paramType)) {
                Annotation[] annotations = paramAnnotations[i];
                for (Annotation annotation : annotations) {
                    if (annotation instanceof CommandOption) {
                        CommandOption commandOption = (CommandOption) annotation;
                        String longName = commandOption.name().isEmpty() ?
                                null : "--" + commandOption.name();
                        String shortName = Character.isLetterOrDigit(commandOption.shortName()) ?
                                "-" + commandOption.shortName() : null;
                        boolean hasArg = !paramFlags[i];
                        String[] names;
                        if (longName != null && shortName != null) {
                            names = new String[] { shortName, longName };
                        } else if (longName != null) {
                            names = new String[] { longName };
                        } else if (shortName != null) {
                            names = new String[] { shortName };
                        } else {
                            throw new IllegalStateException("Option name not specified.");
                        }
                        Argument argument = parser.addArgument(names).help(commandOption.description());
                        if (!hasArg) {
                            argument.action(Arguments.storeTrue());
                        } else if (paramType != null) {
                            argument.type(paramType);
                            if (!commandOption.defaultValue().isEmpty()) {
                                argument.setDefault(commandOption.defaultValue());
                            }
                        } else {
                            if (!commandOption.defaultValue().isEmpty()) {
                                argument.setDefault(commandOption.defaultValue());
                            }
                        }
                    } else if (annotation instanceof CommandParameter) {
                        CommandParameter commandParameter = (CommandParameter) annotation;
                        Argument argument = parser.addArgument(commandParameter.name())
                                .help(commandParameter.description());
                        if (commandParameter.optional()) {
                            argument.nargs("?");
                        }
                        if (paramType != null) {
                            argument.type(paramType);
                        }
                        if (!commandParameter.defaultValue().isEmpty() && paramType != null) {
                            argument.setDefault(convertDefaultValue(paramType, commandParameter.defaultValue()));
                        }
                    }
                }
            }
        }

        return parser;
    }

    private static Object convertDefaultValue(Class<?> targetClass, String value) {
        Class<?> clazz;
        if (targetClass == boolean.class) {
            clazz = Boolean.class;
        } else if (targetClass == long.class) {
            clazz = Long.class;
        } else if (targetClass == int.class) {
            clazz = Integer.class;
        } else {
            clazz = targetClass;
        }

        try {
            return clazz.getConstructor(String.class).newInstance(value);
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            return value;
        }
    }

    static boolean isSupportedType(Class<?> type) {
        return type == boolean.class || type == Boolean.class || type == int.class || type == Integer.class ||
                type == long.class || type == Long.class || type == String.class;
    }

    private static class HelpArgumentAction implements ArgumentAction {

        @Override
        public void run(ArgumentParser parser, Argument arg,
                        Map<String, Object> attrs, String flag, Object value)
                throws ArgumentParserException {
            throw new HelpScreenException(parser);
        }

        @Override
        public boolean consumeArgument() {
            return false;
        }

        @Override
        public void onAttach(Argument arg) {
        }
    }
}
