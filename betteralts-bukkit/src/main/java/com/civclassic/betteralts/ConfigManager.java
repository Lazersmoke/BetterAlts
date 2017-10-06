package com.civclassic.betteralts;

import com.civclassic.betteralts.storage.Database;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import vg.civcraft.mc.civmodcore.itemHandling.ItemMap;
import vg.civcraft.mc.civmodcore.util.ConfigParsing;

public class ConfigManager {

	private BetterAlts plugin;

	private ItemMap requirements;
	private Database db;
	private String baseIP;

	public ConfigManager(BetterAlts plugin) {
		this.plugin = plugin;
	}

	public void parse() {
		Logger log = plugin.getLogger();
		plugin.saveDefaultConfig();
		FileConfiguration config = plugin.getConfig();
		ConfigurationSection dbConfig = config.getConfigurationSection("database");
		if (dbConfig == null) {
			log.severe("No database credentials provided, shutting down BetterAlts!");
			Bukkit.getServer().getPluginManager().disablePlugin(plugin);
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
			log.log(Level.SEVERE, "Error loading database, check the credentials?", e);
			Bukkit.getServer().getPluginManager().disablePlugin(plugin);
			return;
		}
		requirements = ConfigParsing.parseItemMap(config.getConfigurationSection("requirements"));
		baseIP = config.getString("serverIP");
	}

	public ItemMap getBaseRequirements() {
		return requirements;
	}

	public String getBaseIP() {
		return baseIP;
	}

	public Database getDataBase() {
		return db;
	}

}
