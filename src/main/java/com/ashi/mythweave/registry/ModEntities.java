package com.ashi.mythweave.registry;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.entity.base.MythlingEntity;
import com.ashi.mythweave.entity.cindercub.CindercubEntity;
import com.ashi.mythweave.entity.emberlynx.EmberlynxEntity;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

import java.util.Map;
import java.util.Optional;

/** All Mythweave entity types, registered in one place. */
public final class ModEntities {
	public static final EntityType<CindercubEntity> CINDERCUB = register(
			"cindercub",
			// CREATURE = passive animal category (counts toward the animal spawn cap later).
			// 0.5 wide x 0.7 tall, a bit smaller than a vanilla fox (0.6 x 0.7). The height
			// covers the model up to the top of its big head (11 of 16 pixels); ears, horns
			// and flames stick out above it. Eye height matches the model's eyes (9 pixels up).
			EntityType.Builder.of(CindercubEntity::new, MobCategory.CREATURE)
					.sized(0.5f, 0.7f)
					.eyeHeight(0.56f)
	);

	public static final EntityType<EmberlynxEntity> EMBERLYNX = register(
			"emberlynx",
			// Stage 2 of the Cindercub line: taller and leaner. 0.6 wide x 0.95 tall covers the
			// model to the top of its head (15.5 of 16 pixels); ears, tufts, horns and the fire
			// mane stick out above. Eyes are 13.5 pixels up.
			EntityType.Builder.of(EmberlynxEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 0.95f)
					.eyeHeight(0.84f)
	);

	/**
	 * Every Mythling form that exists, by the id its {@link com.ashi.mythweave.evolution.EvolutionLine}
	 * uses. Evolution turns a form id into an entity type through this; a form that isn't here
	 * yet (not built) simply can't be evolved into.
	 */
	private static final Map<String, EntityType<? extends MythlingEntity>> MYTHLING_FORMS = Map.of(
			"cindercub", CINDERCUB,
			"emberlynx", EMBERLYNX
	);

	private ModEntities() {
	}

	/** The entity type for a Mythling form id (e.g. "emberlynx"), if that form exists yet. */
	public static Optional<EntityType<? extends MythlingEntity>> mythlingForm(String formId) {
		return Optional.ofNullable(MYTHLING_FORMS.get(formId));
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
		FabricDefaultAttributeRegistry.register(EMBERLYNX, EmberlynxEntity.createAttributes());
	}
}
