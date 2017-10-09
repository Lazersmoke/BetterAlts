package com.civclassic.betteralts.storage;

import java.util.UUID;

public class AltAccount {
	
	private UUID uuid;
	private String name;
	private String code;
	
	public AltAccount(UUID uuid, String name, String code) {
		this.uuid = uuid;
		this.name = name;
		this.code = code;
	}

	public UUID getUUID() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public String getCode() {
		return code;
	}
}
