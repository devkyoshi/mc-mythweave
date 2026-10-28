package com.mythweave;

import com.mythweave.event.ModEvents;
import com.mythweave.registry.ModBlocks;
import com.mythweave.registry.ModCreativeTabs;
import com.mythweave.registry.ModEntities;
import com.mythweave.registry.ModItems;
import com.mythweave.registry.ModParticles;
import com.mythweave.registry.ModSounds;

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
		ModParticles.initialize();
		ModEntities.initialize();
		ModBlocks.initialize();
		ModItems.initialize();
		ModCreativeTabs.initialize();
		// Then everything that reacts to the game: event listeners and commands.
		ModEvents.register();

		LOGGER.info("Mythweave initialized");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
