package com.ashi.mythweave;

import com.ashi.mythweave.command.MythweaveCommands;
import com.ashi.mythweave.mythling.evolution.KindleEvents;
import com.ashi.mythweave.registry.ModBlocks;
import com.ashi.mythweave.registry.ModEntityTypes;
import com.ashi.mythweave.registry.ModItems;
import com.ashi.mythweave.registry.ModSounds;

import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Mythweave implements ModInitializer {
	public static final String MOD_ID = "mythweave";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		// Entity types and blocks first: items refer to CINDERCUB and EMBER_BRAZIER.
		ModSounds.initialize();
		ModEntityTypes.initialize();
		ModBlocks.initialize();
		ModItems.initialize();
		KindleEvents.register();
		MythweaveCommands.register();

		LOGGER.info("Mythweave initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
