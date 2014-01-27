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
import java.util.logging.Level;

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
    public final String DISPLAY_NAME = "[VGScoreboardPing]";
    protected File configFile;
    protected YamlConfiguration config;

    protected int pingUpdaterTaskPeriod = 100;
    protected String craftBukkitClassName;
    protected String fallbackField;
    protected Boolean compatibilityMode = false;
    protected Boolean metricsOptOut = false;

    protected final String PERMISSION_CONFIG = "vg.scoreboardping.config";

    protected Scoreboard pingScoreboard;
    protected Objective pingObjective;
    protected Scoreboard masterPingScoreboard;
    protected Objective masterPingObjective;

    private ListenerJoin scoreboardListenerJoin;

    protected UpdaterTask scoreboardPingUpdater;
    protected BukkitTask scoreboardPingUpdaterTask;

    public void onEnable() {
	configFile = new File(getDataFolder().getPath().concat(File.separator).concat("config.yml"));
	saveDefaultConfig();
	config = YamlConfiguration.loadConfiguration(configFile);
	pingUpdaterTaskPeriod = config.getInt("period", 100);
	if (pingUpdaterTaskPeriod < 20) {
	    pingUpdaterTaskPeriod = 20;
	    config.set("period", pingUpdaterTaskPeriod);
	    getLogger().info("period set to 20 ticks. Values below 20 are a waste of resources because the server pings at a constant 20 ticks ratio.");
	}
	fallbackField = config.getString("fallback-field", "pingList");
	compatibilityMode = config.getBoolean("compatibility-mode", false);
	metricsOptOut = config.getBoolean("metrics-opt-out", false);

	detectCraftBukkitVersion();

	if (!metricsOptOut) {
	    try {
		MetricsLite metricsLite = new MetricsLite(this);
		metricsLite.start();
	    } catch (IOException e) {
		getLogger()
			.warning(
				"Metrics failed to send statistics to mcstats.org.\nAllowing metrics helps when deciding what plugins should be updated!\nIf there is a firewall preventing outbound connections, ensure traffic is allowed to mcstats.org port 80.");
	    }
	}

	registerScoreboard();
	registerListenerJoin();
	registerUpdaterTask();

	getCommand("sbp")
		.setUsage(
			"/sbp period <integer>\n  Change the update period for the scoreboard.\n/sbp pause\n  Stop updating and clear the scoreboard.\n/sbp resume\n  Resume updating the scoreboard.\n/sbp disable\n  Disable the plugin.");
	getCommand("sbp").setExecutor(new Commands(this));
    }

    public void onDisable() {
	unregisterUpdaterTask();
	unregisterScoreboard();
	unregisterListenerJoin();

	config = null;
	configFile = null;
    }

    protected void registerUpdaterTask() {
	scoreboardPingUpdater = new UpdaterTask(this);
	scoreboardPingUpdaterTask = scoreboardPingUpdater.runTaskTimer(this, 0, pingUpdaterTaskPeriod);
    }

    protected void unregisterUpdaterTask() {
	if (scoreboardPingUpdaterTask != null) {
	    getServer().getScheduler().cancelTask(scoreboardPingUpdaterTask.getTaskId());
	    scoreboardPingUpdaterTask = null;
	}
	if (scoreboardPingUpdater != null) {
	    scoreboardPingUpdater = null;
	}
    }

    protected void registerScoreboard() {
	pingScoreboard = getServer().getScoreboardManager().getNewScoreboard();
	pingObjective = pingScoreboard.registerNewObjective("PlayerPing", "dummy");
	pingObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
	masterPingScoreboard = pingScoreboard;
	masterPingObjective = pingObjective;
    }

    protected void unregisterScoreboard() {
	if (pingObjective != null) {
	    pingObjective.unregister();
	    pingObjective = null;
	}
	if (pingScoreboard != null) {
	    pingScoreboard.clearSlot(DisplaySlot.PLAYER_LIST);
	    pingScoreboard = null;
	}
	masterPingScoreboard = null;
	masterPingObjective = null;
    }

    protected void registerListenerJoin() {
	scoreboardListenerJoin = new ListenerJoin(this);
	getServer().getPluginManager().registerEvents(scoreboardListenerJoin, this);

	for (Player player : getServer().getOnlinePlayers()) {
	    player.setScoreboard(pingScoreboard);
	}
    }

    protected void unregisterListenerJoin() {
	if (scoreboardListenerJoin != null) {
	    PlayerJoinEvent.getHandlerList().unregister(scoreboardListenerJoin);
	    scoreboardListenerJoin = null;
	}
    }

    private void detectCraftBukkitVersion() {
	craftBukkitClassName = getServer().getClass().toString().replace("class ", "").replace(".CraftServer", "");

	try {
	    Class.forName(craftBukkitClassName.concat(".CraftServer")).getName();
	} catch (Exception e) {
	    getLogger().severe(
		    "Failed to auto-detected server class name!\nExpected [".concat(getServer().getClass().toString()).concat("]\nUsing ").concat(craftBukkitClassName)
			    .concat(".CraftServer"));
	    getLogger().info("If you wish this plugin to support this version of bukkit please open a ticket with the above line at ".concat(TRACKER_URL));
	    getServer().getPluginManager().disablePlugin(this);
	}
    }

    protected void reinit() {
	unregisterUpdaterTask();
	unregisterListenerJoin();
	unregisterScoreboard();
	registerScoreboard();
	registerListenerJoin();
	registerUpdaterTask();
    }

    protected void configSave() {
	try {
	    config.save(configFile);
	} catch (IOException e) {
	    getLogger().log(Level.SEVERE, "Error saving config!", e);
	}
    }
}
