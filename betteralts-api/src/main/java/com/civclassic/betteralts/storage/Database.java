package com.civclassic.betteralts.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {

	public static final String INIT_DATABASE = 
			"CREATE TABLE IF NOT EXISTS alt_accounts (" +
			"  uuid VARCHAR(40) UNIQUE NOT NULL," +
			"  name VARCHAR(16) UNIQUE NOT NULL," +
			"  main VARCHAR(40) NOT NULL," +
			"  code VARCHAR(8) PRIMARY KEY" +
			");" +
			"CREATE TABLE IF NOT EXISTS main_accounts (" +
			"  mojang_id VARCHAR(40) UNIQUE NOT NULL," +
			"  real_id VARCHAR(40) UNIQUE NOT NULL" +
			");";
	
	public static final String CREATE_ALT = "INSERT INTO alt_accounts (uuid, name, main, code) VALUES (?,?,?,?);";
	
	public static final String GET_ALTS = "SELECT * FROM alt_accounts WHERE main=?";
	
	public static final String GET_ALT_BY_CODE = "SELECT uuid FROM alt_accounts WHERE code=?";
	
	public static final String GET_ALT_NAME = "SELECT name FROM alt_accounts WHERE uuid=?";
	
	public static final String ADD_NEW_MAIN = "INSERT INTO main_accounts (mojang_id, real_id) VALUES (?,?);";
	
	public static final String GET_REAL_ID = "SELECT * FROM main_accounts WHERE mojang_id=?;";
	
	public static final String GET_ALT_COUNT = "SELECT COUNT(*) FROM alt_accounts WHERE main=?;";
	
	public static final String GET_KEY_COUNT = "SELECT COUNT(*) FROM alt_accounts WHERE code=?;";
	
	public static final String GET_MAIN = "SELECT main FROM alt_accounts WHERE uuid=?;";
	
	private HikariDataSource datasource;

	public Database(Logger log, String user, String pass, String host, int port, String db,
			int poolSize, long connectionTimeout, long idleTimeout, long maxLife) {
		if(user != null && host != null && port > 0 && db != null) {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db);
			config.setConnectionTimeout(connectionTimeout);
			config.setIdleTimeout(idleTimeout);
			config.setMaxLifetime(maxLife);
			config.setMaximumPoolSize(poolSize);
			config.setUsername(user);
			if(pass != null) {
				config.setPassword(pass);
			}
			datasource = new HikariDataSource(config);
			
			try {
				Connection conn = getConnection();
				PreparedStatement statement = conn.prepareStatement(INIT_DATABASE);
				statement.execute();
				statement.close();
				conn.close();
			} catch (SQLException e) {
				log.log(Level.SEVERE, "Unable to initialize database", e);
				datasource = null;
			}
		} else {
			datasource = null;
			log.log(Level.SEVERE, "Database not configured and is unavailable");
		}
	}
	
	public Connection getConnection() throws SQLException {
		available();
		return datasource.getConnection();
	}
	
	public void close() throws SQLException {
		available();
		datasource.close();
	}
	
	public void available() throws SQLException {
		if(datasource == null) {
			throw new SQLException("No Datasource available");
		}
	}
	
	public boolean addNewPlayer(UUID mojangId) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(ADD_NEW_MAIN)) {
			UUID realId = UUID.randomUUID();
			ps.setString(1, mojangId.toString());
			ps.setString(2, realId.toString());
			ps.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public UUID addAlt(UUID main, String altName, String code) {
		UUID generatedId = UUID.randomUUID();
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(CREATE_ALT)) {
			ps.setString(1, generatedId.toString());
			ps.setString(2, altName);
			ps.setString(3, main.toString());
			ps.setString(4, code);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return generatedId;
	}
	
	public UUID getRealId(UUID mojangId) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_REAL_ID)) {
			ps.setString(1, mojangId.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return UUID.fromString(res.getString("real_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean isValidAlt(UUID main, UUID alt) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_MAIN)) {
			ps.setString(1, alt.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return UUID.fromString(res.getString("main")).equals(main);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public String getAltName(UUID altId) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_ALT_NAME)) {
			ps.setString(1, altId.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return res.getString("name");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public UUID getAltFromCode(String code) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_ALT_BY_CODE)) {
			ps.setString(1, code);
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return UUID.fromString(res.getString("uuid"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public int getAltCount(UUID player) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_ALT_COUNT)) {
			ps.setString(1, player.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return res.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	public boolean keyExists(String key) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_KEY_COUNT)) {
			ps.setString(1, key);
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return res.getInt(1) == 1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
}

