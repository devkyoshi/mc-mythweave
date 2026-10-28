package com.mythweave.client.model.entity;

import com.mythweave.client.render.entity.state.CindercubRenderState;
import com.mythweave.entity.cindercub.CindercubEntity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.MeshTransformer;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Cindercub, the ember fox (docs/reference/cindercub): a fox kit with a long body, a
 * pointed cream snout, cheek ruffs, dark eyes under a red brow, tall pointed dark ears,
 * thick legs on dark paws, and a huge bushy tail of embers swept back, up and to one
 * side.
 *
 * <p>Built at 1.5x detail to match the concept art: the mesh is modelled in units of 2/3 of a
 * pixel and {@link MeshTransformer#scaling} shrinks it back to its in-game size (the same
 * mechanism vanilla uses for baby models), so its voxels are finer than a vanilla mob's.
 * Coordinates below are in those small units. Y points down and the ground is at y = 24.
 *
 * <p>Animations, following the concept sheets: idle (breathing, head tilt, ear twitches, and
 * now and then a sniff upward or at the ground), walk, run (a gallop with ears back and the
 * tail streaming out), sit, sleep (curled up with its tail wrapped round it and eyes closed),
 * happy (hops and a fast tail wag) and hurt (a flinch). Through all of them the fire tail
 * sways, ripples and curls like a whip, every segment flickers in size on its own rhythm, and
 * the flame tufts on its sides burn upward. The glowing parts come from
 * {@code cindercub_glow.png}, drawn with a slow pulse by
 * {@link com.mythweave.client.render.entity.layer.PulsingGlowLayer}.
 *
 * <p>Texture sheet is 128 x 128; the layout is documented in
 * {@code tools/textures/cindercub.ps1}, which paints the matching PNG.
 */
public class CindercubModel extends EntityModel<CindercubRenderState> {
	/** Model units per pixel: the mesh is built 1.5x and scaled back down when baked. */
	private static final float DETAIL = 1.5F;
	/** How steeply the tail rises from the rump (positive xRot lifts a part pointing back). */
	private static final float TAIL_LIFT = 0.4F;
	private static final float MID_CURL = 0.25F;
	private static final float UPPER_CURL = 0.3F;
	private static final float TIP_CURL = 0.4F;
	private static final float FLAME_CURL = 0.45F;
	/** The tail plume sweeps off to one side, as in the concept art. */
	private static final float TAIL_SWEEP = 0.35F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart tail;
	private final ModelPart tailMid;
	private final ModelPart rightTuft;
	private final ModelPart leftTuft;
	private final ModelPart tailUpper;
	private final ModelPart tailTip;
	private final ModelPart tailFlame;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;
	private final ModelPart rightEyelid;
	private final ModelPart leftEyelid;

	public CindercubModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.rightEar = this.head.getChild("right_ear");
		this.leftEar = this.head.getChild("left_ear");
		this.tail = this.body.getChild("tail");
		this.tailMid = this.tail.getChild("mid");
		this.rightTuft = this.tailMid.getChild("right_tuft");
		this.leftTuft = this.tailMid.getChild("left_tuft");
		this.tailUpper = this.tailMid.getChild("upper");
		this.tailTip = this.tailUpper.getChild("tip");
		this.tailFlame = this.tailTip.getChild("flame");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
		this.rightEyelid = this.head.getChild("right_eyelid");
		this.leftEyelid = this.head.getChild("left_eyelid");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Body: 9 wide, 8 tall, 15 long, y 7..15, with a cream chest bib at the front-bottom.
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 20).addBox(-4.5F, -4.0F, -7.5F, 9.0F, 8.0F, 15.0F),
				PartPose.offset(0.0F, 11.0F, 0.0F));
		body.addOrReplaceChild("chest",
				CubeListBuilder.create().texOffs(38, 8).addBox(-3.0F, -1.0F, -8.5F, 6.0F, 5.0F, 2.0F),
				PartPose.ZERO);

		// Head: 10 x 8 x 9, y 2..10, set forward over the front of the body.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-5.0F, -6.0F, -7.0F, 10.0F, 8.0F, 9.0F),
				PartPose.offset(0.0F, 8.0F, -7.5F));
		// A pointed cream snout on the lower middle of the face, sticking out three units.
		head.addOrReplaceChild("snout",
				CubeListBuilder.create().texOffs(38, 0).addBox(-2.0F, -1.0F, -10.0F, 4.0F, 3.0F, 3.0F),
				PartPose.ZERO);
		// Small cheek ruffs on both sides, below and behind the eyes.
		CubeListBuilder cheek = CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, 0.0F, -1.5F, 1.0F, 3.0F, 3.0F);
		head.addOrReplaceChild("right_cheek", cheek, PartPose.offset(-5.5F, -2.0F, -4.0F));
		head.addOrReplaceChild("left_cheek", cheek, PartPose.offset(5.5F, -2.0F, -4.0F));
		// Tall pointed ears on top of the head.
		addEar(head, "right_ear", -1.0F);
		addEar(head, "left_ear", 1.0F);
		// Closed eyelids over the eyes, shown only while asleep: flat planes just in front of the face.
		CubeListBuilder eyelid = CubeListBuilder.create().texOffs(110, 8).addBox(-1.0F, -1.5F, 0.0F, 2.0F, 3.0F, 0.0F);
		head.addOrReplaceChild("right_eyelid", eyelid, PartPose.offset(-2.0F, -2.5F, -7.05F));
		head.addOrReplaceChild("left_eyelid", eyelid, PartPose.offset(2.0F, -2.5F, -7.05F));

		// The ember tail, a bushy plume bigger than the head: five segments, each the child of
		// the one before so the tail can ripple like a whip. A 5x5x4 root at the top of the
		// rump swells into an 11x11x8 middle (with a flame tuft on each side), then tapers
		// through 9x9x7 and 6x6x5 to a 3x4x3 flame, each curling further upward.
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(82, 8).addBox(-2.5F, -2.5F, 0.0F, 5.0F, 5.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, -2.5F, 7.0F, TAIL_LIFT, 0.0F, 0.0F));
		PartDefinition mid = tail.addOrReplaceChild("mid",
				CubeListBuilder.create().texOffs(48, 20).addBox(-5.5F, -5.5F, 0.0F, 11.0F, 11.0F, 8.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, MID_CURL, 0.0F, 0.0F));
		CubeListBuilder tuft = CubeListBuilder.create().texOffs(110, 0).addBox(-1.0F, -3.0F, -1.0F, 2.0F, 3.0F, 2.0F);
		mid.addOrReplaceChild("right_tuft", tuft, PartPose.offset(-5.5F, -2.0F, 4.0F));
		mid.addOrReplaceChild("left_tuft", tuft, PartPose.offset(5.5F, -2.0F, 4.0F));
		PartDefinition upper = mid.addOrReplaceChild("upper",
				CubeListBuilder.create().texOffs(86, 20).addBox(-4.5F, -4.5F, 0.0F, 9.0F, 9.0F, 7.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 7.5F, UPPER_CURL, 0.0F, 0.0F));
		PartDefinition tip = upper.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(0, 44).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 6.5F, TIP_CURL, 0.0F, 0.0F));
		tip.addOrReplaceChild("flame",
				CubeListBuilder.create().texOffs(98, 0).addBox(-1.5F, -2.0F, 0.0F, 3.0F, 4.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, FLAME_CURL, 0.0F, 0.0F));

		// All four legs share one texture region: a thick 3 x 7 x 3 leg on a chunky 4 x 3 x 4
		// dark paw, standing on the ground.
		CubeListBuilder leg = CubeListBuilder.create()
				.texOffs(54, 8).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 7.0F, 3.0F)
				.texOffs(66, 8).addBox(-2.0F, 7.0F, -2.0F, 4.0F, 3.0F, 4.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.5F, 14.0F, 5.5F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2.5F, 14.0F, 5.5F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.5F, 14.0F, -5.5F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2.5F, 14.0F, -5.5F));

		return LayerDefinition.create(mesh, 128, 128).apply(MeshTransformer.scaling(1.0F / DETAIL));
	}

	/**
	 * One tall pointed ear: three dark tiers centred on each other and tapering to a point
	 * (6x3x3, 4x3x2, 2x2x2), leaning back and tilted outward. It pivots at its base so it can
	 * twitch. {@code side} is -1 for the right ear (at -x) and 1 for the left.
	 */
	private static void addEar(PartDefinition head, String name, float side) {
		PartDefinition base = head.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(60, 0).addBox(-3.0F, -3.0F, -1.5F, 6.0F, 3.0F, 3.0F),
				PartPose.offsetAndRotation(3.0F * side, -6.0F, -3.0F, -0.15F, 0.0F, 0.25F * side));
		PartDefinition middle = base.addOrReplaceChild("middle",
				CubeListBuilder.create().texOffs(78, 0).addBox(-2.0F, -3.0F, -1.0F, 4.0F, 3.0F, 2.0F),
				PartPose.offset(0.0F, -3.0F, 0.0F));
		middle.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(90, 0).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F),
				PartPose.offset(0.0F, -3.0F, 0.0F));
	}

	@Override
	public void setupAnim(CindercubRenderState state) {
		// super.setupAnim resets every part to its default pose before we apply this frame's pose.
		super.setupAnim(state);
		float age = state.ageInTicks;
		float pos = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		float sleep = state.sleepAmount;
		// 0 walking, 1 running flat out (blends in as it speeds up).
		float run = Mth.clamp((speed - 0.55F) / 0.35F, 0.0F, 1.0F) * (1.0F - sleep);
		// 1 standing still, 0 on the move.
		float idle = 1.0F - Math.min(1.0F, speed * 2.0F);

		// Look where the entity is looking (render state angles are in degrees).
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;

		// Legs. Walking: the standard quadruped cycle, diagonal legs together. Running: a
		// gallop, front legs together and hind legs together, half a stride apart.
		float walk = Mth.cos(pos * 0.6662F) * 1.4F * speed;
		float galFront = Mth.cos(pos * 0.6662F) * 1.1F * speed;
		float galHind = Mth.cos(pos * 0.6662F + 2.4F) * 1.1F * speed;
		this.rightHindLeg.xRot = Mth.lerp(run, walk, galHind);
		this.leftHindLeg.xRot = Mth.lerp(run, -walk, galHind);
		this.rightFrontLeg.xRot = Mth.lerp(run, -walk, galFront);
		this.leftFrontLeg.xRot = Mth.lerp(run, walk, Mth.cos(pos * 0.6662F + 0.3F) * 1.1F * speed);

		// Idle: a slow curious head tilt, ear twitches, breathing, and now and then a sniff of
		// the air (look up) or of the ground (look down), each Cindercub on its own schedule.
		this.head.zRot = Mth.sin(age * 0.045F) * 0.12F * idle;
		this.rightEar.zRot += Mth.sin(age * 0.21F) * 0.06F;
		this.leftEar.zRot -= Mth.sin(age * 0.21F + 1.3F) * 0.06F;
		float breath = Mth.sin(age * 0.08F) * 0.2F;
		this.body.y += breath;
		this.head.y += breath * 0.6F;
		float cycle = (age + state.idleSeed * 53) % 360.0F;
		if (cycle < 50.0F) {
			this.head.xRot -= 0.55F * Mth.sin(cycle / 50.0F * Mth.PI) * idle;           // look up
		} else if (cycle >= 180.0F && cycle < 230.0F) {
			this.head.xRot += 0.5F * Mth.sin((cycle - 180.0F) / 50.0F * Mth.PI) * idle; // look down
		}

		if (state.isSitting) {
			this.setSittingPose();
		}

		// Running: the body pitches with each bound, the head stretches forward and low, the
		// ears lie back and the tail streams out behind.
		if (run > 0.0F) {
			this.body.xRot += Mth.sin(pos * 0.6662F + 1.2F) * 0.12F * run;
			this.body.y -= Math.abs(Mth.sin(pos * 0.6662F)) * 0.6F * run;
			this.head.xRot += 0.2F * run;
			this.head.y += 0.6F * run;
			this.rightEar.xRot -= 0.5F * run;
			this.leftEar.xRot -= 0.5F * run;
			this.tail.xRot -= 0.3F * run;
		}

		// Hurt: a flinch. The head jerks back, the ears flatten and the tail tucks down.
		float hurt = state.hurtTime / 10.0F;
		if (hurt > 0.0F) {
			this.head.xRot -= 0.35F * hurt;
			this.head.z += 1.2F * hurt;
			this.rightEar.xRot -= 0.6F * hurt;
			this.leftEar.xRot -= 0.6F * hurt;
			this.tail.xRot -= 0.35F * hurt;
		}

		this.animateTail(age, speed);

		if (state.happyTime >= 0.0F) {
			this.animateHappy(state.happyTime, state.isSitting);
		}
		if (sleep > 0.0F) {
			this.blendSleep(sleep, age);
		}
		boolean eyesClosed = sleep > 0.6F;
		this.rightEyelid.visible = eyesClosed;
		this.leftEyelid.visible = eyesClosed;
	}

	/**
	 * Tail animation. The whole tail sways side to side, livelier while walking; a travelling
	 * wave ripples down the segments (each lags the one before) while they breathe in their
	 * curl; every segment flickers in scale on its own rhythm so the fire never pulses in sync;
	 * and the side tufts are counter-rotated so they always burn straight up.
	 */
	private void animateTail(float age, float speed) {
		float f = age * (1.0F + speed * 0.5F); // a running Cindercub's fire dances faster

		this.tail.yRot = TAIL_SWEEP + Mth.cos(age * 0.15F) * (0.12F + speed * 0.25F);
		this.tailMid.yRot = Mth.sin(f * 0.25F - 0.9F) * 0.1F;
		this.tailUpper.yRot = Mth.sin(f * 0.25F - 1.5F) * 0.15F;
		this.tailTip.yRot = Mth.sin(f * 0.25F - 2.1F) * 0.22F;
		this.tailFlame.yRot = Mth.sin(f * 0.25F - 2.7F) * 0.3F;
		this.tailMid.xRot += 0.04F * Mth.sin(f * 0.2F);
		this.tailUpper.xRot += 0.06F * Mth.sin(f * 0.2F + 0.5F);
		this.tailTip.xRot += 0.08F * Mth.sin(f * 0.2F + 1.0F);
		this.tailFlame.xRot += 0.12F * Mth.sin(f * 0.2F + 1.5F);

		setScale(this.tailMid, 1.0F + 0.02F * Mth.sin(f * 0.9F));
		setScale(this.tailUpper, 1.0F + 0.04F * Mth.sin(f * 1.0F + 0.6F));
		setScale(this.tailTip, 1.0F + 0.06F * Mth.sin(f * 1.1F + 1.2F));
		setScale(this.tailFlame, 1.0F + 0.12F * Mth.sin(f * 1.4F + 2.0F));

		float midTilt = this.body.xRot + this.tail.xRot + this.tailMid.xRot;
		this.rightTuft.xRot = -midTilt;
		this.leftTuft.xRot = -midTilt;
		this.rightTuft.yScale = 1.0F + 0.4F * Mth.sin(f * 1.3F);
		this.leftTuft.yScale = 1.0F + 0.4F * Mth.sin(f * 1.3F + 2.4F);
	}

	/**
	 * Happy: three little hops (just a bounce of the chest when sitting), a fast tail wag, ears
	 * perked up and a bobbing head tilt. {@code t} is ticks into the animation.
	 */
	private void animateHappy(float t, boolean sitting) {
		float env = Math.min(1.0F, t / 4.0F) * Math.min(1.0F, (CindercubEntity.HAPPY_TICKS - t) / 6.0F);
		if (env <= 0.0F) {
			return;
		}
		float hop = Math.abs(Mth.sin(t * Mth.PI / 10.0F)) * env;
		if (sitting) {
			this.body.y -= hop * 0.8F;
			this.head.y -= hop * 1.2F;
		} else {
			float lift = hop * 3.0F;
			this.body.y -= lift;
			this.head.y -= lift;
			for (ModelPart leg : new ModelPart[] {this.rightHindLeg, this.leftHindLeg, this.rightFrontLeg, this.leftFrontLeg}) {
				leg.y -= lift;
			}
			// Front paws tuck up mid-hop.
			this.rightFrontLeg.xRot -= hop * 0.5F;
			this.leftFrontLeg.xRot -= hop * 0.5F;
		}
		this.tail.yRot += Mth.sin(t * 1.3F) * 0.55F * env;
		this.tailTip.yRot += Mth.sin(t * 1.3F - 0.8F) * 0.3F * env;
		this.rightEar.zRot += 0.2F * env;
		this.leftEar.zRot -= 0.2F * env;
		this.head.zRot += Mth.sin(t * 0.4F) * 0.25F * env;
		this.head.xRot -= 0.15F * env;
	}

	/**
	 * Sleep: from the sitting pose it settles into a curl on the ground: body lying down, legs
	 * folded under, head resting on its front paws and turned toward its tail, which wraps
	 * round the body, ears relaxed, breathing slowly. {@code s} blends it in (0..1).
	 */
	private void blendSleep(float s, float age) {
		float breath = Mth.sin(age * 0.05F) * 0.3F;
		lerpPose(this.body, s, this.body.x, 20.0F + breath, this.body.z, 0.0F, 0.0F, 0.0F);
		lerpPose(this.head, s, 0.0F, 17.0F + breath * 0.5F, -8.5F, 0.2F, -0.5F, -0.2F);
		for (ModelPart leg : new ModelPart[] {this.rightFrontLeg, this.leftFrontLeg, this.rightHindLeg, this.leftHindLeg}) {
			lerpPose(leg, s, leg.x, 21.5F, leg.z, -1.45F, 0.0F, 0.0F);
		}
		this.tail.xRot = Mth.lerp(s, this.tail.xRot, -0.05F);
		this.tail.yRot = Mth.lerp(s, this.tail.yRot, 1.9F);
		this.tailMid.xRot = Mth.lerp(s, this.tailMid.xRot, 0.0F);
		this.tailMid.yRot = Mth.lerp(s, this.tailMid.yRot, 0.5F);
		this.tailUpper.xRot = Mth.lerp(s, this.tailUpper.xRot, 0.05F);
		this.tailUpper.yRot = Mth.lerp(s, this.tailUpper.yRot, 0.5F);
		this.tailTip.xRot = Mth.lerp(s, this.tailTip.xRot, 0.1F);
		this.tailTip.yRot = Mth.lerp(s, this.tailTip.yRot, 0.4F);
		this.rightEar.xRot = Mth.lerp(s, this.rightEar.xRot, -0.45F);
		this.leftEar.xRot = Mth.lerp(s, this.leftEar.xRot, -0.45F);
		this.rightEar.zRot = Mth.lerp(s, this.rightEar.zRot, -0.5F);
		this.leftEar.zRot = Mth.lerp(s, this.leftEar.zRot, 0.5F);
	}

	private static void lerpPose(ModelPart part, float s, float x, float y, float z, float xRot, float yRot, float zRot) {
		part.x = Mth.lerp(s, part.x, x);
		part.y = Mth.lerp(s, part.y, y);
		part.z = Mth.lerp(s, part.z, z);
		part.xRot = Mth.lerp(s, part.xRot, xRot);
		part.yRot = Mth.lerp(s, part.yRot, yRot);
		part.zRot = Mth.lerp(s, part.zRot, zRot);
	}

	private static void setScale(ModelPart part, float scale) {
		part.xScale = scale;
		part.yScale = scale;
		part.zScale = scale;
	}

	/**
	 * Sitting: the body tilts back so the chest is up and the rump touches the ground, the
	 * hind legs fold forward flat, the front legs stay straight, and the tail stands up
	 * behind. Offsets were worked out from the rest pose so the parts stay connected.
	 */
	private void setSittingPose() {
		this.body.xRot = -0.5F;            // tilt: front up, back down
		this.body.y += 5.9F;               // lower so the rump meets the ground
		this.head.y += 2.8F;               // follow the chest's front-top corner as it tilts
		this.head.z += 2.8F;
		this.tail.xRot = TAIL_LIFT + 0.7F; // relative to the tilted body: stands up behind
		this.rightHindLeg.xRot = -1.4F;    // fold forward, nearly flat
		this.leftHindLeg.xRot = -1.4F;
		this.rightHindLeg.y += 6.5F;
		this.leftHindLeg.y += 6.5F;
		// Folded paws would sit flush with the body's sides and flicker where the faces
		// overlap; pushing them out a little reads as haunches instead.
		this.rightHindLeg.x -= 0.75F;
		this.leftHindLeg.x += 0.75F;
		this.rightFrontLeg.xRot = 0.0F;    // no walk swing while sitting
		this.leftFrontLeg.xRot = 0.0F;
	}
}
