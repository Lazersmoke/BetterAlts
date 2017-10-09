package com.civclassic.betteralts.commands;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.civclassic.betteralts.BetterAlts;

import vg.civcraft.mc.civmodcore.command.PlayerCommand;
import vg.civcraft.mc.civmodcore.inventorygui.ClickableInventory;
import vg.civcraft.mc.civmodcore.inventorygui.DecorationStack;
import vg.civcraft.mc.civmodcore.itemHandling.ItemMap;

public class AltMaterialsCommand extends PlayerCommand {

	public AltMaterialsCommand() {
		super("altmats");
	}

	@Override
	public boolean execute(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "Go away console man");
			return true;
		}
		Player player = (Player) sender;
		ItemMap requirements = BetterAlts.getInstance().getConfigManager().getBaseRequirements().clone();
		if (requirements == null) {
			player.sendMessage(ChatColor.RED
					+ "Cost of alts is not configured in the configuration file, so there's nothing to show");
			return true;
		}
		double multiplier = BetterAlts.getInstance().getAltManager().getMultiplier(player.getUniqueId());
		requirements.multiplyContent(multiplier);
		List<ItemStack> requirementsList = requirements.getItemStackRepresentation();
		ClickableInventory requirementsGui = new ClickableInventory(requirementsList.size(), "New Alt Requirements");
		for(ItemStack item : requirementsList) {
			requirementsGui.addSlot(new DecorationStack(item));
		}
		requirementsGui.showInventory(player);
		return true;
	}

	@Override
	public List<String> tabComplete(CommandSender arg0, String[] arg1) {
		return null;
	}

}