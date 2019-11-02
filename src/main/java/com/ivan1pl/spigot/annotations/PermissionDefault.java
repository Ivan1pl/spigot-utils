package com.ivan1pl.spigot.annotations;

/**
 * Permission default value.
 */
public enum PermissionDefault {
    /**
     * Server default.
     */
    DEFAULT(""),

    /**
     * Assigned to all.
     */
    TRUE("true"),

    /**
     * Assigned to none.
     */
    FALSE("false"),

    /**
     * Assigned to server operators.
     */
    OP("op"),

    /**
     * Assigned to everyone except server operators.
     */
    NOT_OP("not op"),
    ;

    private String name;

    PermissionDefault(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
