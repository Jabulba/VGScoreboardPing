/**
 * VGScoreboardPing
 * Copyright (C) 2013 Caio Cogliatti Jabulka (Jabulba) <http://www.jabulba.com>
 * 
 * This file is part of VGScoreboardPing.
 * VGScoreboardPing was originally a module of VG Server Manager(All Right Reserved until public release)
 * 
 * VGScoreboardPing is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * VGScoreboardPing is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with VGScoreboardPing.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jabulba.vgscoreboardping;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

public class ScoreboardPing extends JavaPlugin {
    public final String TRACKER_URL = "https://github.com/Jabulba/VGScoreboardPing/issues";

    private File configFile;
    private YamlConfiguration config;
    private int PING_UPDATER_TASK_PERIOD = 100;
    protected String CRAFT_BUKKIT_CLASS_NAME;

    protected Scoreboard pingScoreboard;
    protected Objective pingObjective;

    private ScoreboardPingListenerJoin scoreboardListenerJoin;

    private ScoreboardPingUpdaterTask scoreboardPingUpdater;
    private BukkitTask scoreboardPingUpdaterTask;

    public void onEnable() {
	configFile = new File(getDataFolder().getPath().concat(File.separator).concat("config.yml"));
	saveDefaultConfig();
	config = YamlConfiguration.loadConfiguration(configFile);
	PING_UPDATER_TASK_PERIOD = config.getInt("period", 100);
	if (PING_UPDATER_TASK_PERIOD < 20) {
	    PING_UPDATER_TASK_PERIOD = 20;
	    getLogger().info("period set to 20 ticks. Values below 20 are a waste of resources because the server pings at a constant 20 ticks ratio.");
	}

	if (!detectCraftBukkitVersion()) {
	    getLogger().info("Found: " + CRAFT_BUKKIT_CLASS_NAME);
	    getLogger().severe("Unable to detect org.bukkit.craftbukkit.v#_#_(R#|#) from ".concat(Bukkit.getServer().getClass().toString()));
	    getLogger().info("If you wish this plugin to support this version of bukkit please open a ticket with the above line at ".concat(TRACKER_URL));
	    disable();
	}

	try {
	    getLogger().info("CraftServer found at: ".concat(Class.forName(CRAFT_BUKKIT_CLASS_NAME.concat(".CraftServer")).getName()));
	} catch (Exception e) {
	    getLogger().severe("Failed to auto-detected server class name! [".concat(getServer().getClass().toString()).concat("]"));
	    getLogger().info("If you wish this plugin to support this version of bukkit please open a ticket with the above line at ".concat(TRACKER_URL));
	    disable();
	    return;
	}

	try {
	    MetricsLite metricsLite = new MetricsLite(this);
	    metricsLite.start();
	} catch (IOException e) {
	    getLogger()
		    .warning(
			    "Metrics failed to send statistics to mcstats.org.\nAllowing metrics helps when deciding what plugins should be updated!\nIf there is a firewall preventing outbound connections, ensure traffic is allowed to mcstats.org port 80.");
	}

	pingScoreboard = Bukkit.getServer().getScoreboardManager().getNewScoreboard();
	pingObjective = pingScoreboard.registerNewObjective("PlayerPing", "dummy");
	pingObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);

	scoreboardListenerJoin = new ScoreboardPingListenerJoin(this);
	getServer().getPluginManager().registerEvents(scoreboardListenerJoin, this);

	for (Player player : getServer().getOnlinePlayers()) {
	    player.setScoreboard(pingScoreboard);
	}

	scoreboardPingUpdater = new ScoreboardPingUpdaterTask(this);
	scoreboardPingUpdaterTask = scoreboardPingUpdater.runTaskTimer(this, 0, PING_UPDATER_TASK_PERIOD);

	getLogger().info("Enabled.");
    }

    public void disable() {
	Bukkit.getScheduler().cancelTask(scoreboardPingUpdaterTask.getTaskId());
	scoreboardPingUpdaterTask = null;

	PlayerJoinEvent.getHandlerList().unregister(scoreboardListenerJoin);
	scoreboardListenerJoin = null;

	pingObjective.unregister();
	pingScoreboard.clearSlot(DisplaySlot.PLAYER_LIST);
	pingObjective = null;
	pingScoreboard = null;
	config = null;
	configFile = null;

	getLogger().info("Disabled.");
    }

    private Boolean detectCraftBukkitVersion() {
	Pattern pattern = Pattern.compile("v\\d_\\d_(R\\d|\\d)");
	Matcher matcher = pattern.matcher(Bukkit.getServer().getClass().toString());
	if (!matcher.find()) {
	    pattern = Pattern.compile("org.bukkit.craftbukkit");
	    matcher = pattern.matcher(Bukkit.getServer().getClass().toString());
	    if (!matcher.find()) {
		CRAFT_BUKKIT_CLASS_NAME = "nothing found :(";
		return false;
	    }
	    CRAFT_BUKKIT_CLASS_NAME = matcher.group();
	    return true;
	}
	String version = matcher.group();
	CRAFT_BUKKIT_CLASS_NAME = "org.bukkit.craftbukkit.".concat(version);
	return true;
    }
}
