package com.civclassic.betteralts.storage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class Database {

	private static final String INIT_DATABASE = 
			"CREATE TABLE IF NOT EXISTS alt_accounts (" +
			"  group BIGINT NOT NULL," +
			"  uuid VARCHAR(40) UNIQUE NOT NULL," +
			"  name VARCHAR(16) UNIQUE NOT NULL," +
			"  code VARCHAR(8) PRIMARY KEY" +
			");" +
			"CREATE TABLE IF NOT EXISTS main_accounts (" +
			"  group bigint auto_increment primary key," +
			"  mojang_id VARCHAR(40) UNIQUE NOT NULL," +
			"  maxonline int not null default 1" +
			");" +
			"CREATE TABLE IF NOT EXISTS reserved_names (" +
			"  reserved_for VARCHAR(40) NOT NULL," +
			"  name VARCHAR(16) PRIMARY KEY);";
	
	private static final String CREATE_ALT = "INSERT INTO alt_accounts (group, uuid, name, code) SELECT main_accounts.group FROM main_accounts WHERE mojang_id=?, ?, ?, ?;";
	
	private static final String GET_ALTS = "SELECT * FROM alt_accounts JOIN (main_accounts) on (main_accounts.group = alt_accounts.group) WHERE main_accounts.mojang_id=?;";
	
	private static final String GET_ALT_BY_CODE = "SELECT uuid FROM alt_accounts WHERE code=?";
	
	private static final String GET_ALT_NAME = "SELECT name FROM alt_accounts WHERE uuid=?";
	
	private static final String ADD_NEW_MAIN = "INSERT INTO main_accounts (mojang_id) VALUES (?);";
	
	private static final String GET_MAX_ONLINE = "SELECT maxonline FROM main_accounts WHERE mojang_id=?;";
	
	private static final String GET_ALT_COUNT = "SELECT COUNT(*) FROM alt_accounts JOIN (main_accounts) on (main_accounts.group = alt_accounts.group) WHERE main_accounts.mojang_id=?;";
	
	private static final String GET_MAIN = "SELECT mojang_id FROM main_accounts JOIN (alt_accounts) on (alt_accounts.group = main_accounts.group) WHERE alt_accounts.uuid=?;";
	
	private static final String KEY_CHECK = "SELECT 1 FROM alt_accounts WHERE code=?;";
	
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
	
	public boolean addNewPlayer(UUID mojangId, String mojangName) {
		try (Connection conn = datasource.getConnection()) {
			PreparedStatement ps = conn.prepareStatement(ADD_NEW_MAIN);
			ps.setString(1, mojangId.toString());
			ps.executeUpdate();
			ps = conn.prepareStatement(GET_ALT_NAME);
			ps.setString(1, mojangId.toString());
			UUID civId = mojangId;
			if(ps.executeQuery().next()) {
				civId = UUID.randomUUID();
			}
			ps = conn.prepareStatement(CREATE_ALT);
			ps.setString(1, mojangId.toString());
			ps.setString(2, civId.toString());
			ps.setString(3, mojangName);
			ps.setString(4, "");
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
			ps.setString(1, main.toString());
			ps.setString(2, generatedId.toString());
			ps.setString(3, altName);
			ps.setString(4, code);
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return generatedId;
	}
	
	public boolean isValidAlt(UUID main, UUID alt) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_MAIN)) {
			ps.setString(1, alt.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return UUID.fromString(res.getString("mojang_id")).equals(main);
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
				PreparedStatement ps = conn.prepareStatement(KEY_CHECK)) {
			ps.setString(1, key);
			ResultSet res = ps.executeQuery();
			return res.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	public int getMaxOnlineCount(UUID player) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_MAX_ONLINE)) {
			ps.setString(1, player.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return res.getInt("maxonline");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 1;
	}
	
	public Set<UUID> getAlts(UUID mojangId) {
		Set<UUID> alts = new HashSet<UUID>();
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_ALTS)) {
			ps.setString(1, mojangId.toString());
			ResultSet res = ps.executeQuery();
			while(res.next()) {
				alts.add(UUID.fromString(res.getString("uuid")));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return alts;
	}
}

