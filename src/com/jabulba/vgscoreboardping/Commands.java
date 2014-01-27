package com.jabulba.vgscoreboardping;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class Commands implements CommandExecutor {

    private ScoreboardPing plugin;

    public Commands(ScoreboardPing plugin) {
	this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
	if (args.length < 1) {
	    sender.sendMessage("Not enough arguments!");
	    return false;
	}

	if (!sender.hasPermission("vg.scoreboardping.admin")) {
	    sender.sendMessage("Insuficient permissions.");
	    return true;
	}

	if (args[0].equalsIgnoreCase("period")) {
	    if (args.length != 2) {
		sender.sendMessage("Wrong number of arguments.");
		return false;
	    }

	    Integer period;
	    try {
		period = Integer.parseInt(args[1]);
	    } catch (NumberFormatException e) {
		sender.sendMessage(plugin.DISPLAY_NAME.concat(" ").concat(args[1]).concat(" is not a valid number."));
		return true;
	    }
	    if (period < 20) {
		sender.sendMessage(plugin.DISPLAY_NAME.concat(" Period below 20 has no effect real effect, bukkit pings players only once a second."));
	    }
	    plugin.pingUpdaterTaskPeriod = period;
	    plugin.config.set("period", plugin.pingUpdaterTaskPeriod);
	    plugin.configSave();
	    sender.sendMessage(plugin.DISPLAY_NAME.concat(" The period has been set to ").concat(String.valueOf(period)));

	    if (plugin.scoreboardPingUpdaterTask == null) {
		sender.sendMessage(plugin.DISPLAY_NAME.concat(" The plugin is paused and your changes will take effect after resuming it"));
		return true;
	    }
	    plugin.unregisterUpdaterTask();
	    plugin.registerUpdaterTask();
	    return true;

	} else if (args[0].equalsIgnoreCase("pause")) {
	    if (plugin.scoreboardPingUpdaterTask != null) {
		plugin.unregisterUpdaterTask();
		plugin.unregisterScoreboard();
		plugin.unregisterListenerJoin();
		sender.sendMessage(plugin.DISPLAY_NAME.concat(" Now paused."));
		return true;
	    }
	    sender.sendMessage(plugin.DISPLAY_NAME.concat(" Not running!"));
	    return true;

	} else if (args[0].equalsIgnoreCase("resume")) {
	    if (plugin.scoreboardPingUpdaterTask == null) {
		plugin.registerScoreboard();
		plugin.registerListenerJoin();
		plugin.registerUpdaterTask();
		sender.sendMessage(plugin.DISPLAY_NAME.concat(" Now resumed."));
		return true;
	    }
	    sender.sendMessage(plugin.DISPLAY_NAME.concat(" Not paused!"));
	    return true;

	} else if (args[0].equalsIgnoreCase("compmode")) {
	    if (plugin.compatibilityMode) {
		sender.sendMessage(plugin.DISPLAY_NAME.concat(" Restarting without compatibility mode."));
		plugin.compatibilityMode = false;
		plugin.reinit();
	    } else {
		sender.sendMessage(plugin.DISPLAY_NAME.concat(" Restarting with compatibility mode."));
		plugin.compatibilityMode = true;
		plugin.reinit();
	    }
	    plugin.config.set("compatibility-mode", plugin.compatibilityMode);
	    plugin.configSave();
	    return true;

	} else if (args[0].equalsIgnoreCase("disable")) {
	    sender.sendMessage(plugin.DISPLAY_NAME.concat(" Disabled."));
	    plugin.getServer().getPluginManager().disablePlugin(plugin);
	    return true;
	}
	return false;
    }
}
