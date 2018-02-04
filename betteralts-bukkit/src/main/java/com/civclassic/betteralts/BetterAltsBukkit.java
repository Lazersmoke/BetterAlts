package com.civclassic.betteralts;

import java.security.SecureRandom;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import vg.civcraft.mc.civmodcore.inventorygui.DecorationStack;
import vg.civcraft.mc.civmodcore.inventorygui.IClickable;
import vg.civcraft.mc.civmodcore.inventorygui.MultiPageView;
import vg.civcraft.mc.civmodcore.itemHandling.ItemMap;
import vg.civcraft.mc.civmodcore.util.ConfigParsing;
import vg.civcraft.mc.namelayer.NameAPI;

public class BetterAltsBukkit extends JavaPlugin {

	private static BetterAltsBukkit instance;
	
	private AltApi api;
	private Logger log;
	private double altScaleFactor;
	private double onlineScaleFactor;
	private ItemMap altRequirements;
	private ItemMap onlineRequirements;
	private SecureRandom rng = new SecureRandom();
	
	public void onEnable() {
		instance = this;
		saveDefaultConfig();
		FileConfiguration config = getConfig();
		log = getLogger();
		
		ConfigurationSection dbConfig = config.getConfigurationSection("database");
		if(dbConfig == null) {
			log.severe("No database credentials, shut her down!");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		String user = dbConfig.getString("user");
		String pass = dbConfig.getString("password");
		String host = dbConfig.getString("host");
		int port = dbConfig.getInt("port");
		String database = dbConfig.getString("db");
		int poolSize = dbConfig.getInt("poolSize");
		long connectionTimeout = dbConfig.getLong("connectionTimeout");
		long idleTimeout = dbConfig.getLong("idleTimeout");
		long maxLife = dbConfig.getLong("maxLifetime");
		api = new AltManager(log, user, pass, host, port, database, poolSize, connectionTimeout, idleTimeout, maxLife);
		if (!api.initialized()) {
			log.log(Level.SEVERE, "Error loading db, check the credentials?");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		altScaleFactor = config.getDouble("altScaleFactor");
		onlineScaleFactor = config.getDouble("onlineScaleFactor");
		
		altRequirements = ConfigParsing.parseItemMap(config.getConfigurationSection("altCost"));
		onlineRequirements = ConfigParsing.parseItemMap(config.getConfigurationSection("onlineCost"));
		
		getCommand("alts").setExecutor(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player)) {
			
		} else {
			if(args.length == 0) {
				sender.sendMessage(ChatColor.RED + "Please specify /alts <create|upgrade|createmats|upgrademats>");
			} else {
				switch(args[0]) {
				case "create":
					altsCreateCommand((Player)sender, args);
					break;
				case "upgrade":
					altsUpgradeCommand((Player)sender);
					break;
				case "createmats":
					createMatsCommand((Player)sender);
					break;
				case "upgrademats":
					upgradeMatsCommand((Player)sender);
					break;
				case "check":
					altsCheckCommand(sender, args);
					break;
				}
			}
		}
		return true;
	}
	
	private void altsCheckCommand(CommandSender sender, String[] args) {
		if(!sender.hasPermission("betteralts.admin")) {
			sender.sendMessage(ChatColor.RED + "You do not have permission to run that command!");
		} else {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Please specify a player to check.");
			} else {
				UUID id;
				try {
					id = UUID.fromString(args[1]);
				} catch (IllegalArgumentException e) {
					id = NameAPI.getUUID(args[1]);
				}
				if(id == null) {
					sender.sendMessage(ChatColor.RED + "Player not found");
				} else {
					String name = NameAPI.getCurrentName(id);
					Set<UUID> alts = api.getAlts(id);
					Player player = Bukkit.getPlayer(id);
					if(alts.size() == 0) {
						ChatColor color = player.isOnline() ?  ChatColor.GREEN : ChatColor.WHITE;
						sender.sendMessage(color + name + ChatColor.RESET + " has no alts");
					} else {
						StringBuilder msgBuilder = new StringBuilder(ChatColor.GOLD + "Alts for " + name);
						for(UUID user : alts) {
							player = Bukkit.getPlayer(user);
							ChatColor color = player.isOnline() ? ChatColor.GREEN : ChatColor.WHITE;
							msgBuilder.append(color).append(NameAPI.getCurrentName(user)).append(", ");
						}
						String msg = msgBuilder.toString();
						sender.sendMessage(msg.substring(0, msg.length() - 2));
					}
				}
			}
		}
	}

	private void createMatsCommand(Player player) {
		ItemMap requirements = getAltRequirements(player);
		List<IClickable> items = new LinkedList<IClickable>();
		for(ItemStack stack : requirements.getItemStackRepresentation()) {
			DecorationStack item = new DecorationStack(stack);
			items.add(item);
		}
		MultiPageView view = new MultiPageView(player, items, "Alt Creation Requirements", true);
		view.showScreen();
	}
	
	private void upgradeMatsCommand(Player player) {
		ItemMap requirements = getOnlineRequirements(player);
		List<IClickable> items = new LinkedList<IClickable>();
		for(ItemStack stack : requirements.getItemStackRepresentation()) {
			DecorationStack item = new DecorationStack(stack);
			items.add(item);
		}
		MultiPageView view = new MultiPageView(player, items, "Alt Upgrade Requirements", true);
		view.showScreen();
	}

	private void altsCreateCommand(Player player, String[] args) {
		ItemMap requirements = getAltRequirements(player);
		if(requirements == null) {
			player.sendMessage(ChatColor.DARK_RED + "An internal error occurred, the creation of alts is temporarily disabled.");
		} else {
			if(requirements.isContainedIn(player.getInventory())) {
				if(args.length == 1) {
					player.sendMessage(ChatColor.RED + "Please specify a name for your new alt.");
				} else {
					String name = args[1];
					if(!api.canPlayerTakeName(player.getUniqueId(), name)) {
						player.sendMessage(ChatColor.RED + "You're not allowed to use that name!");
					} else {
						if(requirements.removeSafelyFrom(player.getInventory())) {
							String loginCode = generateLoginCode();
							NameAPI.getAssociationList().addPlayer(name, api.addAlt(player.getUniqueId(), name, loginCode));
							player.sendMessage(ChatColor.GREEN + "Your alt '" + name + "' was created successfully!");
							player.sendMessage(ChatColor.GREEN + "Log in to the server with the ip: " + loginCode + ".mc.civclassic.com");
						} else {
							player.sendMessage(ChatColor.RED + "Failed to remove required items from your inventory.");
						}
					}
				}
			} else {
				player.sendMessage(ChatColor.RED + "You don't have the required materials, do /alts createmats to see what you need.");
			}
		}
	}
	
	private void altsUpgradeCommand(Player player) {
		ItemMap requirements = getOnlineRequirements(player);
		if(requirements == null) {
			player.sendMessage(ChatColor.DARK_RED + "An internal error occurred, upgrading your alts is temporarily disabled.");
		} else {
			if(requirements.isContainedIn(player.getInventory())) {
				if(requirements.removeSafelyFrom(player.getInventory())) {
					api.increaseMaxOnline(player.getUniqueId());
					int max = api.getMaxOnlineCount(player.getUniqueId());
					player.sendMessage(ChatColor.GREEN + "You can now have " + max + " accounts online!");
				} else {
					player.sendMessage(ChatColor.RED + "Failed to remove required items from your inventory.");
				}
			} else {
				player.sendMessage(ChatColor.RED + "You don't have the required materials, do /alts upgrademats to see what you need.");
			}
		}
	}

	private ItemMap getAltRequirements(Player player) {
		int count = api.getAltCount(player.getUniqueId());
		double multiplier = Math.pow(count, altScaleFactor);
		ItemMap requirements = altRequirements.clone();
		requirements.multiplyContent(multiplier);
		return requirements;
	}
	
	private ItemMap getOnlineRequirements(Player player) {
		int count = api.getMaxOnlineCount(player.getUniqueId());
		double multiplier = Math.pow(count, onlineScaleFactor);
		ItemMap requirements = onlineRequirements.clone();
		requirements.multiplyContent(multiplier);
		return requirements;
	}
	
	private static final char[] alphabet = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890".toCharArray();
	private String generateLoginCode() {
		String code = "";
		do {
			code = "";
			for(int i = 0; i < 8; i++) {
				code += alphabet[rng.nextInt(alphabet.length)];
			}
		} while (api.keyExists(code));
		return code;
	}
	
	public static AltApi getAltApi() {
		return instance.api;
	}
}
