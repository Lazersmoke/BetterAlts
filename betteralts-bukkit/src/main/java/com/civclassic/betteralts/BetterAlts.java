package com.civclassic.betteralts;

import com.civclassic.betteralts.commands.AltsCommandManager;
import vg.civcraft.mc.civmodcore.ACivMod;

public class BetterAlts extends ACivMod {

	private static BetterAlts instance;

	private ConfigManager configManager;
	private AltManager altManager;

	@Override
	public void onEnable() {
		instance = this;
		handle = new AltsCommandManager();
		handle.registerCommands();
		super.onEnable();
		configManager = new ConfigManager(this);
		configManager.parse();
	}

	public static BetterAlts getInstance() {
		return instance;
	}

	public ConfigManager getConfigManager() {
		return configManager;
	}

	public AltManager getAltManager() {
		return altManager;
	}

	@Override
	protected String getPluginName() {
		return "BetterAlts";
	}
}
