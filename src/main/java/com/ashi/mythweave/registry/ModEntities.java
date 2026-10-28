package com.ashi.mythweave.registry;

import com.ashi.mythweave.Mythweave;
import com.ashi.mythweave.entity.ashenox.AshenoxEntity;
import com.ashi.mythweave.entity.base.MythlingEntity;
import com.ashi.mythweave.entity.cindercub.CindercubEntity;
import com.ashi.mythweave.entity.emberlynx.EmberlynxEntity;
import com.ashi.mythweave.entity.solflare.SolflareEntity;

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

/**
 * All Mythweave entity types, registered in one place.
 *
 * <p>Sizes here match each model at scale 1. A form's {@code SCALE} attribute (in its
 * {@code createAttributes()}) multiplies the hitbox, eye height and model together, so later
 * stages are bigger: Cindercub 1.0, Emberlynx 1.15, Ashenox and Solflare 1.3.
 */
public final class ModEntities {
	public static final EntityType<CindercubEntity> CINDERCUB = register(
			"cindercub",
			// CREATURE = passive animal category (counts toward the animal spawn cap later).
			// 0.6 wide x 0.9 tall, the size in the concept sheets (docs/reference/cindercub).
			// The height covers the model up to the top of its head (14.7 of 16 pixels);
			// the ears and the ember tail stick out above it. Eye height matches the model's
			// eyes (12.3 pixels up).
			EntityType.Builder.of(CindercubEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 0.9f)
					.eyeHeight(0.77f)
	);

	public static final EntityType<EmberlynxEntity> EMBERLYNX = register(
			"emberlynx",
			// Stage 2 of the Cindercub line: taller and leaner. 0.6 wide x 0.95 tall covers the
			// model to the top of its head (15.5 of 16 pixels); ears, tufts, horns and the fire
			// mane stick out above. Eyes are 13.5 pixels up. Scale 1.15: 0.69 x 1.09 in game.
			EntityType.Builder.of(EmberlynxEntity::new, MobCategory.CREATURE)
					.sized(0.6f, 0.95f)
					.eyeHeight(0.84f)
	);

	public static final EntityType<AshenoxEntity> ASHENOX = register(
			"ashenox",
			// Stage 3 (combat) of the Cindercub line: heavy and armored. 0.8 wide x 1.1 tall
			// covers the body and head (top at 17 of 16 pixels); horns and mane stick out above.
			// Eyes are 14.5 pixels up. Fire-immune: a volcanic creature doesn't burn in lava.
			// Scale 1.3: 1.04 x 1.43 in game.
			EntityType.Builder.of(AshenoxEntity::new, MobCategory.CREATURE)
					.sized(0.8f, 1.1f)
					.eyeHeight(0.9f)
					.fireImmune()
	);

	public static final EntityType<SolflareEntity> SOLFLARE = register(
			"solflare",
			// Stage 3 (exploration) of the Cindercub line: lean and long-legged. 0.7 wide x 1.1
			// tall covers the body and head (top at 17.5 of 16 pixels); ears, crest and wings
			// stick out above. Eyes are 15 pixels up. Fire-immune: it's made of sunfire.
			// Scale 1.3: 0.91 x 1.43 in game.
			EntityType.Builder.of(SolflareEntity::new, MobCategory.CREATURE)
					.sized(0.7f, 1.1f)
					.eyeHeight(0.94f)
					.fireImmune()
	);

	/**
	 * Every Mythling form that exists, by the id its {@link com.ashi.mythweave.evolution.EvolutionLine}
	 * uses. Evolution turns a form id into an entity type through this; a form that isn't here
	 * yet (not built) simply can't be evolved into.
	 */
	private static final Map<String, EntityType<? extends MythlingEntity>> MYTHLING_FORMS = Map.of(
			"cindercub", CINDERCUB,
			"emberlynx", EMBERLYNX,
			"ashenox", ASHENOX,
			"solflare", SOLFLARE
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
		FabricDefaultAttributeRegistry.register(ASHENOX, AshenoxEntity.createAttributes());
		FabricDefaultAttributeRegistry.register(SOLFLARE, SolflareEntity.createAttributes());
	}
}
