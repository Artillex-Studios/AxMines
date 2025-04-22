package com.artillexstudios.axmines.config.impl;

import com.artillexstudios.axmines.config.AbstractConfig;
import com.artillexstudios.axmines.utils.FileUtils;

public class Config extends AbstractConfig {

    @Key("update-checker.enabled")
    @Comment({
            "Enable this, if you want the plugin to check for updates"
    })
    public static boolean UPDATE_CHECKER_ENABLED = true;

    @Key("update-checker.message-on-join")
    @Comment({
            "Enable this, if you want the plugin to check for updates",
            "and send messages to users with the axmines.updatecheck.onjoin permission.",
            "This only works if the update checker is enabled!"
    })
    public static boolean UPDATE_CHECKER_MESSAGE_ON_JOIN = true;

    @Key("debug")
    @Comment({"Whether to send debug messages to the console", "debug: false"})
    public static boolean DEBUG = false;

    private static final Config CONFIG = new Config();

    public static void reload() {
        CONFIG.reload(FileUtils.PLUGIN_DIRECTORY.resolve("config.yml"), Config.class, null, null);
    }
}
