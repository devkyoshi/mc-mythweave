package com.ashi.mythweave.client.model.entity;

import com.ashi.mythweave.client.render.entity.state.SolflareRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Solflare, the exploration stage-3 form of the Cindercub line: a radiant guardian fox. A
 * lean body on long legs, a cream chest ruff, a narrow head with tall ears and a three-blade
 * flame crest, two fans of flame wings at the shoulders (three blades each, swept up and back)
 * and a long comet tail of fire.
 *
 * <p>Fire animation as on the Emberlynx (rippling tail, flickering tongues, all burning
 * upward); the wings breathe open and closed and sweep back when it runs. The glowing parts
 * come from {@code solflare_glow.png}.
 *
 * <p>Model space is in pixels (16 = one block), y down, ground at y = 24. Texture sheet is
 * 64 x 64; the layout is documented in {@code tools/textures/solflare.ps1}.
 */
public class SolflareModel extends EntityModel<SolflareRenderState> {
	private static final float TAIL_LIFT = 0.55F;
	/** How far each wing leans out from the body, and how far back each of its blades sweeps. */
	private static final float WING_SPREAD = 0.5F;
	private static final float[] BLADE_SWEEP = {-0.25F, -0.7F, -1.15F};

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart[] crest;
	private final ModelPart rightWing;
	private final ModelPart leftWing;
	private final ModelPart[] blades;
	private final ModelPart tail;
	private final ModelPart tailMid;
	private final ModelPart tailMid2;
	private final ModelPart tailTip;
	private final ModelPart plume;
	private final ModelPart[] tailTongues;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;

	public SolflareModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.head = root.getChild("head");
		this.rightEar = this.head.getChild("right_ear");
		this.leftEar = this.head.getChild("left_ear");
		this.crest = new ModelPart[3];
		for (int i = 0; i < 3; i++) {
			this.crest[i] = this.head.getChild("crest_" + i);
		}
		this.rightWing = this.body.getChild("right_wing");
		this.leftWing = this.body.getChild("left_wing");
		this.blades = new ModelPart[6];
		for (int i = 0; i < 3; i++) {
			this.blades[i] = this.rightWing.getChild("blade_" + i);
			this.blades[i + 3] = this.leftWing.getChild("blade_" + i);
		}
		this.tail = this.body.getChild("tail");
		this.tailMid = this.tail.getChild("mid");
		this.tailMid2 = this.tailMid.getChild("mid2");
		this.tailTip = this.tailMid2.getChild("tip");
		this.plume = this.tailTip.getChild("plume");
		this.tailTongues = new ModelPart[] {
				this.tail.getChild("tongue"), this.tailMid.getChild("tongue"), this.tailMid2.getChild("tongue")};
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		CubeListBuilder flame = CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F);

		// Body: 5 wide, 5 tall, 11 long, on long 8-pixel legs (y 11..16).
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 11).addBox(-2.5F, -2.5F, -5.5F, 5.0F, 5.0F, 11.0F),
				PartPose.offset(0.0F, 13.5F, 0.0F));
		// Cream ruff on the lower chest, a little wider than the body and in front of it.
		body.addOrReplaceChild("ruff",
				CubeListBuilder.create().texOffs(32, 11).addBox(-3.0F, -1.5F, -1.5F, 6.0F, 3.0F, 3.0F),
				PartPose.offset(0.0F, 1.0F, -5.0F));

		// Flame wings: at each shoulder a fan of three thin blades (long, middle, short), each
		// sweeping further back, the whole fan leaning out from the body.
		addWing(body, "right_wing", -2.5F, -WING_SPREAD);
		addWing(body, "left_wing", 2.5F, WING_SPREAD);

		// Comet tail: a 3x3x3 base, two 2x2x4 segments, a 2x2x3 tip and a plume.
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(38, 17).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, -1.5F, 5.5F, TAIL_LIFT, 0.0F, 0.0F));
		tail.addOrReplaceChild("tongue", flame, PartPose.offset(0.0F, -1.5F, 1.5F));
		CubeListBuilder segment = CubeListBuilder.create().texOffs(0, 27).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 4.0F);
		PartDefinition mid = tail.addOrReplaceChild("mid", segment, PartPose.offset(0.0F, 0.0F, 3.0F));
		mid.addOrReplaceChild("tongue", flame, PartPose.offset(0.0F, -1.0F, 2.0F));
		PartDefinition mid2 = mid.addOrReplaceChild("mid2", segment, PartPose.offset(0.0F, 0.0F, 4.0F));
		mid2.addOrReplaceChild("tongue", flame, PartPose.offset(0.0F, -1.0F, 2.0F));
		PartDefinition tip = mid2.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(12, 27).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F),
				PartPose.offset(0.0F, 0.0F, 4.0F));
		tip.addOrReplaceChild("plume",
				CubeListBuilder.create().texOffs(60, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offset(0.0F, -1.0F, 1.5F));

		// Head (6 x 5 x 6) over the front of the body, a narrow 3 x 2 x 3 muzzle, tall ears, and
		// a crest of three flame blades along the top, leaning back.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -5.0F, -6.0F, 6.0F, 5.0F, 6.0F),
				PartPose.offset(0.0F, 11.5F, -4.5F));
		head.addOrReplaceChild("snout",
				CubeListBuilder.create().texOffs(24, 0).addBox(-1.5F, -2.0F, -9.0F, 3.0F, 2.0F, 3.0F),
				PartPose.ZERO);
		head.addOrReplaceChild("right_ear",
				CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(-2.0F, -5.0F, -2.5F, 0.0F, 0.0F, -0.15F));
		head.addOrReplaceChild("left_ear",
				CubeListBuilder.create().texOffs(42, 0).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(2.0F, -5.0F, -2.5F, 0.0F, 0.0F, 0.15F));
		CubeListBuilder crestBig = CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F);
		CubeListBuilder crestSmall = CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F);
		head.addOrReplaceChild("crest_0", crestSmall, PartPose.offsetAndRotation(0.0F, -5.0F, -5.0F, -0.5F, 0.0F, 0.0F));
		head.addOrReplaceChild("crest_1", crestBig, PartPose.offsetAndRotation(0.0F, -5.0F, -3.5F, -0.65F, 0.0F, 0.0F));
		head.addOrReplaceChild("crest_2", crestBig, PartPose.offsetAndRotation(0.0F, -5.0F, -2.0F, -0.8F, 0.0F, 0.0F));

		// Long legs (2 x 8 x 2).
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 33).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 8.0F, 2.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-1.5F, 16.0F, 4.0F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(1.5F, 16.0F, 4.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-1.5F, 16.0F, -4.0F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(1.5F, 16.0F, -4.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	/** One flame wing: a pivot at the shoulder leaning out by {@code zRot}, with three blades. */
	private static void addWing(PartDefinition body, String name, float x, float zRot) {
		PartDefinition wing = body.addOrReplaceChild(name, CubeListBuilder.create(),
				PartPose.offsetAndRotation(x, -2.0F, -2.5F, 0.0F, 0.0F, zRot));
		int[][] blades = {{50, 11, 10}, {56, 11, 8}, {32, 17, 6}}; // u, v, length
		for (int i = 0; i < 3; i++) {
			int length = blades[i][2];
			wing.addOrReplaceChild("blade_" + i,
					CubeListBuilder.create().texOffs(blades[i][0], blades[i][1]).addBox(-0.5F, -length, -1.0F, 1.0F, length, 2.0F),
					PartPose.offsetAndRotation(0.0F, 0.0F, i * 0.8F, BLADE_SWEEP[i], 0.0F, 0.0F));
		}
	}

	@Override
	public void setupAnim(SolflareRenderState state) {
		// super.setupAnim resets every part to its default pose before we apply this frame's pose.
		super.setupAnim(state);
		float age = state.ageInTicks;

		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;

		// A long, light stride.
		float pos = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		this.rightHindLeg.xRot = Mth.cos(pos * 0.6662F) * 1.1F * speed;
		this.leftHindLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.1F * speed;
		this.rightFrontLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.1F * speed;
		this.leftFrontLeg.xRot = Mth.cos(pos * 0.6662F) * 1.1F * speed;

		// Idle: an alert head tilt and ear twitches.
		float idle = 1.0F - Math.min(1.0F, speed * 2.0F);
		this.head.zRot = Mth.sin(age * 0.05F) * 0.06F * idle;
		this.rightEar.zRot += Mth.sin(age * 0.21F) * 0.05F;
		this.leftEar.zRot -= Mth.sin(age * 0.21F + 1.3F) * 0.05F;

		this.tail.yRot = Mth.cos(age * 0.12F) * (0.12F + speed * 0.25F);

		if (state.isSitting) {
			this.setSittingPose();
		}

		this.animateFire(age, speed);
	}

	/** Wings breathe and sweep back when running; every flame flickers; the tail ripples. */
	private void animateFire(float age, float speed) {
		float f = age * (1.0F + speed * 0.5F);

		// Wings: open and close a little, slowly; fold back as it runs.
		float breathe = 0.08F * Mth.sin(age * 0.08F);
		this.rightWing.zRot = -(WING_SPREAD + breathe);
		this.leftWing.zRot = WING_SPREAD + breathe;
		this.rightWing.xRot = -speed * 0.35F;
		this.leftWing.xRot = -speed * 0.35F;
		for (int i = 0; i < this.blades.length; i++) {
			this.blades[i].yScale = 1.0F + 0.12F * Mth.sin(f * 0.9F + i * 1.3F);
		}

		// Crest: three candle flames.
		for (int i = 0; i < this.crest.length; i++) {
			this.crest[i].yScale = 1.0F + 0.25F * Mth.sin(f * 1.1F + i * 2.0F);
		}

		// Tail: a wave travels down the segments (each lags the last) while they curl up.
		this.tailMid.yRot = Mth.sin(f * 0.22F - 0.9F) * 0.25F;
		this.tailMid2.yRot = Mth.sin(f * 0.22F - 1.8F) * 0.3F;
		this.tailTip.yRot = Mth.sin(f * 0.22F - 2.7F) * 0.35F;
		this.tailMid.xRot = 0.12F + 0.06F * Mth.sin(f * 0.18F);
		this.tailMid2.xRot = 0.16F + 0.08F * Mth.sin(f * 0.18F + 0.6F);
		this.tailTip.xRot = 0.2F + 0.1F * Mth.sin(f * 0.18F + 1.2F);
		setScale(this.tail, 1.0F + 0.04F * Mth.sin(f * 0.9F));
		setScale(this.tailMid, 1.0F + 0.06F * Mth.sin(f * 1.1F + 1.0F));
		setScale(this.tailMid2, 0.95F * (1.0F + 0.07F * Mth.sin(f * 1.2F + 2.0F)));
		setScale(this.tailTip, 0.9F * (1.0F + 0.1F * Mth.sin(f * 1.4F + 3.0F)));

		float tilt = this.body.xRot + this.tail.xRot;
		ModelPart[] chain = {this.tail, this.tailMid, this.tailMid2};
		for (int i = 0; i < this.tailTongues.length; i++) {
			if (i > 0) {
				tilt += chain[i].xRot;
			}
			this.tailTongues[i].xRot = -tilt;
			this.tailTongues[i].yScale = 1.0F + 0.45F * Mth.sin(f * 1.3F + i * 2.1F);
		}
		float tipTilt = tilt + this.tailTip.xRot;
		this.plume.xRot = -tipTilt + 0.2F * Mth.sin(f * 0.5F);
		this.plume.zRot = 0.2F * Mth.cos(f * 0.43F);
		this.plume.yScale = 1.5F + 0.4F * Mth.sin(f * 1.1F) + 0.15F * Mth.sin(f * 2.3F);
	}

	private static void setScale(ModelPart part, float scale) {
		part.xScale = scale;
		part.yScale = scale;
		part.zScale = scale;
	}

	/**
	 * Sitting: the body tilts back so the chest is up and the rump is on the ground, the long
	 * hind legs fold flat, the front legs stay straight, and the tail lies back. Offsets keep
	 * the head on the chest and the haunches clear of the body's sides.
	 */
	private void setSittingPose() {
		this.body.xRot = -0.45F;
		this.body.y += 6.0F;
		this.head.y += 3.6F;
		this.head.z += 2.2F;
		this.tail.xRot = 0.15F;
		this.rightHindLeg.xRot = -1.45F;
		this.leftHindLeg.xRot = -1.45F;
		this.rightHindLeg.y += 6.0F;
		this.leftHindLeg.y += 6.0F;
		this.rightHindLeg.z -= 0.5F;
		this.leftHindLeg.z -= 0.5F;
		this.rightHindLeg.x -= 0.5F;
		this.leftHindLeg.x += 0.5F;
		this.rightFrontLeg.xRot = 0.0F;
		this.leftFrontLeg.xRot = 0.0F;
	}
}
