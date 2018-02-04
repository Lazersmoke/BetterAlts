package com.civclassic.betteralts;

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

public class AltManager implements AltApi {

	private static final String ALT_TABLE = 
			"CREATE TABLE IF NOT EXISTS alt_accounts (" +
			"  group_id BIGINT NOT NULL," +
			"  uuid VARCHAR(40) UNIQUE NOT NULL," +
			"  name VARCHAR(16) UNIQUE NOT NULL," +
			"  code VARCHAR(8) PRIMARY KEY" +
			");";
	private static final String MAIN_TABLE =
			"CREATE TABLE IF NOT EXISTS main_accounts (" +
			"  group_id BIGINT AUTO_INCREMENT," +
			"  mojang_id VARCHAR(40) UNIQUE NOT NULL," +
			"  maxonline INT NOT NULL DEFAULT 1," +
			"  PRIMARY KEY(group_id)" +
			");";
	private static final String RESERVATION_TABLE =
			"CREATE TABLE IF NOT EXISTS reserved_names (" +
			"  reserved_for VARCHAR(40) NOT NULL," +
			"  name VARCHAR(16) PRIMARY KEY);";
	
	private static final String CREATE_ALT = "INSERT INTO alt_accounts (group_id, uuid, name, code) SELECT main_accounts.group_id, ?, ?, ? FROM main_accounts WHERE mojang_id=?;";
	
	private static final String GET_ALTS = "SELECT * FROM alt_accounts as a JOIN (alt_accounts as b) on (a.group_id = b.group_id) WHERE b.uuid=?;";
	
	private static final String GET_ALT_BY_CODE = "SELECT uuid FROM alt_accounts JOIN (main_accounts) on (main_accounts.group_id = alt_accounts.group_id) WHERE alt_accounts.code=? AND main_accounts.mojang_id=?;";
	
	private static final String GET_ALT_NAME = "SELECT name FROM alt_accounts WHERE uuid=?";
	
	private static final String ADD_NEW_MAIN = "INSERT INTO main_accounts (mojang_id) VALUES (?);";
	
	private static final String GET_MAX_ONLINE = "SELECT maxonline FROM main_accounts WHERE mojang_id=?;";
	
	private static final String GET_ALT_COUNT = "SELECT COUNT(*) FROM alt_accounts as a JOIN (alt_accounts as b) on (a.group_id = b.group_id) WHERE b.uuid=?;";
	
	private static final String GET_MAIN = "SELECT mojang_id FROM main_accounts JOIN (alt_accounts) on (alt_accounts.group_id = main_accounts.group_id) WHERE alt_accounts.uuid=?;";
	
	private static final String KEY_CHECK = "SELECT 1 FROM alt_accounts WHERE code=?;";
	
	private static final String GET_NAME_RESERVATION = "SELECT reserved_for FROM reserved_names WHERE name=?;";
	
	private static final String NAME_CHECK = "SELECT 1 FROM alt_accounts WHERE name=?;";
	
	private static final String INCREASE_MAX_ONLINE = "UPDATE main_accounts SET maxonline = maxonline + 1 JOIN (alt_accounts) on (alt_accounts.group_id = main_accounts.group_id) WHERE alt_accounts.uuid=?;";
	
	private static final String GET_ALT_GROUP = "SELECT group_id FROM alt_accounts WHERE uuid=?;";
	
	private static final String UPDATE_NAME = "UPDATE alt_accounts SET name=? WHERE uuid=?;";
			
	private HikariDataSource datasource;

	public AltManager(Logger log, String user, String pass, String host, int port, String db,
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
				PreparedStatement statement = conn.prepareStatement(ALT_TABLE);
				statement.execute();
				statement = conn.prepareStatement(MAIN_TABLE);
				statement.execute();
				statement = conn.prepareStatement(RESERVATION_TABLE);
				statement.execute();
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
	
	private Connection getConnection() throws SQLException {
		available();
		return datasource.getConnection();
	}
	
	private void available() throws SQLException {
		if(datasource == null) {
			throw new SQLException("No Datasource available");
		}
	}
	
	@Override
	public boolean initialized() {
		return datasource != null;
	}

	@Override
	public void addNewPlayer(UUID mojangId, String mojangName) {
		try (Connection conn = getConnection()) {
			PreparedStatement ps = conn.prepareStatement("SELECT * FROM main_accounts WHERE mojang_id=?;");
			ps.setString(1, mojangId.toString());
			if(ps.executeQuery().next()) {
				System.out.println("Player already in the system, skipping.");
				return;
			}
			System.out.println("Adding new main account player.");
			ps = conn.prepareStatement(ADD_NEW_MAIN);
			ps.setString(1, mojangId.toString());
			ps.executeUpdate();
			ps = conn.prepareStatement(GET_ALT_NAME);
			ps.setString(1, mojangId.toString());
			UUID civId = mojangId;
			if(ps.executeQuery().next()) {
				civId = UUID.randomUUID();
			}
			ps = conn.prepareStatement(CREATE_ALT);
			ps.setString(1, civId.toString());
			ps.setString(2, mojangName);
			ps.setString(3, "");
			ps.setString(4, mojangId.toString());
			ps.executeUpdate();
			return;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return;
	}
	
	@Override
	public UUID addAlt(UUID player, String altName, String code) {
		try (Connection conn = getConnection();) {
			PreparedStatement ps = conn.prepareStatement(GET_MAIN);
			ps.setString(1, player.toString());
			ResultSet res = ps.executeQuery();
			if(!res.next()) {
				return null;
			}
			UUID main = UUID.fromString(res.getString("mojang_id"));
			UUID generatedId = UUID.randomUUID();
			ps = conn.prepareStatement(CREATE_ALT);
			ps.setString(4, main.toString());
			ps.setString(1, generatedId.toString());
			ps.setString(2, altName);
			ps.setString(3, code);
			ps.executeUpdate();
			return generatedId;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public boolean isValidAlt(UUID main, UUID alt) {
		try (Connection conn = getConnection();
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
	
	@Override
	public String getAltName(UUID altId) {
		try (Connection conn = getConnection();
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
	
	@Override
	public UUID getAlt(UUID main, String code) {
		try (Connection conn = getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_ALT_BY_CODE)) {
			ps.setString(1, code);
			ps.setString(2, main.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return UUID.fromString(res.getString("uuid"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public int getAltCount(UUID player) {
		try (Connection conn = getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_ALT_COUNT)) {
			ps.setString(1, player.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return res.getInt(1) -1;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	@Override
	public boolean keyExists(String key) {
		try (Connection conn = getConnection();
				PreparedStatement ps = conn.prepareStatement(KEY_CHECK)) {
			ps.setString(1, key);
			ResultSet res = ps.executeQuery();
			return res.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
	
	@Override
	public int getMaxOnlineCount(UUID mojangId) {
		try (Connection conn = getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_MAX_ONLINE)) {
			ps.setString(1, mojangId.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return res.getInt("maxonline");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 1;
	}
	
	@Override
	public Set<UUID> getAlts(UUID mojangId) {
		Set<UUID> alts = new HashSet<UUID>();
		try (Connection conn = getConnection();
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

	@Override
	public boolean canPlayerTakeName(UUID mojangId, String name) {
		try (Connection conn = getConnection();) {
			PreparedStatement ps = conn.prepareStatement(GET_NAME_RESERVATION);
			ps.setString(1, name);
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return UUID.fromString(res.getString("reserved_for")).equals(mojangId);
			}
			ps = conn.prepareStatement(NAME_CHECK);
			ps.setString(1, name);
			res = ps.executeQuery();
			return !res.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	@Override
	public void increaseMaxOnline(UUID player) {
		try (Connection conn = getConnection();
				PreparedStatement ps = conn.prepareStatement(INCREASE_MAX_ONLINE)) {
			ps.setString(1, player.toString());
			ps.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public long getAltGroup(UUID player) {
		try (Connection conn = getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_ALT_GROUP)) {
			ps.setString(1, player.toString());
			ResultSet res = ps.executeQuery();
			if(res.next()) {
				return res.getLong("group");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}
}

