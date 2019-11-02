package com.ivan1pl.spigot.base;

import com.ivan1pl.spigot.utils.CommandUtils;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Base plugin class, extend it to create your plugin.
 */
public class BasePlugin extends JavaPlugin {
    /**
     * Executed when the plugin is enabled. Override to add additional behaviour (remember to invoke the method from the
     * superclass if you do, otherwise annotations will not be processed).
     */
    @Override
    public void onEnable() {
        super.onEnable();
        CommandUtils.initCommands(this);
    }
}
