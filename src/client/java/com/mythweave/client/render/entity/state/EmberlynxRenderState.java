package com.mythweave.client.render.entity.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Per-frame snapshot of what the Emberlynx model needs beyond the vanilla living-entity data. */
public class EmberlynxRenderState extends LivingEntityRenderState {
	/** From {@code TamableAnimal#isInSittingPose()}, which the server syncs to clients. */
	public boolean isSitting;
}
