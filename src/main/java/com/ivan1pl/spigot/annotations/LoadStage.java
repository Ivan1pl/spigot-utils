package com.ivan1pl.spigot.annotations;

/**
 * Enum representing server loading stage.
 */
public enum LoadStage {
    /**
     * Server startup.
     */
    STARTUP,

    /**
     * After worlds are loaded.
     */
    POSTWORLD,
    ;
}
