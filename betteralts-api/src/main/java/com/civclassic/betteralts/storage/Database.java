package com.civclassic.betteralts.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Database {

	private static final String INIT_DATABASE = "CREATE TABLE IF NOT EXISTS alt_accounts ("
			+ "  uuid VARCHAR(40) UNIQUE NOT NULL,  name VARCHAR(16) UNIQUE NOT NULL,"
			+ "  altGroupId int NOT NULL,  code VARCHAR(8) NOT NULL, primary key(uuid), "
			+ "index `nameIndex` (name),  index `altIdIndex` (altGroupId),  index `codeIndex` (code));"
			+ "CREATE TABLE IF NOT EXISTS uuidRemapping (mojang_id VARCHAR(40) UNIQUE NOT NULL,"
			+ "  serverside_id VARCHAR(40) UNIQUE NOT NULL, primary key(mojang_id), "
			+ "constraint `foreignUUIDKey` foreign key (serverside_id) references alt_accounts(uuid) on delete cascade;";

	private static final String CREATE_ALT = "INSERT INTO alt_accounts (uuid, name, main, code) VALUES (?,?,?,?);";

	private static final String GET_ALTS = "SELECT a2.uuid, a2.code FROM alt_accounts a1 inner join alt_accounts a2 on a1.altGroupId = a2.altGroupId WHERE a1.uuid=?";

	private static final String GET_ALT_BY_CODE = "SELECT uuid FROM alt_accounts WHERE code=?";

	private static final String GET_ALT_NAME = "SELECT name FROM alt_accounts WHERE uuid=?";

	private static final String GET_MAX_ALTID = "select max(altGroupId) from alt_accounts;";

	private static final String ADD_REMAPPING = "INSERT INTO uuidRemapping (mojang_id, serverside_id) VALUES (?,?);";

	private static final String GET_SERVERSIDE_ID = "SELECT * FROM uuidRemapping WHERE mojang_id=?;";

	private static final String GET_ALT_COUNT = "SELECT COUNT(*) FROM alt_accounts a1 inner join alt_accounts a2 on a1.altGroupId = a2.altGroupId WHERE a1.uuid=?;";

	private static final String GET_KEY_COUNT = "SELECT COUNT(*) FROM alt_accounts WHERE code=?;";

	private static final String GET_CODE_BY_ALT = "select code from alt_accounts where uuid = ?;";

	private static final String CHECK_NAME = "SELECT * FROM alt_accounts WHERE name=?;";

	private static final String GET_ALT_ID = "SELECT altGroupId FROM alt_accounts WHERE uuid=?;";

	private HikariDataSource datasource;

	public Database(Logger log, String user, String pass, String host, int port, String db, int poolSize,
			long connectionTimeout, long idleTimeout, long maxLife) {
		if (user != null && host != null && port > 0 && db != null) {
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + db);
			config.setConnectionTimeout(connectionTimeout);
			config.setIdleTimeout(idleTimeout);
			config.setMaxLifetime(maxLife);
			config.setMaximumPoolSize(poolSize);
			config.setUsername(user);
			if (pass != null) {
				config.setPassword(pass);
			}
			datasource = new HikariDataSource(config);

			try (Connection conn = getConnection(); PreparedStatement statement = conn.prepareStatement(INIT_DATABASE);) {
				statement.execute();
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
		if (datasource == null) {
			throw new SQLException("No Datasource available");
		}
	}

	public UUID addNewPlayer(UUID mojangId, String name) {
		// TODO enforce name uniqueness in case the name of a new player is taken
		UUID playerUUID;
		if (isUUIDTaken(mojangId)) {
			playerUUID = getAvailableUUID();
			addUUIDRemapping(mojangId, playerUUID);
		} else {
			playerUUID = mojangId;
		}
		int altId = getMaximumAltId() + 1;
		try (Connection conn = datasource.getConnection(); PreparedStatement ps = conn.prepareStatement(CREATE_ALT)) {
			ps.setString(1, playerUUID.toString());
			ps.setString(2, name);
			ps.setInt(3, altId);
			ps.setString(4, "");
			ps.execute();
			return playerUUID;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	private int getMaximumAltId() {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_MAX_ALTID);
				ResultSet rs = ps.executeQuery()) {
			rs.next();
			return rs.getInt(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	private void addUUIDRemapping(UUID mojang, UUID serverSide) {
		try (Connection conn = datasource.getConnection(); PreparedStatement ps = conn.prepareStatement(ADD_REMAPPING)) {
			ps.setString(1, mojang.toString());
			ps.setString(2, serverSide.toString());
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private boolean isUUIDTaken(UUID uuid) {
		try (Connection conn = datasource.getConnection(); PreparedStatement ps = conn.prepareStatement(CREATE_ALT)) {
			ps.setString(1, uuid.toString());
			try (ResultSet rs = ps.executeQuery()) {
				return rs.next();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		// always assume taken in case of error to avoid messing up something
		return true;
	}

	public UUID addAlt(UUID main, String altName, String code) {
		UUID generatedId = getAvailableUUID();
		try (Connection conn = datasource.getConnection(); PreparedStatement ps = conn.prepareStatement(CREATE_ALT)) {
			ps.setString(1, generatedId.toString());
			ps.setString(2, altName);
			ps.setInt(3, getAltId(main));
			ps.setString(4, code);
			ps.execute();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return generatedId;
	}

	private UUID getAvailableUUID() {
		UUID generatedId;
		do {
			generatedId = UUID.randomUUID();
		} while (isUUIDTaken(generatedId));
		return generatedId;
	}

	public int getAltId(UUID uuid) {
		try (Connection conn = datasource.getConnection(); PreparedStatement ps = conn.prepareStatement(CREATE_ALT)) {
			ps.setString(1, uuid.toString());
			try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					return rs.getInt(1);
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return -1;
	}

	public UUID getServerSideId(UUID mojangId) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_SERVERSIDE_ID)) {
			ps.setString(1, mojangId.toString());
			ResultSet res = ps.executeQuery();
			if (res.next()) {
				return UUID.fromString(res.getString("real_id"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return mojangId;
	}

	public String getAltName(UUID altId) {
		try (Connection conn = datasource.getConnection(); PreparedStatement ps = conn.prepareStatement(GET_ALT_NAME)) {
			ps.setString(1, altId.toString());
			ResultSet res = ps.executeQuery();
			if (res.next()) {
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
			if (res.next()) {
				return UUID.fromString(res.getString("uuid"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public String getCodeByUUID(UUID player) {
		try (Connection conn = datasource.getConnection();
				PreparedStatement ps = conn.prepareStatement(GET_CODE_BY_ALT)) {
			ps.setString(1, player.toString());
			ResultSet res = ps.executeQuery();
			if (res.next()) {
				return res.getString(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public int getAltCount(UUID player) {
		try (Connection conn = datasource.getConnection(); PreparedStatement ps = conn.prepareStatement(GET_ALT_COUNT)) {
			ps.setString(1, player.toString());
			ResultSet res = ps.executeQuery();
			if (res.next()) {
				return res.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return 0;
	}

	public boolean isNameTaken(String name) {
		try (Connection conn = datasource.getConnection(); PreparedStatement ps = conn.prepareStatement(CHECK_NAME)) {
			ps.setString(1, name);
			ResultSet res = ps.executeQuery();
			return res.next();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return true;
	}

	public boolean keyExists(String key) {
		try (Connection conn = datasource.getConnection(); PreparedStatement ps = conn.prepareStatement(GET_KEY_COUNT)) {
			ps.setString(1, key);
			ResultSet res = ps.executeQuery();
			if (res.next()) {
				return res.getInt(1) > 0;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}
}
