package com.ashi.mythweave.client.model.entity;

import com.ashi.mythweave.client.render.entity.state.CindercubRenderState;

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
 * <p>Fire animation: the tail sways, ripples and curls like a whip, every segment flickers in
 * size on its own rhythm, and the flame tufts on its sides always burn upward. The head tilts
 * and the ears twitch while idle. The glowing parts (the tail, the eye shine) come from
 * {@code cindercub_glow.png}, drawn with a slow pulse by
 * {@link com.ashi.mythweave.client.render.entity.layer.PulsingGlowLayer}.
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
