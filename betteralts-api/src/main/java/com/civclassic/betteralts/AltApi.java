package com.civclassic.betteralts;

import java.util.Set;
import java.util.UUID;

public interface AltApi {
	
	/**
	 * Checks that the api initialized properly
	 * @return true if the api intitialized
	 */
	public boolean initialized();
	
	/**
	 * Adds a new player to the database
	 * @param mojangId The mojang issues UUID for the player
	 * @param mojangName The mojang issued name for the player
	 */
	public void addNewPlayer(UUID mojangId, String mojangName);
	
	/**
	 * Adds an alt for a player
	 * @param player The player (alt or main) to add the alt for
	 * @param altName The name for the alt
	 * @param code The secure token for the alt
	 * @return The uuid for the alt or null if this fails
	 */
	public UUID addAlt(UUID player, String altName, String code);
	
	/**
	 * Checks if an alt belongs to a main account
	 * @param main The main account
	 * @param alt The alt to check
	 * @return True if the alt is owned by the main
	 */
	public boolean isValidAlt(UUID main, UUID alt);
	
	/**
	 * Gets the name for an alt
	 * @param altId The uuid of the alt
	 * @return The name of the alt
	 */
	public String getAltName(UUID altId);
	
	/**
	 * Gets an alt uuid by unique login code
	 * @param main The main account
	 * @param code The login code
	 * @return The uuid of the alt or null if one doesn't exist
	 */
	public UUID getAlt(UUID main, String code);
	
	/**
	 * Get the number of alt accounts (not including the main account) for a player
	 * @param player the player to check
	 * @return the number of alts
	 */
	public int getAltCount(UUID player);
	
	/**
	 * Checks if a unique login code exists
	 * @param key The code to check
	 * @return true if the code exists
	 */
	public boolean keyExists(String key);
	
	/**
	 * Gets the maximum number of accounts a player can have online at once
	 * @param mojangId The mojang uuid of the player to check
	 * @return The number of accounts they can have online at once
	 */
	public int getMaxOnlineCount(UUID mojangId);
	
	/**
	 * Gets all alts for a player
	 * @param mojangId The mojang uuid of the player
	 * @return A set of all account uuids (including main)
	 */
	public Set<UUID> getAlts(UUID mojangId);
	
	/**
	 * Checks if the player can take a name for an alt
	 * This checks reservations as well as existing names
	 * @param mojangId The mojang uuid to check for
	 * @param name The name to check
	 * @return True if the player can claim the name
	 */
	public boolean canPlayerTakeName(UUID mojangId, String name);
	
	/**
	 * Increases the maximum number of online accounts for a player by one
	 * @param player Any alt of the player to increase for
	 */
	public void increaseMaxOnline(UUID player);
	
	/**
	 * Gets the unique association id for a player
	 * @param player The player to check for
	 * @return The alt group id
	 */
	public long getAltGroup(UUID player);
}
