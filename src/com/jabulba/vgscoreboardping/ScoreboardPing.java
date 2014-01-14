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

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

/**
 * @author Caio
 * 
 */
public class ScoreboardPing extends JavaPlugin {
    private File configFile;
    private YamlConfiguration config;
    private int PING_UPDATER_TASK_PERIOD = 100;

    protected static Scoreboard pingScoreboard;
    protected static Objective pingObjective;

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

	scoreboardListenerJoin = new ScoreboardPingListenerJoin();
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
}
