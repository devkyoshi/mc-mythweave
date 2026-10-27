package com.ashi.mythweave.client.render;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Per-frame snapshot of what the Cindercub model needs beyond the vanilla living-entity data. */
public class CindercubRenderState extends LivingEntityRenderState {
	/** From {@code TamableAnimal#isInSittingPose()}, which the server syncs to clients. */
	public boolean isSitting;
}
