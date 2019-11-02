package com.ivan1pl.spigot.utils;

import com.google.common.collect.Maps;
import com.ivan1pl.spigot.annotations.Command;
import com.ivan1pl.spigot.annotations.CommandOption;
import com.ivan1pl.spigot.annotations.CommandParameter;
import net.sourceforge.argparse4j.helper.HelpScreenException;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

class CommandExecutor implements org.bukkit.command.CommandExecutor {
    private final JavaPlugin plugin;
    private final Map<Method, List<CommandEntry>> commands;
    private final Map<Class<?>, Object> methodOwners = new HashMap<>();

    CommandExecutor(JavaPlugin plugin, Map<Method, List<Command>> methodCommands) {
        this.plugin = plugin;
        this.commands = Maps.transformEntries(methodCommands,
                (m, l) -> m == null || l == null ? null : l.stream().map(
                        c -> new CommandEntry(c.command(), getParserForCommand(m, c)))
                        .collect(Collectors.toList()));
    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        boolean displayingHelp = false;
        for (Method m : commands.keySet()) {
            for (CommandEntry commandEntry : commands.get(m)) {
                if (commandEntry.name.equals(command.getName())) {
                    try {
                        Namespace namespace = commandEntry.argumentParser.parseArgs(args);
                        Object thisObject = getMethodOwner(m);
                        Object[] parameters = getMethodParameters(sender, m, namespace);
                        m.invoke(thisObject, parameters);
                        return true;
                    } catch (HelpScreenException e) {
                        sender.sendMessage(commandEntry.argumentParser.formatHelp());
                        //not returning true, might be other @Command entries to handle this - help should be displayed
                        //for them as well.
                        displayingHelp = true;
                    } catch (ArgumentParserException e) {
                        //nop, maybe other @Command entry for this command will handle this input.
                    } catch (IllegalAccessException | InstantiationException e) {
                        plugin.getLogger().log(Level.SEVERE,
                                "Failed to instantiate declaring class for method: " + m.toString(), e);
                    } catch (InvocationTargetException e) {
                        plugin.getLogger().log(Level.SEVERE,
                                "Failed to invoke method: " + m.toString(), e);
                    }
                }
            }
        }
        return displayingHelp;
    }

    private Object getMethodOwner(Method m) throws IllegalAccessException, InstantiationException {
        Class<?> clazz = m.getDeclaringClass();
        Object result = methodOwners.get(clazz);
        if (result == null) {
            result = clazz.newInstance();
        }
        methodOwners.put(clazz, result);
        return result;
    }

    private Object[] getMethodParameters(CommandSender sender, Method m, Namespace namespace) {
        Class<?>[] paramTypes = m.getParameterTypes();
        Annotation[][] paramAnnotations = m.getParameterAnnotations();
        Object[] result = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; ++i) {
            if (paramTypes[i].isAssignableFrom(CommandSender.class)) {
                result[i] = sender;
            } else if (CommandUtils.isSupportedType(paramTypes[i])) {
                String paramName = null;
                Annotation[] annotations = paramAnnotations[i];
                for (Annotation annotation : annotations) {
                    if (annotation instanceof CommandOption) {
                        CommandOption commandOption = (CommandOption) annotation;
                        String longName = commandOption.name().isEmpty() ?
                                null : commandOption.name();
                        String shortName = Character.isLetterOrDigit(commandOption.shortName()) ?
                                Character.toString(commandOption.shortName()) : null;
                        paramName = longName == null ? shortName : longName;
                    } else if (annotation instanceof CommandParameter) {
                        CommandParameter commandParameter = (CommandParameter) annotation;
                        paramName = commandParameter.name();
                    }
                }
                if (paramName != null) {
                    if (paramTypes[i] == boolean.class || paramTypes[i] == Boolean.class) {
                        result[i] = namespace.getBoolean(paramName);
                    } else if (paramTypes[i] == int.class || paramTypes[i] == Integer.class) {
                        result[i] = namespace.getInt(paramName);
                    } else if (paramTypes[i] == long.class || paramTypes[i] == Long.class) {
                        result[i] = namespace.getLong(paramName);
                    } else if (paramTypes[i] == String.class) {
                        result[i] = namespace.getString(paramName);
                    } else {
                        result[i] = null;
                    }
                } else {
                    result[i] = null;
                }
            } else {
                throw new UnsupportedOperationException(String.format("The type %s is not supported.",
                        paramTypes[i].getCanonicalName()));
            }
        }
        return result;
    }

    private ArgumentParser getParserForCommand(Method m, Command c) {
        Class<?>[] paramTypes = m.getParameterTypes();
        Annotation[][] paramAnnotations = m.getParameterAnnotations();
        if (paramTypes.length == paramAnnotations.length) {
            Boolean[] paramFlags = Arrays.stream(paramTypes)
                    .map(t -> t == boolean.class || t == Boolean.class)
                    .collect(Collectors.toList())
                    .toArray(new Boolean[paramTypes.length]);
            return CommandUtils.getArgumentParser(c.command(), c.description(), paramAnnotations, paramTypes, paramFlags);
        } else {
            throw new IllegalStateException("Internal error.");
        }
    }

    private static class CommandEntry {
        final String name;
        final ArgumentParser argumentParser;

        CommandEntry(String name, ArgumentParser argumentParser) {
            this.name = name;
            this.argumentParser = argumentParser;
        }
    }
}
