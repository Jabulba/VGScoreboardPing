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

import org.bukkit.craftbukkit.v1_7_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ScoreboardPingUpdaterTask extends BukkitRunnable {
    private final ScoreboardPing plugin;

    public ScoreboardPingUpdaterTask(ScoreboardPing plugin) {
	this.plugin = plugin;
    }

    @Override
    public void run() {
	if (plugin.getServer().getOnlinePlayers().length == 0) {
	    return;
	}

	for (Player player : plugin.getServer().getOnlinePlayers()) {
	    ScoreboardPing.pingObjective.getScore(plugin.getServer().getOfflinePlayer(player.getPlayerListName())).setScore(((CraftPlayer) player).getHandle().ping);
	}
    }
}
