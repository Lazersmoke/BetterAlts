package com.civclassic.betteralts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.civclassic.betteralts.storage.Database;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.PreLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.protocol.packet.LoginRequest;

public class BetterAltsBungee extends Plugin implements Listener {

	private Configuration config;
	private Database db;
	private Logger log;
	
	public void onEnable() {
		log = getLogger();
		try {
			if(!getDataFolder().exists()) getDataFolder().mkdir();

			File configFile = new File(getDataFolder(), "config.yml");

			if(!configFile.exists()) {
				try (InputStream in = getResourceAsStream("config.yml")) {
					Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if(config == null) {
			log.severe("No database credentials, shut her down!");
			return;
		}
		String user = config.getString("user");
		String pass = config.getString("password");
		String host = config.getString("host");
		int port = config.getInt("port");
		String database = config.getString("db");
		int poolSize = config.getInt("poolSize");
		long connectionTimeout = config.getLong("connectionTimeout");
		long idleTimeout = config.getLong("idleTimeout");
		long maxLife = config.getLong("maxLifetime");
		db = new Database(log, user, pass, host, port, database, poolSize, connectionTimeout, idleTimeout, maxLife);
		try {
			db.available();
		} catch (SQLException e) {
			log.log(Level.SEVERE, "Error loading db, check the credentials?", e);
			return;
		}
		getProxy().getPluginManager().registerListener(this, this);
	}
	
	@EventHandler
	public void onPreLogin(PreLoginEvent event) {
		PendingConnection conn = event.getConnection();
		UUID mojangId = conn.getUniqueId();
		UUID main;
		do {
			db.addNewPlayer(mojangId);
		} while ((main = db.getRealId(mojangId)) == null);
		conn.setUniqueId(main);
		String code = conn.getVirtualHost().getHostString().split("\\.")[0];
		if(code == "mc" || code == "play") return;
		if(!conn.isOnlineMode()) {
			event.setCancelReason(ChatColor.DARK_RED + "You must be authenticated to connect!");
			event.setCancelled(true);
		} else {
			UUID alt = db.getAltFromCode(code);
			if(db.isValidAlt(main, alt)) {
				conn.setUniqueId(alt);
				setRealName(conn, db.getAltName(alt));
			} else {
				event.setCancelReason(ChatColor.DARK_RED + "Invalid alt code, message modmail if you think this is an error.");
				event.setCancelled(true);
			}
		}
	}
	
	private void setRealName(PendingConnection conn, String name) {
		InitialHandler handle = (InitialHandler) conn;
		try {
			Field nameField = InitialHandler.class.getDeclaredField("name");
			nameField.setAccessible(true);
			nameField.set(handle, name);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			//e.printStackTrace();
		}
	}
}
