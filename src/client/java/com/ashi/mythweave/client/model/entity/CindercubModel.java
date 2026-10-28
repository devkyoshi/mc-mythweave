package com.ashi.mythweave.client.model.entity;

import com.ashi.mythweave.client.render.entity.state.CindercubRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Cindercub, the ember fox (docs/reference/cindercub): a boxy fox kit with big dark eyes, a
 * cream muzzle, big stepped charcoal ears, chunky charcoal paws, and a huge
 * fluffy tail of glowing embers that rises up behind it, sweeping off to one side.
 *
 * <p>Fire animation: the tail sways, ripples and curls like a whip, every segment flickers in
 * size on its own rhythm, and the flame tufts on its sides always burn upward. The head tilts
 * and the ears twitch while idle. The glowing parts (the tail, the eye shine) come from
 * {@code cindercub_glow.png}, drawn with a slow pulse by
 * {@link com.ashi.mythweave.client.render.entity.layer.PulsingGlowLayer}.
 *
 * <p>Model space is in pixels (16 = one block). Y points down and the ground is at y = 24.
 * Texture sheet is 64 x 64; the layout is documented in
 * {@code tools/textures/cindercub.ps1}, which paints the matching PNG.
 */
public class CindercubModel extends EntityModel<CindercubRenderState> {
	/** How steeply the tail rises from the rump (positive xRot lifts a part pointing back). */
	private static final float TAIL_LIFT = 0.8F;
	private static final float MID_CURL = 0.3F;
	private static final float TIP_CURL = 0.4F;
	private static final float FLAME_CURL = 0.45F;
	/** The tail plume sweeps off to one side, as in the concept sheets. */
	private static final float TAIL_SWEEP = 0.35F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart tail;
	private final ModelPart tailMid;
	private final ModelPart rightTuft;
	private final ModelPart leftTuft;
	private final ModelPart tailTip;
	private final ModelPart tailFlame;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;

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
		this.tailTip = this.tailMid.getChild("tip");
		this.tailFlame = this.tailTip.getChild("flame");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Body: 6 wide, 5 tall, 8 long, on 5-pixel legs, y 14..19.
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 12).addBox(-3.0F, -2.5F, -4.0F, 6.0F, 5.0F, 8.0F),
				PartPose.offset(0.0F, 16.5F, 0.0F));

		// Head: a big box (8 x 6 x 6), y 10..16, pushed forward over the front of the body.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -3.0F, -5.0F, 8.0F, 6.0F, 6.0F),
				PartPose.offset(0.0F, 13.0F, -3.0F));
		// Short cream muzzle across the bottom of the face, sticking out one pixel.
		head.addOrReplaceChild("muzzle",
				CubeListBuilder.create().texOffs(28, 0).addBox(-2.0F, 1.0F, -6.0F, 4.0F, 2.0F, 1.0F),
				PartPose.ZERO);
		// Big charcoal ears on the outer corners of the head, stepped: a 3x3x3 base with a
		// cream-and-red inner ear, and a 2x2x2 tip set toward the outside. Each ear pivots at
		// its base so it can twitch.
		CubeListBuilder earBase = CubeListBuilder.create().texOffs(48, 0).addBox(-1.5F, -3.0F, -1.5F, 3.0F, 3.0F, 3.0F);
		CubeListBuilder earTip = CubeListBuilder.create().texOffs(56, 12).addBox(-1.0F, -2.0F, -1.0F, 2.0F, 2.0F, 2.0F);
		PartDefinition rightEar = head.addOrReplaceChild("right_ear", earBase,
				PartPose.offsetAndRotation(-2.5F, -3.0F, -2.0F, 0.0F, 0.0F, -0.1F));
		rightEar.addOrReplaceChild("tip", earTip, PartPose.offset(-0.5F, -3.0F, 0.0F));
		PartDefinition leftEar = head.addOrReplaceChild("left_ear", earBase,
				PartPose.offsetAndRotation(2.5F, -3.0F, -2.0F, 0.0F, 0.0F, 0.1F));
		leftEar.addOrReplaceChild("tip", earTip, PartPose.offset(0.5F, -3.0F, 0.0F));

		// The ember tail, a plume bigger than the head: four segments, each the child of the
		// one before so the tail can ripple like a whip. A 4x4x3 root at the rump rises,
		// swells into a fluffy 8x8x7 middle (with a flame tuft on each side), then tapers to a
		// 6x6x5 tip and a 4x4x3 flame, each curling further upward.
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(36, 12).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, TAIL_LIFT, 0.0F, 0.0F));
		PartDefinition mid = tail.addOrReplaceChild("mid",
				CubeListBuilder.create().texOffs(0, 25).addBox(-4.0F, -4.0F, 0.0F, 8.0F, 8.0F, 7.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 2.5F, MID_CURL, 0.0F, 0.0F));
		CubeListBuilder tuft = CubeListBuilder.create().texOffs(52, 12).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		mid.addOrReplaceChild("right_tuft", tuft, PartPose.offset(-4.0F, -2.0F, 3.5F));
		mid.addOrReplaceChild("left_tuft", tuft, PartPose.offset(4.0F, -2.0F, 3.5F));
		PartDefinition tip = mid.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(30, 25).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 6.5F, TIP_CURL, 0.0F, 0.0F));
		tip.addOrReplaceChild("flame",
				CubeListBuilder.create().texOffs(0, 40).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, 0.0F, 4.5F, FLAME_CURL, 0.0F, 0.0F));

		// All four legs share one texture region: a 2 x 3 x 2 leg on a chunky 3 x 2 x 3
		// charcoal paw, standing on the ground.
		CubeListBuilder leg = CubeListBuilder.create()
				.texOffs(28, 12).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F)
				.texOffs(50, 17).addBox(-1.5F, 3.0F, -1.5F, 3.0F, 2.0F, 3.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-1.75F, 19.0F, 2.5F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(1.75F, 19.0F, 2.5F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-1.75F, 19.0F, -2.5F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(1.75F, 19.0F, -2.5F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(CindercubRenderState state) {
		// super.setupAnim resets every part to its default pose before we apply this frame's pose.
		super.setupAnim(state);
		float age = state.ageInTicks;

		// Look where the entity is looking (render state angles are in degrees).
		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;

		// Standard quadruped walk cycle: diagonal legs swing together.
		float pos = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		this.rightHindLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;
		this.leftHindLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.4F * speed;
		this.rightFrontLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.4F * speed;
		this.leftFrontLeg.xRot = Mth.cos(pos * 0.6662F) * 1.4F * speed;

		// Idle cuteness, fading out while walking: a slow curious head tilt and ear twitches.
		float idle = 1.0F - Math.min(1.0F, speed * 2.0F);
		this.head.zRot = Mth.sin(age * 0.045F) * 0.12F * idle;
		this.rightEar.zRot += Mth.sin(age * 0.21F) * 0.06F;
		this.leftEar.zRot -= Mth.sin(age * 0.21F + 1.3F) * 0.06F;

		if (state.isSitting) {
			this.setSittingPose();
		}

		this.animateTail(age, speed);
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
		this.tailMid.yRot = Mth.sin(f * 0.25F - 0.9F) * 0.15F;
		this.tailTip.yRot = Mth.sin(f * 0.25F - 1.8F) * 0.25F;
		this.tailFlame.yRot = Mth.sin(f * 0.25F - 2.7F) * 0.3F;
		this.tailMid.xRot += 0.05F * Mth.sin(f * 0.2F);
		this.tailTip.xRot += 0.08F * Mth.sin(f * 0.2F + 0.7F);
		this.tailFlame.xRot += 0.12F * Mth.sin(f * 0.2F + 1.4F);

		setScale(this.tailMid, 1.0F + 0.03F * Mth.sin(f * 0.9F));
		setScale(this.tailTip, 1.0F + 0.06F * Mth.sin(f * 1.1F + 1.0F));
		setScale(this.tailFlame, 1.0F + 0.12F * Mth.sin(f * 1.4F + 2.0F));

		float midTilt = this.body.xRot + this.tail.xRot + this.tailMid.xRot;
		this.rightTuft.xRot = -midTilt;
		this.leftTuft.xRot = -midTilt;
		this.rightTuft.yScale = 1.0F + 0.4F * Mth.sin(f * 1.3F);
		this.leftTuft.yScale = 1.0F + 0.4F * Mth.sin(f * 1.3F + 2.4F);
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
		this.body.y += 3.4F;               // lower so the rump meets the ground
		this.head.y += 1.8F;               // follow the chest's front-top corner as it tilts
		this.head.z += 1.7F;
		this.tail.xRot = TAIL_LIFT + 0.7F; // relative to the tilted body: stands up behind
		this.rightHindLeg.xRot = -1.4F;    // fold forward, nearly flat
		this.leftHindLeg.xRot = -1.4F;
		this.rightHindLeg.y += 3.0F;
		this.leftHindLeg.y += 3.0F;
		// Folded legs would sit flush with the body's sides and flicker where the faces
		// overlap; half a pixel outward reads as haunches instead.
		this.rightHindLeg.x -= 0.5F;
		this.leftHindLeg.x += 0.5F;
		this.rightFrontLeg.xRot = 0.0F;    // no walk swing while sitting
		this.leftFrontLeg.xRot = 0.0F;
	}
}
