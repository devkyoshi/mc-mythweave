package com.ashi.mythweave.registry;

import com.ashi.mythweave.Mythweave;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;

import net.minecraft.world.item.CreativeModeTabs;

/** Where Mythweave's items appear in the Creative inventory (vanilla tabs, for now). */
public final class ModCreativeTabs {
	private ModCreativeTabs() {
	}

	/** Called from {@link Mythweave#onInitialize()}, after {@link ModItems}. */
	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.SPAWN_EGGS)
				.register(output -> {
					output.accept(ModItems.CINDERCUB_SPAWN_EGG);
					output.accept(ModItems.EMBERLYNX_SPAWN_EGG);
					output.accept(ModItems.ASHENOX_SPAWN_EGG);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS)
				.register(output -> output.accept(ModItems.EMBER_BRAZIER));
	}
}
