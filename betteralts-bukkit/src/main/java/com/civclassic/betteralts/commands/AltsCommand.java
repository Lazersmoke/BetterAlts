package com.civclassic.betteralts.commands;

import java.util.List;
import java.util.UUID;

import org.bukkit.command.CommandSender;

import com.civclassic.betteralts.BetterAlts;
import com.civclassic.betteralts.storage.AltAccount;

import net.md_5.bungee.api.ChatColor;
import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.namelayer.NameAPI;

public class AltsCommand extends PlayerCommand {

	public AltsCommand() {
		super("alts");
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		UUID player;
		try {
			player = UUID.fromString(args[0]);
		} catch (IllegalArgumentException e) {
			player = NameAPI.getUUID(args[0]);
		}
		if(player == null) {
			sender.sendMessage(ChatColor.RED + "Player not found!");
			return true;
		}
		String name = NameAPI.getCurrentName(player);
		List<AltAccount> alts = BetterAlts.getInstance().getConfigManager().getDataBase().getAlts(player);
		if(alts.size() == 1) {
			sender.sendMessage(ChatColor.YELLOW + name + " has no alts");
		}
		StringBuilder builder = new StringBuilder("Alts for " + name + ": ");
		for(AltAccount alt : alts) {
			builder.append(alt.getName() + ", ");
		}
		String msg = builder.toString();
		sender.sendMessage(ChatColor.YELLOW + msg.substring(0, msg.length() - 2));
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender sender, String[] args) {
		return BetterAlts.getInstance().getConfigManager().getDataBase().getAllAccounts();
	}

}
