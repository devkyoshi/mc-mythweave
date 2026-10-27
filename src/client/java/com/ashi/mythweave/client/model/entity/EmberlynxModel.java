package com.ashi.mythweave.client.model.entity;

import com.ashi.mythweave.client.render.entity.state.EmberlynxRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Emberlynx, stage 2 of the Cindercub line: grown up from the kit but still friendly. Taller
 * legs and a longer body, a head in proportion, a short muzzle, tall ears ending in flame
 * tufts, longer swept-back crystal horns, three spine shards, a fire mane over the shoulders,
 * and a long tail of fire (three segments and a tip).
 *
 * <p>Fire animation: the tail ripples and curls, and every flame (tail, tongues, mane, ear
 * tufts) flickers on its own rhythm; tongues and the plume are counter-rotated to burn
 * straight up. The glowing parts come from {@code emberlynx_glow.png}.
 *
 * <p>Model space is in pixels (16 = one block), y down, ground at y = 24. Texture sheet is
 * 64 x 64; the layout is documented in {@code tools/textures/emberlynx.ps1}.
 */
public class EmberlynxModel extends EntityModel<EmberlynxRenderState> {
	private static final float TAIL_LIFT = 0.5F;

	private final ModelPart body;
	private final ModelPart mane;
	private final ModelPart[] maneTongues;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart rightTuft;
	private final ModelPart leftTuft;
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

	public EmberlynxModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.mane = this.body.getChild("mane");
		this.maneTongues = new ModelPart[] {
				this.mane.getChild("tongue_left"), this.mane.getChild("tongue_middle"), this.mane.getChild("tongue_right")};
		this.head = root.getChild("head");
		this.rightEar = this.head.getChild("right_ear");
		this.leftEar = this.head.getChild("left_ear");
		this.rightTuft = this.rightEar.getChild("tuft");
		this.leftTuft = this.leftEar.getChild("tuft");
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
		CubeListBuilder flame = CubeListBuilder.create().texOffs(48, 4).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F);

		// Body: 5 wide, 5 tall, 10 long, on 6-pixel legs (y 13..18).
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 12).addBox(-2.5F, -2.5F, -5.0F, 5.0F, 5.0F, 10.0F),
				PartPose.offset(0.0F, 15.5F, 0.0F));
		// Three crystal shards along the spine, leaning back.
		CubeListBuilder shard = CubeListBuilder.create().texOffs(56, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		for (int i = 0; i < 3; i++) {
			body.addOrReplaceChild("spine_crystal_" + i, shard,
					PartPose.offsetAndRotation(0.0F, -2.5F, -1.5F + i * 2.5F, -0.4F, 0.0F, 0.0F));
		}
		// Fire mane: a collar of flame over the shoulders, with three tongues licking up.
		PartDefinition mane = body.addOrReplaceChild("mane",
				CubeListBuilder.create().texOffs(30, 12).addBox(-3.0F, -3.0F, -1.5F, 6.0F, 3.0F, 3.0F),
				PartPose.offset(0.0F, -2.0F, -3.5F));
		mane.addOrReplaceChild("tongue_left", flame, PartPose.offset(-2.0F, -3.0F, 0.0F));
		mane.addOrReplaceChild("tongue_middle", flame, PartPose.offset(0.0F, -3.0F, -0.5F));
		mane.addOrReplaceChild("tongue_right", flame, PartPose.offset(2.0F, -3.0F, 0.0F));

		// Fire tail from the back-top of the body: a 3x3x3 base, two 2x2x3 segments and a tip,
		// each the child of the one before so it ripples; a tongue on each segment, a plume on top.
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(30, 18).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, -1.5F, 5.0F, TAIL_LIFT, 0.0F, 0.0F));
		tail.addOrReplaceChild("tongue", flame, PartPose.offset(0.0F, -1.5F, 1.5F));
		CubeListBuilder segment = CubeListBuilder.create().texOffs(42, 18).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F);
		PartDefinition mid = tail.addOrReplaceChild("mid", segment, PartPose.offset(0.0F, 0.0F, 3.0F));
		mid.addOrReplaceChild("tongue", flame, PartPose.offset(0.0F, -1.0F, 1.5F));
		PartDefinition mid2 = mid.addOrReplaceChild("mid2", segment, PartPose.offset(0.0F, 0.0F, 3.0F));
		mid2.addOrReplaceChild("tongue", flame, PartPose.offset(0.0F, -1.0F, 1.5F));
		PartDefinition tip = mid2.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(52, 18).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F),
				PartPose.offset(0.0F, 0.0F, 3.0F));
		tip.addOrReplaceChild("plume",
				CubeListBuilder.create().texOffs(60, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offset(0.0F, -1.0F, 1.0F));

		// Head (7 x 5 x 6) on the front-top of the body, a short muzzle, tall ears with flame
		// tufts, and long crystal horns swept back over the head.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -5.0F, -6.0F, 7.0F, 5.0F, 6.0F),
				PartPose.offset(0.0F, 13.5F, -4.5F));
		head.addOrReplaceChild("snout",
				CubeListBuilder.create().texOffs(26, 0).addBox(-1.5F, -2.0F, -8.0F, 3.0F, 2.0F, 2.0F),
				PartPose.ZERO);
		addEar(head, "right_ear", 36, -2.5F, -0.15F);
		addEar(head, "left_ear", 42, 2.5F, 0.15F);
		CubeListBuilder horn = CubeListBuilder.create().texOffs(52, 0).addBox(-0.5F, -4.0F, -0.5F, 1.0F, 4.0F, 1.0F);
		head.addOrReplaceChild("right_horn", horn, PartPose.offsetAndRotation(-1.2F, -5.0F, -4.5F, -0.8F, 0.0F, -0.3F));
		head.addOrReplaceChild("left_horn", horn, PartPose.offsetAndRotation(1.2F, -5.0F, -4.5F, -0.8F, 0.0F, 0.3F));

		// Long legs (2 x 6 x 2), standing on the ground.
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 28).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 6.0F, 2.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-1.5F, 18.0F, 4.0F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(1.5F, 18.0F, 4.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-1.5F, 18.0F, -4.0F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(1.5F, 18.0F, -4.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	/** A tall ear (2 x 5 x 1) tilted outward by zRot, with a flame tuft on its tip. */
	private static void addEar(PartDefinition head, String name, int u, float x, float zRot) {
		PartDefinition ear = head.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(u, 0).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 5.0F, 1.0F),
				PartPose.offsetAndRotation(x, -5.0F, -2.5F, 0.0F, 0.0F, zRot));
		ear.addOrReplaceChild("tuft",
				CubeListBuilder.create().texOffs(48, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offset(0.0F, -5.0F, 0.5F));
	}

	@Override
	public void setupAnim(EmberlynxRenderState state) {
		// super.setupAnim resets every part to its default pose before we apply this frame's pose.
		super.setupAnim(state);
		float age = state.ageInTicks;

		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;

		// Quadruped walk cycle: diagonal legs swing together (a little less than the kit's,
		// since the legs are longer).
		float pos = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		this.rightHindLeg.xRot = Mth.cos(pos * 0.6662F) * 1.2F * speed;
		this.leftHindLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.2F * speed;
		this.rightFrontLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.2F * speed;
		this.leftFrontLeg.xRot = Mth.cos(pos * 0.6662F) * 1.2F * speed;

		// Idle: a calmer head tilt than the kit's, and ear twitches.
		float idle = 1.0F - Math.min(1.0F, speed * 2.0F);
		this.head.zRot = Mth.sin(age * 0.04F) * 0.07F * idle;
		this.rightEar.zRot += Mth.sin(age * 0.19F) * 0.05F;
		this.leftEar.zRot -= Mth.sin(age * 0.19F + 1.3F) * 0.05F;

		this.tail.yRot = Mth.cos(age * 0.12F) * (0.12F + speed * 0.25F);

		if (state.isSitting) {
			this.setSittingPose();
		}

		this.animateFire(age, speed);
	}

	/** Every flame flickers on its own rhythm; the tail ripples; tongues and plume burn upward. */
	private void animateFire(float age, float speed) {
		float f = age * (1.0F + speed * 0.5F); // runs make the fire dance faster

		// Tail: a wave travels down the segments (each lags the last) while they curl up.
		this.tailMid.yRot = Mth.sin(f * 0.22F - 0.9F) * 0.25F;
		this.tailMid2.yRot = Mth.sin(f * 0.22F - 1.8F) * 0.3F;
		this.tailTip.yRot = Mth.sin(f * 0.22F - 2.7F) * 0.35F;
		this.tailMid.xRot = 0.15F + 0.06F * Mth.sin(f * 0.18F);
		this.tailMid2.xRot = 0.2F + 0.08F * Mth.sin(f * 0.18F + 0.6F);
		this.tailTip.xRot = 0.25F + 0.1F * Mth.sin(f * 0.18F + 1.2F);
		setScale(this.tail, 1.0F + 0.04F * Mth.sin(f * 0.9F));
		setScale(this.tailMid, 1.0F + 0.06F * Mth.sin(f * 1.1F + 1.0F));
		setScale(this.tailMid2, 0.95F * (1.0F + 0.07F * Mth.sin(f * 1.2F + 2.0F)));
		setScale(this.tailTip, 0.85F * (1.0F + 0.1F * Mth.sin(f * 1.4F + 3.0F)));

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
		this.plume.yScale = 1.3F + 0.4F * Mth.sin(f * 1.1F) + 0.15F * Mth.sin(f * 2.3F);

		// Mane: the collar breathes, its tongues flicker and stay upright.
		setScale(this.mane, 1.0F + 0.04F * Mth.sin(f * 0.8F));
		for (int i = 0; i < this.maneTongues.length; i++) {
			this.maneTongues[i].xRot = -this.body.xRot;
			this.maneTongues[i].yScale = 1.0F + 0.5F * Mth.sin(f * 1.2F + i * 1.7F);
		}

		// Ear tufts: little candle flames.
		this.rightTuft.yScale = 1.0F + 0.35F * Mth.sin(f * 1.0F);
		this.leftTuft.yScale = 1.0F + 0.35F * Mth.sin(f * 1.0F + 2.0F);
	}

	private static void setScale(ModelPart part, float scale) {
		part.xScale = scale;
		part.yScale = scale;
		part.zScale = scale;
	}

	/**
	 * Sitting: the body tilts back so the chest is up and the rump is on the ground, the hind
	 * legs fold flat, the front legs stay straight, and the tail lies back along the ground.
	 * Offsets keep the head on the chest and the haunches clear of the body's sides.
	 */
	private void setSittingPose() {
		this.body.xRot = -0.45F;
		this.body.y += 4.0F;               // rump down to the ground
		this.head.y += 2.1F;               // follow the chest's front-top corner
		this.head.z += 1.6F;
		this.tail.xRot = 0.15F;
		this.rightHindLeg.xRot = -1.45F;   // fold forward, flat
		this.leftHindLeg.xRot = -1.45F;
		this.rightHindLeg.y += 4.0F;
		this.leftHindLeg.y += 4.0F;
		this.rightHindLeg.z -= 0.5F;
		this.leftHindLeg.z -= 0.5F;
		this.rightHindLeg.x -= 0.5F;       // just outside the body's sides, no flicker
		this.leftHindLeg.x += 0.5F;
		this.rightFrontLeg.xRot = 0.0F;
		this.leftFrontLeg.xRot = 0.0F;
	}
}
