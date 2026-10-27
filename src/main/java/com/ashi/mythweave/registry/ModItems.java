package com.ashi.mythweave.registry;

import com.ashi.mythweave.Mythweave;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;

import java.util.function.Function;

/** All Mythweave items, registered in one place. */
public final class ModItems {
	// In 26.3 a spawn egg is a plain SpawnEggItem; the entity it spawns comes from
	// Item.Properties#spawnEgg rather than a constructor argument.
	public static final Item CINDERCUB_SPAWN_EGG = register(
			"cindercub_spawn_egg",
			SpawnEggItem::new,
			new Item.Properties().spawnEgg(ModEntities.CINDERCUB)
	);

	// Block items use the block's name ("block.mythweave.ember_brazier"), like vanilla's.
	public static final Item EMBER_BRAZIER = register(
			"ember_brazier",
			properties -> new BlockItem(ModBlocks.EMBER_BRAZIER, properties),
			new Item.Properties().useBlockDescriptionPrefix()
	);

	private ModItems() {
	}

	// Items also need their registry key set on the properties before construction (1.21.2+).
	private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Mythweave.id(name));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	/**
	 * Called from {@link Mythweave#onInitialize()}, after entities and blocks; loading the class
	 * runs the registrations. Creative-tab placement is in {@link ModCreativeTabs}.
	 */
	public static void initialize() {
	}
}
