package com.artillexstudios.axmines.config.impl;

import com.artillexstudios.axmines.AxMinesPlugin;
import com.artillexstudios.axmines.utils.FileUtils;

import java.util.List;
import java.util.Map;

public class MineConfig extends Messages {

    @Key("display-name")
    public String DISPLAY_NAME = "<color:#FF0000>Example";

    @Key("contents")
    public Map<Object, Object> CONTENTS = Map.of("gold_block", 11, "diamond_block", 10, "emerald_block", 25, "iron_block", 10);

    @Key("selection.1")
    public String SELECTION_CORNER_1 = "world;10;10;10;10;10";

    @Key("selection.2")
    public String SELECTION_CORNER_2 = "world;10;10;10;10;10";

    @Key("teleport-location")
    public String TELEPORT_LOCATION = "world;10;10;10;10;10";

    @Key("teleport-on-reset")
    public int TELEPORT_ON_RESET = 0;

    @Key("broadcast-reset")
    public int BROADCAST_RESET = -1;

    @Key("reset.ticks")
    public long RESET_TICKS = 12000;

    @Key("reset.percent")
    public double RESET_PERCENT = 10.0;

    @Key("random-rewards")
    public List<Map<String, Object>> RANDOM_REWARDS = List.of(Map.of("chance", 0.001, "blocks", List.of("diamond_block", "emerald_block"), "commands", List.of("eco give <player> 100000")));

    @Key("reset-commands")
    public List<String> RESET_COMMANDS = List.of("say Mine A has been reset!");

    @Key("actionbar.enabled")
    public boolean ACTION_BAR_ENABLED = false;

    @Key("actionbar.range")
    public int ACTION_BAR_RANGE = 10;

    @Key("timer-format")
    @Comment({"The format of the time placeholder", "1 -> HH:MM:SS, for example 01:25:35", "2 -> short format, for example 20m", "3 - text format, for example 01h 25m 35s"})
    public int TIMER_FORMAT = 2;

    @Key("setter")
    @Comment({"What blocksetter should we use to set the blocks?", "Options:", "parallel - Best performance, does not lag the server thread. Sometimes can be a little bit inaccurate - CURRENTLY NOT AVAILABLE", "fast - Sets the blocks on the main thread using faster methods", "bukkit - Uses the Bukkit API to set the blocks. This is the slowest out of all the setters", "", "If you are having issues with blocks not being set correctly, we suggest using bukkit."})
    public String SETTER = "fast";

    public MineConfig(String fileName) {
        super(fileName);
    }

    @Override
    public void reload() {
        this.reload(FileUtils.PLUGIN_DIRECTORY.resolve(fileName), Messages.class, this, AxMinesPlugin.MESSAGES);
        this.reload(FileUtils.PLUGIN_DIRECTORY.resolve(fileName), MineConfig.class, this, null);
    }
}