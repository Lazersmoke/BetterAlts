package com.civclassic.betteralts;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.event.LoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;
import net.md_5.bungee.connection.InitialHandler;
import net.md_5.bungee.event.EventHandler;

public class BetterAltsBungee extends Plugin implements Listener {

	private Configuration config;
	private AltApi api;
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
		api = new AltManager(log, user, pass, host, port, database, poolSize, connectionTimeout, idleTimeout, maxLife);
		if (!api.initialized()) {
			log.log(Level.SEVERE, "Error loading db, check the credentials?");
			return;
		}
		getProxy().getPluginManager().registerListener(this, this);
	}
	
	@EventHandler
	public void onLogin(LoginEvent event) {
		PendingConnection conn = event.getConnection();
		UUID mojangId = conn.getUniqueId();
		String mojangName = conn.getName();
		api.addNewPlayer(mojangId, mojangName);
		String code = conn.getVirtualHost().getHostString().split("\\.")[0];
		if(code.length() < 8) code = "";
		if(!conn.isOnlineMode()) {
			event.setCancelReason(ChatColor.DARK_RED + "You must be authenticated to connect!");
			event.setCancelled(true);
		} else {
			System.out.println("Code: " + code);
			UUID alt = api.getAlt(mojangId, code);
			if(alt == null || alt.equals(mojangId)) return;
			if(api.isValidAlt(mojangId, alt)) {
				setRealUUID(conn, alt);
				setRealName(conn, conn.getName());
			} else {
				event.setCancelReason(ChatColor.DARK_RED + "Invalid alt code, message modmail if you think this is an error.");
				event.setCancelled(true);
			}
		}
	}
	
	private void setRealUUID(PendingConnection conn, UUID uuid) {
		InitialHandler handle = (InitialHandler) conn;
		try {
			Field uuidField = InitialHandler.class.getDeclaredField("uniqueId");
			uuidField.setAccessible(true);
			uuidField.set(handle, uuid);
			uuidField = InitialHandler.class.getDeclaredField("offlineId");
			uuidField.setAccessible(true);
			uuidField.set(handle, uuid);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Failed to set player uuid", e);
		}
	}
	
	private void setRealName(PendingConnection conn, String name) {
		InitialHandler handle = (InitialHandler) conn;
		try {
			Field nameField = InitialHandler.class.getDeclaredField("name");
			nameField.setAccessible(true);
			nameField.set(handle, name);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			log.log(Level.SEVERE, "Failed to set player name", e);
		}
	}
}
