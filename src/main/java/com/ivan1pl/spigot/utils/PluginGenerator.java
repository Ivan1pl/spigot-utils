package com.ivan1pl.spigot.utils;

import com.google.auto.service.AutoService;
import com.google.common.collect.Lists;
import com.ivan1pl.spigot.annotations.*;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compile-time annotation processor used for generating plugin.yml file.
 */
@AutoService(Processor.class)
public class PluginGenerator extends AbstractProcessor {
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Map<String, Object> pluginFile = new LinkedHashMap<>();

        Set<? extends Element> pluginElements = roundEnvironment.getElementsAnnotatedWith(Plugin.class);
        if (pluginElements == null || pluginElements.size() != 1) {
            return false;
        }
        Element pluginElement = pluginElements.iterator().next();
        Plugin pluginAnnotation = pluginElement.getAnnotation(Plugin.class);
        pluginFile.put("main", pluginElement.asType().toString());
        pluginFile.put("name", pluginAnnotation.name());
        pluginFile.put("version", pluginAnnotation.version());
        if (!pluginAnnotation.description().isEmpty()) {
            pluginFile.put("description", pluginAnnotation.description());
        }
        if (!pluginAnnotation.apiVersion().isEmpty()) {
            pluginFile.put("api-version", pluginAnnotation.apiVersion());
        }
        pluginFile.put("load", pluginAnnotation.loadStage().name());
        if (pluginAnnotation.authors().length == 1) {
            pluginFile.put("author", pluginAnnotation.authors()[0]);
        } else if (pluginAnnotation.authors().length > 1) {
            pluginFile.put("authors", Arrays.asList(pluginAnnotation.authors()));
        }
        if (!pluginAnnotation.website().isEmpty()) {
            pluginFile.put("website", pluginAnnotation.website());
        }
        if (pluginAnnotation.depend().length > 0) {
            pluginFile.put("depend", Arrays.asList(pluginAnnotation.depend()));
        }
        if (!pluginAnnotation.prefix().isEmpty()) {
            pluginFile.put("prefix", pluginAnnotation.prefix());
        }
        if (pluginAnnotation.softDepend().length > 0) {
            pluginFile.put("softdepend", Arrays.asList(pluginAnnotation.softDepend()));
        }
        if (pluginAnnotation.loadBefore().length > 0) {
            pluginFile.put("loadbefore", Arrays.asList(pluginAnnotation.loadBefore()));
        }

        Map<String, Object> permissions = processPermissions(pluginAnnotation.permissions());
        if (permissions != null && !permissions.isEmpty()) {
            pluginFile.put("permissions", permissions);
        }

        Map<String, Object> commands = processCommands(roundEnvironment);
        if (commands != null && !commands.isEmpty()) {
            pluginFile.put("commands", commands);
        }

        try {
            FileObject resource = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "", "plugin.yml");
            DumperOptions options = new DumperOptions();
            options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            options.setPrettyFlow(true);

            Yaml yaml = new Yaml(options);
            String output = yaml.dump(pluginFile);
            Writer writer = resource.openWriter();
            writer.write(output);
            writer.close();
            return true;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, "Error saving resource.");
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, sw.toString());
            return false;
        }
    }

    private Map<String, Object> processPermissions(Permission[] permissions) {
        if (permissions.length == 0) {
            return null;
        }
        Map<String, Object> permissionEntries = new LinkedHashMap<>();
        for (Permission permission : permissions) {
            Map<String, Object> permissionEntry = processPermission(permission);
            if (!permissionEntry.isEmpty()) {
                permissionEntries.put(permission.node(), processPermission(permission));
            }
        }
        return permissionEntries;
    }

    private Map<String, Object> processPermission(Permission permission) {
        Map<String, Object> permissionEntry = new LinkedHashMap<>();

        if (!permission.description().isEmpty()) {
            permissionEntry.put("description", permission.description());
        }

        if (permission.defaultValue() != PermissionDefault.DEFAULT) {
            permissionEntry.put("default", permission.defaultValue().getName());
        }

        Map<String, Object> childPermissionEntries = new LinkedHashMap<>();
        for (ChildPermission childPermission : permission.children()) {
            childPermissionEntries.put(childPermission.node(), childPermission.inherit());
        }
        if (!childPermissionEntries.isEmpty()) {
            permissionEntry.put("children", childPermissionEntries);
        }

        return permissionEntry;
    }

    private Map<String, Object> processCommands(RoundEnvironment roundEnvironment) {
        Map<String, List<CommandData>> commands = new LinkedHashMap<>();
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Command.class)) {
            Command command = element.getAnnotation(Command.class);
            List<CommandData> commandData = commands.computeIfAbsent(command.command(), k -> new ArrayList<>());
            commandData.add(new CommandData(
                    command.command(),
                    command.description(),
                    command.aliases(),
                    command.permission(),
                    command.permissionMessage(),
                    getParamAnnotations((ExecutableElement) element),
                    getParamFlags((ExecutableElement) element)));
        }
        for (Element element : roundEnvironment.getElementsAnnotatedWith(Command.List.class)) {
            for (Command command : element.getAnnotation(Command.List.class).value()) {
                List<CommandData> commandData = commands.computeIfAbsent(command.command(), k -> new ArrayList<>());
                commandData.add(new CommandData(
                        command.command(),
                        command.description(),
                        command.aliases(),
                        command.permission(),
                        command.permissionMessage(),
                        getParamAnnotations((ExecutableElement) element),
                        getParamFlags((ExecutableElement) element)));
            }
        }
        commands.values().forEach(l -> l.forEach(c -> c.argumentParser = CommandUtils.getArgumentParser(
                c.name, c.description, c.annotations, null, c.flags)));

        Map<String, Object> commandsEntry = new LinkedHashMap<>();

        for (String s : commands.keySet()) {
            Map<String, Object> command = processCommand(commands.get(s));
            if (command != null && !command.isEmpty()) {
                commandsEntry.put(s, command);
            }
        }
        return commandsEntry.isEmpty() ? null : commandsEntry;
    }

    private Map<String, Object> processCommand(List<CommandData> commandData) {
        if (commandData.isEmpty()) {
            return null;
        }
        String descriptions = commandData.stream()
                .map(c -> c.description)
                .filter(d -> !d.isEmpty())
                .collect(Collectors.joining("\n"));
        List<String> aliases = Lists.newArrayList(
                commandData.stream().flatMap(c -> Arrays.stream(c.aliases)).collect(Collectors.toSet()));
        String permission = commandData.stream().allMatch(c -> c.permission.equals(commandData.get(0).permission)) ?
                commandData.get(0).permission : null;
        String permissionMessage = commandData.stream().allMatch(
                c -> c.permissionMessage.equals(commandData.get(0).permissionMessage)) ?
                commandData.get(0).permissionMessage : null;
        String usage = commandData.stream().map(c -> c.argumentParser.formatUsage()).collect(Collectors.joining("\n"));

        Map<String, Object> commandEntry = new LinkedHashMap<>();
        if (!descriptions.isEmpty()) {
            commandEntry.put("description", descriptions);
        }
        if (!aliases.isEmpty()) {
            commandEntry.put("aliases", aliases);
        }
        if (permission != null && !permission.isEmpty()) {
            commandEntry.put("permission", permission);
            if (permissionMessage != null) {
                commandEntry.put("permission-message", permissionMessage);
            }
        }
        commandEntry.put("usage", usage);
        return commandEntry;
    }

    private Annotation[][] getParamAnnotations(ExecutableElement element) {
        List<Annotation[]> paramAnnotations = new LinkedList<>();
        for (VariableElement childElement : element.getParameters()) {
            CommandOption commandOption = childElement.getAnnotation(CommandOption.class);
            CommandParameter commandParameter = childElement.getAnnotation(CommandParameter.class);
            Annotation[] annotations;
            if (commandOption != null && commandParameter != null) {
                annotations = new Annotation[]{commandOption, commandParameter};
            } else if (commandOption != null) {
                annotations = new Annotation[]{commandOption};
            } else if (commandParameter != null) {
                annotations = new Annotation[]{commandParameter};
            } else {
                annotations = new Annotation[0];
            }
            paramAnnotations.add(annotations);
        }
        return paramAnnotations.toArray(new Annotation[0][]);
    }

    private Boolean[] getParamFlags(ExecutableElement element) {
        List<Boolean> paramFlags = new LinkedList<>();
        for (VariableElement childElement : element.getParameters()) {
            TypeMirror declaredType = childElement.asType();
            PrimitiveType primitiveType;
            if (declaredType instanceof PrimitiveType) {
                primitiveType = (PrimitiveType) declaredType;
            } else {
                try {
                    primitiveType = processingEnv.getTypeUtils().unboxedType(declaredType);
                } catch (Exception e) {
                    primitiveType = null;
                }
            }
            paramFlags.add(primitiveType != null && primitiveType.getKind() == TypeKind.BOOLEAN);
        }
        return paramFlags.toArray(new Boolean[0]);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Plugin.class.getCanonicalName());
        annotations.add(Permission.class.getCanonicalName());
        annotations.add(ChildPermission.class.getCanonicalName());
        annotations.add(Command.class.getCanonicalName());
        annotations.add(Command.List.class.getCanonicalName());
        annotations.add(CommandOption.class.getCanonicalName());
        annotations.add(CommandParameter.class.getCanonicalName());
        return annotations;
    }

    private static class CommandData {
        final String name;
        final String description;
        final String[] aliases;
        final String permission;
        final String permissionMessage;
        final Annotation[][] annotations;
        final Boolean[] flags;
        ArgumentParser argumentParser;

        CommandData(String name, String description, String[] aliases, String permission, String permissionMessage,
                    Annotation[][] annotations, Boolean[] flags) {
            this.name = name;
            this.description = description;
            this.aliases = aliases;
            this.permission = permission;
            this.permissionMessage = permissionMessage;
            this.annotations = annotations;
            this.flags = flags;
        }
    }
}
