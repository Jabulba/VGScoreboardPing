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

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ScoreboardPingUpdaterTask extends BukkitRunnable {
    private final ScoreboardPing plugin;
    private Field craftBukkitEntityField;
    private String pingFieldName = "ping";

    public ScoreboardPingUpdaterTask(ScoreboardPing plugin) {
	this.plugin = plugin;

	try {
	    craftBukkitEntityField = Class.forName(plugin.CRAFT_BUKKIT_CLASS_NAME.concat(".entity.CraftEntity")).getDeclaredField("entity");
	    craftBukkitEntityField.setAccessible(true);
	    OfflinePlayer offlinePlayer = plugin.getServer().getOfflinePlayer("Jabulba");
	    @SuppressWarnings("unused")
	    Object testAccess = craftBukkitEntityField.get(offlinePlayer);
	} catch (ClassNotFoundException | SecurityException | NoSuchFieldException | IllegalAccessException e) {
	    plugin.getLogger().log(Level.SEVERE,
		    "Something extremely bad happened trying to gain access to CraftEntity fields! Please do report this problem at ".concat(plugin.TRACKER_URL), e);
	} catch (IllegalArgumentException e) {

	}
    }

    @Override
    public void run() {
	if (plugin.getServer().getOnlinePlayers().length == 0) {
	    return;
	}

	for (Player player : plugin.getServer().getOnlinePlayers()) {
	    int ping;
	    try {
		Object craftPlayer = craftBukkitEntityField.get(player);
		Class<?> entityCraftPlayer = Class.forName(plugin.CRAFT_BUKKIT_CLASS_NAME.concat(".entity.CraftPlayer"));
		Method getHandle = entityCraftPlayer.getMethod("getHandle", new Class[0]);
		Object playerHandle = getHandle.invoke(player);

		ping = playerHandle.getClass().getField(pingFieldName).getInt(craftPlayer);
	    } catch (NoSuchFieldException e) {
		if (pingFieldName == "lastPing") {
		    plugin.getLogger().info("Parsing player: ".concat(player.getName()));
		    plugin.getLogger().log(Level.SEVERE, "Unable to determine ping field. Can't continue!", e);
		    plugin.disable();
		    return;
		}
		if (pingFieldName == "field_71138_i") {
		    pingFieldName = "lastPing";
		    plugin.getLogger().info("Failed to find \"field_71138_i\" field! Attempting fallback to \"lastPing\" field name. This field has a drawback and it takes long to update but should always exist.");
		} else {
		    pingFieldName = "field_71138_i";
		    plugin.getLogger().info("Failed to get ping from field \"ping\", field does not exist! Attempting \"field_71138_i\" as ping field name.");
		}
		ping = 0;
	    } catch (Exception e) {
		plugin.getLogger().info("Parsing player: ".concat(player.getName()));
		plugin.getLogger()
			.log(Level.SEVERE,
				"VG Scoreboard Ping was unable to find the NMS player class. In rought words the bukkit version you use is not supported. Please report this on the tracker so the plugin can be updated!",
				e);
		plugin.disable();
		return;
	    }

	    plugin.pingObjective.getScore(plugin.getServer().getOfflinePlayer(player.getPlayerListName())).setScore(ping);
	}
    }
}
