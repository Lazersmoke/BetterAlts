package com.civclassic.betteralts.commands;

import com.civclassic.betteralts.BetterAlts;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;

public class CreateAltCommand extends PlayerCommand {

	public CreateAltCommand() {
		super("createalt");
		setArguments(1, 1);
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Go away console man");
			return true;
		}
		BetterAlts.getInstance().getAltManager().requestAltCreation((Player) sender, args[0]);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender arg0, String[] arg1) {
		return null;
	}

}
