package com.ashi.mythweave.registry;

import com.ashi.mythweave.Mythweave;

import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;

import net.minecraft.core.Registry;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;

/**
 * Mythweave particle types. Registered on both sides (the server sends them by id); how each
 * one looks and moves is client-only, see {@code client/registry/ModParticleProviders}.
 */
public final class ModParticles {
	/** A tiny glowing square ember that drifts up and fades, as in the Cindercub concept art. */
	public static final SimpleParticleType EMBER = register("ember", FabricParticleTypes.simple());

	private ModParticles() {
	}

	private static SimpleParticleType register(String name, SimpleParticleType type) {
		return Registry.register(BuiltInRegistries.PARTICLE_TYPE, Mythweave.id(name), type);
	}

	/** Called from {@link Mythweave#onInitialize()}; loading the class runs the registrations. */
	public static void initialize() {
	}
}
