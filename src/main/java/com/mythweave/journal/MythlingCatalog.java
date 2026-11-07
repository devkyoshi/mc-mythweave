package com.mythweave.journal;

import com.mythweave.entity.base.MythlingEntity;
import com.mythweave.evolution.EvolutionLine;
import com.mythweave.evolution.EvolutionLines;
import com.mythweave.evolution.EvolutionRules;
import com.mythweave.registry.ModEntities;
import com.mythweave.registry.ModItems;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The journal's ordered list of Mythling forms and the small amount of hand-written
 * information (element, rarity, habitat, description, abilities) that can't be derived from
 * an entity type. Evolution data comes from {@link EvolutionRules}.
 */
public final class MythlingCatalog {
	private static final String ELEMENT_FIRE = "mythling.mythweave.element.fire";
	private static final String ELEMENT_EARTH = "mythling.mythweave.element.earth";

	private static final List<MythlingProfile> PROFILES = buildProfiles();
	private static final Map<String, MythlingProfile> BY_ID = buildIndex();

	private MythlingCatalog() {
	}

	private static List<MythlingProfile> buildProfiles() {
		List<MythlingProfile> profiles = new ArrayList<>();
		for (EvolutionLine line : EvolutionLines.ALL) {
			profiles.add(new MythlingProfile(
					line.base(), ModItems.CINDERCUB_SPAWN_EGG, Items.BLAZE_POWDER,
					List.of(ELEMENT_FIRE),
					"mythling.mythweave.cindercub.rarity",
					"mythling.mythweave.cindercub.habitat",
					"mythling.mythweave.cindercub.description",
					List.of(
							"mythling.mythweave.cindercub.ability.ember_spark",
							"mythling.mythweave.cindercub.ability.timid"
					)
			));
			profiles.add(new MythlingProfile(
					line.stage2(), ModItems.ASHENOX_SPAWN_EGG, Items.BLAZE_POWDER,
					List.of(ELEMENT_FIRE, ELEMENT_EARTH),
					"mythling.mythweave.ashenox.rarity",
					"mythling.mythweave.ashenox.habitat",
					"mythling.mythweave.ashenox.description",
					List.of(
							"mythling.mythweave.ashenox.ability.ember_spark",
							"mythling.mythweave.ashenox.ability.magma_bite",
							"mythling.mythweave.ashenox.ability.fire_immune"
					)
			));
			profiles.add(new MythlingProfile(
					line.stage3(), ModItems.EMBERLYNX_SPAWN_EGG, Items.BLAZE_POWDER,
					List.of(ELEMENT_FIRE),
					"mythling.mythweave.emberlynx.rarity",
					"mythling.mythweave.emberlynx.habitat",
					"mythling.mythweave.emberlynx.description",
					List.of(
							"mythling.mythweave.emberlynx.ability.ember_spark",
							"mythling.mythweave.emberlynx.ability.defender"
					)
			));
		}
		return List.copyOf(profiles);
	}

	private static Map<String, MythlingProfile> buildIndex() {
		Map<String, MythlingProfile> index = new LinkedHashMap<>();
		for (MythlingProfile profile : PROFILES) {
			index.put(profile.formId(), profile);
		}
		return Map.copyOf(index);
	}

	/** Every form the journal knows about, in evolution-line order. */
	public static List<MythlingProfile> profiles() {
		return PROFILES;
	}

	public static Optional<MythlingProfile> profile(String formId) {
		return Optional.ofNullable(BY_ID.get(formId));
	}

	/** The item shown as a form's small journal icon. */
	public static Item spawnEgg(String formId) {
		return profile(formId).map(MythlingProfile::spawnEgg).orElse(Items.BARRIER);
	}

	/** A form's stage in its line: 1, 2, 3, or 0 if it isn't in a known line. */
	public static int stageOf(String formId) {
		return lineOf(formId).map(line -> EvolutionRules.stageOf(line, formId)).orElse(0);
	}

	/** The evolution line a form belongs to, if known. */
	public static Optional<EvolutionLine> lineOf(String formId) {
		for (EvolutionLine line : EvolutionLines.ALL) {
			if (EvolutionRules.stageOf(line, formId) > 0) {
				return Optional.of(line);
			}
		}
		return Optional.empty();
	}

	public static Optional<EntityType<? extends MythlingEntity>> entityType(String formId) {
		return ModEntities.mythlingForm(formId);
	}

	/** A form's real in-game width, including its SCALE attribute. */
	public static float width(String formId) {
		return entityType(formId).map(type -> dimensions(type).width()).orElse(0.0F);
	}

	/** A form's real in-game height, including its SCALE attribute. */
	public static float height(String formId) {
		return entityType(formId).map(type -> dimensions(type).height()).orElse(0.0F);
	}

	public static float maxHealth(String formId) {
		return baseAttribute(formId, Attributes.MAX_HEALTH);
	}

	public static float armor(String formId) {
		return baseAttribute(formId, Attributes.ARMOR);
	}

	public static float attackDamage(String formId) {
		return baseAttribute(formId, Attributes.ATTACK_DAMAGE);
	}

	@SuppressWarnings("unchecked")
	private static float baseAttribute(String formId, Holder<Attribute> attribute) {
		return entityType(formId)
				.map(type -> {
					AttributeSupplier supplier = DefaultAttributes.getSupplier(
							(EntityType<? extends LivingEntity>) type);
					return supplier.hasAttribute(attribute) ? (float) supplier.getBaseValue(attribute) : 0.0F;
				})
				.orElse(0.0F);
	}

	private static EntityDimensions dimensions(EntityType<? extends MythlingEntity> type) {
		return type.getDimensions().scale(MythlingEntity.formScale(type));
	}
}
