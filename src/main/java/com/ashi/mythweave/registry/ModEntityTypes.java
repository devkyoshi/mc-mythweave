package com.ashi.mythweave.registry;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.entity.CindercubEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/** All Mythweave entity types, registered in one place. */
public final class ModEntityTypes {
	public static final EntityType<CindercubEntity> CINDERCUB = register(
			"cindercub",
			// CREATURE = passive animal category (counts toward the animal spawn cap later).
			// 0.5 wide x 0.65 tall, a bit smaller than a vanilla fox (0.6 x 0.7). The height
			// covers the model up to the top of the head; the ears stick out above it, like
			// a fox's. Eye height matches the model's eyes (8.5 of 16 pixels up).
			EntityType.Builder.of(CindercubEntity::new, MobCategory.CREATURE)
					.sized(0.5f, 0.65f)
					.eyeHeight(0.53f)
	);

	private ModEntityTypes() {
	}

	// Since 1.21.2 an EntityType must be built with its own registry key, so the key is
	// created first and passed to both build() and Registry.register().
	private static <T extends Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
		ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, Mythweave.id(name));
		return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, builder.build(key));
	}

	/** Called from {@link Mythweave#onInitialize()}. Loading this class runs the registrations above. */
	public static void initialize() {
		// Every LivingEntity needs default attributes, or the game crashes when one spawns.
		FabricDefaultAttributeRegistry.register(CINDERCUB, CindercubEntity.createAttributes());
	}
}
