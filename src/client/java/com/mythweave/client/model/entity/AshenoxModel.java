package com.mythweave.client.model.entity;

import com.mythweave.client.render.entity.state.AshenoxRenderState;
import com.mythweave.entity.ashenox.AshenoxEntity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;

/**
 * Ashenox, stage 2 of the Cindercub line: a horse-sized fire fox you can ride, generated from
 * its design sheet (docs/reference/ashenox) by {@code tools/models/ashenox_gen.ps1}. The mesh
 * ({@link AshenoxModelGeometry}) is carved from the sheet's side and front silhouettes at 2.5x
 * voxel detail and painted with the sheet's own colours; the three flame-plume tails are chains
 * of tapering segments (centre, left, right), each with its own pivot and rhythm.
 *
 * <p>Parts: body (with the chest ruff, the head with its ears and sleep eyelids, and the three
 * tails) and four two-part legs hanging from the root, so the feet stay planted when the body
 * moves. About 1.6 blocks to the ear tips, 2 blocks from nose to tail tips.
 *
 * <p>Animated in code from {@link AshenoxRenderState}: idle breathing and tail sway, a walk
 * (diagonal pairs) that blends into a bounding run, sitting, lying down asleep, looking around,
 * a happy bounce, an alert stance, and a steadier carriage while ridden. Positions here are in
 * model units ({@link #U} per pixel), because the mesh is scaled down only at its root.
 */
public class AshenoxModel extends EntityModel<AshenoxRenderState> {
	/** Model units per pixel. */
	private static final float U = AshenoxModelGeometry.DETAIL;
	/** Each tail's own sway rhythm (centre, left, right), so the three never move as one. */
	private static final float[] TAIL_PHASE = {0.0F, 2.1F, 4.2F};
	private static final float[] TAIL_SPEED = {0.08F, 0.067F, 0.094F};

	private final ModelPart body;
	private final ModelPart chest;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart eyelids;
	/** Centre, left, right; each is the tail's segments from the rump out, then its flame. */
	private final ModelPart[][] tails;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLower;
	private final ModelPart leftFrontLower;
	private final ModelPart rightHindLower;
	private final ModelPart leftHindLower;

	public AshenoxModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.chest = this.body.getChild("chest");
		this.head = this.body.getChild("head");
		this.rightEar = this.head.getChild("right_ear");
		this.leftEar = this.head.getChild("left_ear");
		this.eyelids = this.head.getChild("eyelids");
		String[] names = {"tail_center", "tail_left", "tail_right"};
		int segments = AshenoxModelGeometry.TAIL_SEGMENTS;
		this.tails = new ModelPart[3][segments + 1];
		for (int i = 0; i < 3; i++) {
			ModelPart part = this.body.getChild(names[i]);
			this.tails[i][0] = part;
			for (int s = 1; s < segments; s++) {
				part = part.getChild("seg" + s);
				this.tails[i][s] = part;
			}
			this.tails[i][segments] = part.getChild("flame");
		}
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightFrontLower = this.rightFrontLeg.getChild("lower");
		this.leftFrontLower = this.leftFrontLeg.getChild("lower");
		this.rightHindLower = this.rightHindLeg.getChild("lower");
		this.leftHindLower = this.leftHindLeg.getChild("lower");
	}

	public static LayerDefinition createBodyLayer() {
		return AshenoxModelGeometry.createBodyLayer();
	}

	@Override
	public void setupAnim(AshenoxRenderState state) {
		// super.setupAnim resets every part to its default pose before we apply this frame's pose.
		super.setupAnim(state);
		float age = state.ageInTicks;
		float sleep = state.sleepAmount;
		float awake = 1.0F - sleep;
		float alert = state.alertAmount * awake;
		float speed = state.isSitting ? 0.0F : Math.min(1.0F, state.walkAnimationSpeed);
		float run = state.isRunning ? speed : 0.0F;
		float idle = 1.0F - Math.min(1.0F, speed * 2.0F);
		float happy = state.happyTime >= 0.0F ? Mth.sin(state.happyTime / AshenoxEntity.HAPPY_TICKS * Mth.PI) : 0.0F;

		this.eyelids.visible = sleep > 0.5F;

		if (state.isSitting) {
			this.setSittingPose();
		}
		if (sleep > 0.0F) {
			this.blendSleepingPose(sleep);
		}
		if (!state.isSitting) {
			this.animateLegs(state.walkAnimationPos, speed, run);
		}

		// Ridden: a steady, level carriage with the head a little lower, and the tails swept
		// back so they stay clear of the rider.
		if (state.isRidden) {
			this.head.xRot += 0.12F;
			this.head.y += 0.5F * U;
			this.tails[0][0].xRot -= 0.25F;
			this.tails[1][0].xRot -= 0.1F;
			this.tails[2][0].xRot -= 0.1F;
		}

		// Alert: head raised, ears pricked forward and upright, tails lifted.
		this.head.xRot -= 0.3F * alert;
		this.head.y -= 1.0F * U * alert;
		this.rightEar.xRot -= 0.2F * alert;
		this.leftEar.xRot -= 0.2F * alert;
		for (ModelPart[] tail : this.tails) {
			tail[0].xRot += 0.25F * alert;
		}

		// Idle breathing: the body rises and falls slowly, the ruff swells. Not while ridden.
		if (!state.isRidden) {
			float breath = Mth.sin(age * 0.08F);
			this.body.y += breath * 0.3F * U * idle;
			this.chest.zScale = 1.0F + 0.04F * breath * idle;
		}

		// Happy: a bouncy little trot in place and a quick head tilt.
		if (happy > 0.0F) {
			float t = state.happyTime;
			this.body.y -= Math.abs(Mth.sin(t * 0.6F)) * 1.5F * U * happy;
			this.head.zRot += Mth.sin(t * 0.5F) * 0.2F * happy;
			this.rightFrontLeg.xRot -= Math.max(0.0F, Mth.sin(t * 0.6F)) * 0.5F * happy;
			this.leftFrontLeg.xRot -= Math.max(0.0F, -Mth.sin(t * 0.6F)) * 0.5F * happy;
		}

		this.animateHead(state, idle, alert, awake);
		this.animateTails(age, speed, run, happy, state.happyTime, awake);
		this.animateEars(age, awake);
	}

	/** Walk: diagonal pairs with bending knees. Run: front and hind pairs bound together. */
	private void animateLegs(float pos, float speed, float run) {
		float walk = 1.0F - run;
		float w = pos * 0.6662F;
		float r = pos * 0.5F;

		this.rightHindLeg.xRot = walk * Mth.cos(w) * 0.8F * speed + run * Mth.cos(r + Mth.PI) * 1.0F * speed;
		this.leftHindLeg.xRot = walk * Mth.cos(w + Mth.PI) * 0.8F * speed + run * Mth.cos(r + Mth.PI + 0.4F) * 1.0F * speed;
		this.rightFrontLeg.xRot = walk * Mth.cos(w + Mth.PI) * 0.8F * speed + run * Mth.cos(r) * 1.0F * speed;
		this.leftFrontLeg.xRot = walk * Mth.cos(w) * 0.8F * speed + run * Mth.cos(r + 0.4F) * 1.0F * speed;

		// Knees bend while a leg swings forward: front knees fold back, hind hocks forward.
		this.rightFrontLower.xRot = Math.max(0.0F, Mth.sin(w + Mth.PI)) * 0.7F * speed;
		this.leftFrontLower.xRot = Math.max(0.0F, Mth.sin(w)) * 0.7F * speed;
		this.rightHindLower.xRot = -Math.max(0.0F, Mth.sin(w)) * 0.6F * speed;
		this.leftHindLower.xRot = -Math.max(0.0F, Mth.sin(w + Mth.PI)) * 0.6F * speed;

		// Running: the body rocks and bobs with each bound, and the head reaches forward.
		this.body.xRot += Mth.sin(r) * 0.08F * run;
		this.body.y -= Math.abs(Mth.cos(r)) * 0.8F * U * run;
		this.head.xRot += 0.15F * run;
	}

	/**
	 * Looking: the head follows where the entity looks (relative to the body), with a slow
	 * scan while idle and the ears turning the same way.
	 */
	private void animateHead(AshenoxRenderState state, float idle, float alert, float awake) {
		float scan = state.isRidden ? 0.0F : Mth.sin(state.ageInTicks * 0.021F) * 0.18F * idle * (1.0F - alert);
		float yaw = (state.yRot * Mth.DEG_TO_RAD + scan) * awake;
		this.head.yRot += yaw;
		this.head.xRot += state.xRot * Mth.DEG_TO_RAD * awake;
		this.rightEar.yRot += yaw * 0.3F;
		this.leftEar.yRot += yaw * 0.3F;
	}

	/**
	 * Each tail sways on its own rhythm, rippling from the rump to the flame; faster when
	 * moving or happy. Running streams them back. The flames flicker.
	 */
	private void animateTails(float age, float speed, float run, float happy, float happyTime, float awake) {
		float energy = (0.05F + 0.05F * speed) * (0.35F + 0.65F * awake);
		for (int i = 0; i < 3; i++) {
			ModelPart[] tail = this.tails[i];
			float phase = TAIL_PHASE[i];
			float f = age * TAIL_SPEED[i] * (1.0F + speed);
			tail[0].yRot += Mth.sin(f + phase) * energy * 2.0F;
			tail[0].xRot += Mth.sin(f * 0.8F + phase) * 0.05F - 0.35F * run;
			for (int s = 1; s < tail.length; s++) {
				tail[s].yRot += Mth.sin(f - 0.5F * s + phase) * energy;
				tail[s].xRot += Mth.sin(f * 1.3F - 0.4F * s + phase) * 0.02F;
			}
			if (happy > 0.0F) {
				tail[0].yRot += Mth.sin(happyTime * 1.2F + phase) * 0.35F * happy;
			}
			// Flame flicker: the flame tufts pulse in size on their own beat.
			ModelPart flame = tail[tail.length - 1];
			flame.yScale = 1.0F + 0.2F * Mth.sin(age * 0.6F + phase);
			flame.xScale = 1.0F + 0.12F * Mth.sin(age * 0.75F + phase + 1.0F);
			flame.zScale = 1.0F + 0.15F * Mth.sin(age * 0.5F + phase + 2.0F);
		}
	}

	/** Ear flicks now and then (unless asleep). */
	private void animateEars(float age, float awake) {
		this.rightEar.zRot -= Math.max(0.0F, Mth.sin(age * 0.11F) - 0.9F) * 2.5F * awake;
		this.leftEar.zRot += Math.max(0.0F, Mth.sin(age * 0.11F + 2.3F) - 0.9F) * 2.5F * awake;
	}

	/**
	 * Sitting: the chest up and the rump on the ground, hind legs folded flat, front legs
	 * straight, the head kept level, the ruff hanging straight, and the side tails curled
	 * around both sides so all three stay visible.
	 */
	private void setSittingPose() {
		this.body.y += 4.0F * U;
		this.body.xRot = -0.5F;
		this.head.xRot = 0.5F;
		this.chest.xRot = 0.5F;
		this.tails[0][0].xRot = 0.55F;
		this.tails[1][0].xRot = 0.3F;
		this.tails[1][0].yRot = 1.3F;
		this.tails[2][0].xRot = 0.3F;
		this.tails[2][0].yRot = -1.3F;
		for (ModelPart leg : new ModelPart[] {this.rightHindLeg, this.leftHindLeg}) {
			leg.y += 5.0F * U;
			leg.z -= 1.5F * U;
			leg.xRot = -1.5F;
		}
		this.rightHindLeg.x -= 0.5F * U;
		this.leftHindLeg.x += 0.5F * U;
		this.rightHindLower.xRot = 1.5F;
		this.leftHindLower.xRot = 1.5F;
	}

	/**
	 * Asleep (only ever from sitting): lying flat with the front legs stretched forward, the
	 * head resting on them, the ears drooping, and the side tails wrapped around the body.
	 * Offsets are from the rest pose (the defaults come from the generated mesh).
	 */
	private void blendSleepingPose(float s) {
		float bodyY = this.body.getInitialPose().y() + 6.5F * U;
		this.body.y = Mth.lerp(s, this.body.y, bodyY);
		this.body.xRot = Mth.lerp(s, this.body.xRot, 0.0F);
		this.chest.xRot = Mth.lerp(s, this.chest.xRot, 0.0F);
		this.head.xRot = Mth.lerp(s, this.head.xRot, 0.2F);
		this.head.y = Mth.lerp(s, this.head.y, this.head.getInitialPose().y() + 2.0F * U);
		this.rightEar.zRot = Mth.lerp(s, this.rightEar.zRot, -0.6F);
		this.leftEar.zRot = Mth.lerp(s, this.leftEar.zRot, 0.6F);
		this.tails[0][0].xRot = Mth.lerp(s, this.tails[0][0].xRot, -0.1F);
		this.tails[1][0].xRot = Mth.lerp(s, this.tails[1][0].xRot, -0.15F);
		this.tails[1][0].yRot = Mth.lerp(s, this.tails[1][0].yRot, 1.9F);
		this.tails[2][0].xRot = Mth.lerp(s, this.tails[2][0].xRot, -0.15F);
		this.tails[2][0].yRot = Mth.lerp(s, this.tails[2][0].yRot, -1.9F);
		for (ModelPart leg : new ModelPart[] {this.rightFrontLeg, this.leftFrontLeg}) {
			leg.y = Mth.lerp(s, leg.y, leg.getInitialPose().y() + 6.5F * U);
			leg.z = Mth.lerp(s, leg.z, leg.getInitialPose().z() - 1.0F * U);
			leg.xRot = Mth.lerp(s, leg.xRot, -1.5F);
		}
		for (ModelPart leg : new ModelPart[] {this.rightHindLeg, this.leftHindLeg}) {
			leg.y = Mth.lerp(s, leg.y, leg.getInitialPose().y() + 6.5F * U);
		}
	}
}
