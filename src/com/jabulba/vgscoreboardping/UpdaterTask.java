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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Scoreboard;

public class UpdaterTask extends BukkitRunnable {
    private final ScoreboardPing plugin;
    private Field craftBukkitEntityField;
    private String pingFieldName = "ping";

    public UpdaterTask(ScoreboardPing plugin) {
	this.plugin = plugin;

	try {
	    craftBukkitEntityField = Class.forName(plugin.CRAFT_BUKKIT_CLASS_NAME.concat(".entity.CraftEntity")).getDeclaredField("entity");
	    craftBukkitEntityField.setAccessible(true);

	    // test access to the field. this is here only to cause a SecurityException if something went wrong.
	    craftBukkitEntityField.get(plugin.getServer().getOfflinePlayer("Jabulba"));

	} catch (ClassNotFoundException | SecurityException | NoSuchFieldException | IllegalAccessException e) {
	    plugin.getLogger().log(Level.SEVERE,
		    "Something extremely bad happened trying to gain access to CraftEntity fields! Please do report this problem at ".concat(plugin.TRACKER_URL), e);
	    plugin.getServer().getPluginManager().disablePlugin(plugin);
	    ;
	} catch (IllegalArgumentException e) {
	    // The field test was successful! The exception occurs because an OfflinePlayer was used.
	}
    }

    @Override
    public void run() {
	if (plugin.getServer().getOnlinePlayers().length == 0) {
	    return;
	}

	if (plugin.compatibilityMode) {
	    setPlayerScoreCompatibliblity();
	} else {
	    setPlayerScore();
	}
    }

    private void setPlayerScore() {
	for (Player player : plugin.getServer().getOnlinePlayers()) {
	    plugin.pingObjective.getScore(plugin.getServer().getOfflinePlayer(player.getPlayerListName())).setScore(getPing(player));
	}
    }

    private void setPlayerScoreCompatibliblity() {
	for (Player player : plugin.getServer().getOnlinePlayers()) {
	    Scoreboard playerScoreboard = player.getScoreboard();
	    if (playerScoreboard != null && playerScoreboard != plugin.pingScoreboard) {
		plugin.pingScoreboard = player.getScoreboard();
		if (plugin.pingScoreboard.getObjective("PlayerPing") == null) {
		    plugin.pingObjective = plugin.pingScoreboard.registerNewObjective("PlayerPing", "dummy");
		    plugin.pingObjective.setDisplaySlot(DisplaySlot.PLAYER_LIST);
		}
	    }
	    plugin.pingObjective.getScore(plugin.getServer().getOfflinePlayer(player.getPlayerListName())).setScore(getPing(player));
	}
    }

    private int getPing(Player player) {
	try {
	    Class<?> entityCraftPlayer = Class.forName(plugin.CRAFT_BUKKIT_CLASS_NAME.concat(".entity.CraftPlayer"));
	    Method getHandle = entityCraftPlayer.getMethod("getHandle", new Class[0]);
	    Object playerHandle = getHandle.invoke(player);
	    Object craftPlayer = craftBukkitEntityField.get(player);
	    return playerHandle.getClass().getField(pingFieldName).getInt(craftPlayer);

	} catch (NoSuchFieldException e) {
	    if (pingFieldName == "ping") {
		pingFieldName = "field_71138_i";
		plugin.getLogger().info("Failed to get ping from field \"ping\", field does not exist! Attempting \"field_71138_i\" as ping field name.");

	    } else if (pingFieldName == "field_71138_i") {
		pingFieldName = "lastPing";
		plugin.getLogger()
			.warning(
				"Failed to find \"field_71138_i\" field! Attempting fallback to \"lastPing\" field name. This field has a drawback and it takes long to update but should always exist.");

	    } else if (pingFieldName == plugin.FALLBACK_FIELD) {
		plugin.getLogger().info("Parsing player: ".concat(player.getName()));
		plugin.getLogger().log(Level.SEVERE, "Unable to determine ping field. Can't continue!", e);
		plugin.getServer().getPluginManager().disablePlugin(plugin);
		;

	    }
	    return 0;

	} catch (Exception e) {
	    plugin.getLogger().info("Parsing player: ".concat(player.getName()));
	    plugin.getLogger()
		    .log(Level.SEVERE,
			    "VG Scoreboard Ping was unable to find the NMS player class. In rought words the bukkit version you use is not supported. Please report this on the tracker so the plugin can be updated!",
			    e);
	    plugin.getServer().getPluginManager().disablePlugin(plugin);
	    ;
	    return 0;
	}
    }
}
