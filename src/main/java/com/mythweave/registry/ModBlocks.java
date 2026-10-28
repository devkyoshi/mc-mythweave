package com.mythweave.registry;

import com.mythweave.Mythweave;
import com.mythweave.block.EmberBrazierBlock;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

import java.util.function.Function;

/** All Mythweave blocks, registered in one place. Their items live in {@link ModItems}. */
public final class ModBlocks {
	// Lantern-like: metal, glows at full light 15 only while lit, not a full cube.
	public static final Block EMBER_BRAZIER = register(
			"ember_brazier",
			EmberBrazierBlock::new,
			BlockBehaviour.Properties.of()
					.mapColor(MapColor.METAL)
					.strength(3.5F)
					.sound(SoundType.LANTERN)
					.lightLevel(state -> state.getValue(EmberBrazierBlock.LIT) ? 15 : 0)
					.noOcclusion()
	);

	private ModBlocks() {
	}

	// Like items, blocks need their registry key set on the properties before construction.
	private static Block register(String name, Function<BlockBehaviour.Properties, Block> factory,
			BlockBehaviour.Properties properties) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Mythweave.id(name));
		return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(properties.setId(key)));
	}

	/** Called from {@link Mythweave#onInitialize()}; loading the class runs the registrations. */
	public static void initialize() {
	}
}
