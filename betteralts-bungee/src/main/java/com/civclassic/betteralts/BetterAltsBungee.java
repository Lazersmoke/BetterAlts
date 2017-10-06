package com.civclassic.betteralts;

import com.civclassic.betteralts.storage.Database;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
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
	private List<String> baseIps;

	@Override
	public void onEnable() {
		log = getLogger();
		try {
			if (!getDataFolder().exists()) {
				getDataFolder().mkdir();
			}

			File configFile = new File(getDataFolder(), "config.yml");

			if (!configFile.exists()) {
				try (InputStream in = getResourceAsStream("config.yml")) {
					Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
				}
			}
			config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (config == null) {
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
		baseIps = config.getStringList("baseIPs");
		getProxy().getPluginManager().registerListener(this, this);
	}

	@EventHandler
	public void onPreLogin(PreLoginEvent event) {
		PendingConnection conn = event.getConnection();
		if (!conn.isOnlineMode()) {
			event.setCancelReason(ChatColor.RED + "You must be authenticated to connect!");
			event.setCancelled(true);
			return;
		}
		UUID mojangId = conn.getUniqueId();
		// first check for remap
		UUID serverSideId = db.getServerSideId(mojangId);

		int altId = db.getAltId(mojangId);
		// if altId is -1, the player is logging in for the first time
		if (altId == -1) {
			serverSideId = db.addNewPlayer(serverSideId, conn.getName());
		}

		// check if player is using right login code
		String loginCode = db.getCodeByUUID(serverSideId);

		if (!conn.getVirtualHost().getHostString().startsWith(loginCode)) {
			event.setCancelReason(ChatColor.RED + "You are using an invalid login id");
			event.setCancelled(true);
			return;
		}
		conn.setUniqueId(serverSideId);
		setRealName(conn, db.getAltName(serverSideId));
	}

	private void setRealName(PendingConnection conn, String name) {
		InitialHandler handle = (InitialHandler) conn;
		try {
			Field loginField = InitialHandler.class.getDeclaredField("loginRequest");
			loginField.setAccessible(true);
			LoginRequest request = (LoginRequest) loginField.get(handle);
			request.setData(name);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			// e.printStackTrace();
		}
		try {
			Field nameField = InitialHandler.class.getDeclaredField("name");
			nameField.setAccessible(true);
			nameField.set(handle, name);
		} catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException e) {
			// e.printStackTrace();
		}
	}
}
