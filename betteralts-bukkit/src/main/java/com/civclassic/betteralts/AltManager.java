package com.civclassic.betteralts;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.civclassic.betteralts.storage.Database;

import vg.civcraft.mc.civmodcore.itemHandling.ItemMap;
import vg.civcraft.mc.namelayer.NameAPI;

public class AltManager {

	private static Pattern nameRegex = Pattern.compile("\\w{3,16}");
	private static final char[] alphabet = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890"
			.toCharArray();

	private Database db;
	private Logger log;
	private SecureRandom random;

	public AltManager(BetterAlts plugin) {
		random = new SecureRandom();
		db = plugin.getConfigManager().getDataBase();
		log = plugin.getLogger();
	}

	public void requestAltCreation(Player p, String altName) {
		if (!nameSanityCheck(p, altName)) {
			return;
		}
		ItemMap requirements = BetterAlts.getInstance().getConfigManager().getBaseRequirements().clone();
		if (requirements == null) {
			p.sendMessage(ChatColor.RED
					+ "Cost of alts is not configured in the configuration file, alt could not be created");
			return;
		}
		double multiplier = getMultiplier(p.getUniqueId());
		requirements.multiplyContent(multiplier);
		if (!requirements.isContainedIn(p.getInventory())) {
			p.sendMessage(ChatColor.RED
					+ "You don't have the required materials for a new alt, run /altmats to see what you need.");
			return;
		}
		if (requirements.removeSafelyFrom(p.getInventory())) {
			log.info("Creating alt " + altName + " for player " + p.getName() + " with uuid " + p.getUniqueId());
			String loginCode = generateSecureLoginCode(p.getUniqueId());
			if (loginCode == null) {
				p.sendMessage(ChatColor.RED
						+ "Failed to create an alt because no valid login code could be generated. You should complain about this to an admin");
			}
			NameAPI.getAssociationList().addPlayer(altName, db.addAlt(p.getUniqueId(), altName, loginCode));
			p.sendMessage(ChatColor.GREEN + "Your alt '" + altName + "' was created successfully!");
			p.sendMessage(ChatColor.GREEN + "Log in to the server with the ip: " + loginCode + "."
					+ BetterAlts.getInstance().getConfigManager().getBaseIP() + " to use it");
		} else {
			p.sendMessage(ChatColor.RED
					+ "Failed to remove required items from your inventory, something weird happened. Dont do this again");
			log.warning("Failed to remove items for alt from the inentory of " + p.getName()
					+ ". Investigation recommended");
		}
	}

	private String generateSecureLoginCode(UUID player) {
		StringBuilder code = new StringBuilder();
		int reattemptCount = 0;
		do {
			if (reattemptCount++ >= 20) {
				return null;
			}
			for (int i = 0; i < 8; i++) {
				code.append(alphabet[random.nextInt(alphabet.length)]);
			}
		} while (db.keyExists(code.toString()));
		return code.toString();
	}

	public double getMultiplier(UUID player) {
		int altCount = db.getAltCount(player);
		return Math.pow(altCount, 1.5);
	}

	private boolean nameSanityCheck(Player p, String name) {
		if (name == null) {
			return false;
		}
		Matcher match = nameRegex.matcher(name);
		if (!match.matches()) {
			if (p != null) {
				p.sendMessage(ChatColor.RED
						+ "The name you provided was not valid. The name must be at least 3 characters, "
						+ "at most 16 characters and may only contain alpha-numeric characters and underscores");
			}
			return false;
		}
		// TODO what else?
		return true;
	}

}
