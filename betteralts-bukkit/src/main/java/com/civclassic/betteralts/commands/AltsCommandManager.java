package com.civclassic.betteralts.commands;

import vg.civcraft.mc.civmodcore.command.CommandHandler;

public class AltsCommandManager extends CommandHandler {

	@Override
	public void registerCommands() {
		addCommands(new CreateAltCommand());
	}

}
