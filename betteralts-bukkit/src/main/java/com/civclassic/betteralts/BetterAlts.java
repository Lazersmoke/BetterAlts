package com.civclassic.betteralts;

import java.security.SecureRandom;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import com.civclassic.betteralts.storage.Database;

import vg.civcraft.mc.civmodcore.itemHandling.ItemMap;
import vg.civcraft.mc.civmodcore.util.ConfigParsing;
import vg.civcraft.mc.namelayer.NameAPI;

public class BetterAlts extends JavaPlugin {

	private Database db;
	private Logger log;
	private Invocable multInvocer;
	private ItemMap requirements;
	private SecureRandom rng = new SecureRandom();
	
	public void onEnable() {
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
		db = new Database(log, user, pass, host, port, database, poolSize, connectionTimeout, idleTimeout, maxLife);
		try {
			db.available();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Error loading db, check the credentials?", e);
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		String equation = config.getString("scalingEquation");
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("nashorn");
		try {
			engine.eval("var getMultiplier = function(count){ return %%; };".replace("%%", equation));
			multInvocer = (Invocable) engine;
		} catch (ScriptException e) {
			log.log(Level.SEVERE, "Failed to set up scaling invocer", e);
		}
		
		requirements = ConfigParsing.parseItemMap(config.getConfigurationSection("requirements"));
		
		getCommand("createalt").setExecutor(this);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if(!(sender instanceof Player)) {
			
		} else {
			Player player = (Player) sender;
			ItemMap requirements = getRequirements(player);
			if(requirements == null) {
				player.sendMessage(ChatColor.DARK_RED + "An internal error occurred, the creation of alts is temporarily disabled.");
			} else {
				if(requirements.isContainedIn(player.getInventory())) {
					if(args.length == 0) {
						player.sendMessage(ChatColor.RED + "Please specify a name for your new alt.");
					} else {
						String name = args[0];
						if(!canUseName(player, name)) {
							player.sendMessage(ChatColor.RED + "You're not allowed to use that name!");
						} else {
							if(requirements.removeSafelyFrom(player.getInventory())) {
								String loginCode = generateLoginCode();
								NameAPI.getAssociationList().addPlayer(name, db.addAlt(player.getUniqueId(), name, loginCode));
								player.sendMessage(ChatColor.GREEN + "Your alt '" + name + "' was created successfully!");
								player.sendMessage(ChatColor.GREEN + "Log in to the server with the ip: " + loginCode + ".mc.civclassic.com");
							} else {
								player.sendMessage(ChatColor.RED + "Failed to remove required items from your inventory.");
							}
						}
					}
				} else {
					player.sendMessage(ChatColor.RED + "You don't have the required materials, do /altmats to see what you need.");
				}
			}
		}
		return true;
	}

	private ItemMap getRequirements(Player player) {
		int count = db.getAltCount(player.getUniqueId());
		double multiplier;
		try {
			multiplier = (double) multInvocer.invokeFunction("getMultiplier", count);
		} catch (NoSuchMethodException | ScriptException e) {
			log.log(Level.SEVERE, "Error calling multiplier calculation, shutting down", e);
			getServer().getPluginManager().disablePlugin(this);
			return null;
		}
		ItemMap requirements = this.requirements.clone();
		requirements.multiplyContent(multiplier);
		return requirements;
	}
	
	private boolean canUseName(Player player, String name) {
		//TODO implement to have reserved names, not necessary for initial testing
		return true;
	}
	
	private static final char[] alphabet = "QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm1234567890".toCharArray();
	private String generateLoginCode() {
		String code = "";
		do {
			for(int i = 0; i < 8; i++) {
				code += alphabet[rng.nextInt(alphabet.length)];
			}
		} while (db.keyExists(code));
		return code;
	}
}
