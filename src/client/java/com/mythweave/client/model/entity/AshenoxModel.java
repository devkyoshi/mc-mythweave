package com.mythweave.client.model.entity;

import com.mythweave.client.render.entity.state.AshenoxRenderState;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Ashenox, the combat stage-3 form of the Cindercub line: a heavy volcanic fox. A broad body
 * on thick legs, two obsidian plates across the back (cracked with glowing magma in the
 * texture), a magma mane, a strong jaw, small swept ears, big two-part horns that sweep out and curve back,
 * and a thick tail of fire.
 *
 * <p>Fire animation as on the Emberlynx (rippling tail, flickering tongues and mane, all
 * burning upward), slower and heavier. The glowing parts come from {@code ashenox_glow.png}.
 *
 * <p>Model space is in pixels (16 = one block), y down, ground at y = 24. Texture sheet is
 * 64 x 64; the layout is documented in {@code tools/textures/ashenox.ps1}.
 */
public class AshenoxModel extends EntityModel<AshenoxRenderState> {
	private static final float TAIL_LIFT = 0.35F;

	private final ModelPart body;
	private final ModelPart mane;
	private final ModelPart[] maneTongues;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
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

	public AshenoxModel(ModelPart root) {
		super(root);
		this.body = root.getChild("body");
		this.mane = this.body.getChild("mane");
		this.maneTongues = new ModelPart[4];
		for (int i = 0; i < 4; i++) {
			this.maneTongues[i] = this.mane.getChild("tongue_" + i);
		}
		this.head = root.getChild("head");
		this.rightEar = this.head.getChild("right_ear");
		this.leftEar = this.head.getChild("left_ear");
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
		CubeListBuilder flame = CubeListBuilder.create().texOffs(32, 6).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F);

		// Body: 7 wide, 6 tall, 12 long, on 6-pixel legs (y 12..18).
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 13).addBox(-3.5F, -3.0F, -6.0F, 7.0F, 6.0F, 12.0F),
				PartPose.offset(0.0F, 15.0F, 0.0F));
		// Two obsidian plates across the back, half a pixel wider than the body on each side
		// (so their sides never sit flush with the body's), tilted slightly like scales.
		CubeListBuilder plate = CubeListBuilder.create().texOffs(38, 13).addBox(-4.0F, -2.0F, -2.0F, 8.0F, 2.0F, 4.0F);
		body.addOrReplaceChild("plate_front", plate, PartPose.offsetAndRotation(0.0F, -2.5F, -0.5F, -0.12F, 0.0F, 0.0F));
		body.addOrReplaceChild("plate_back", plate, PartPose.offsetAndRotation(0.0F, -2.5F, 3.5F, -0.12F, 0.0F, 0.0F));
		// Magma mane over the shoulders with four tongues of flame.
		PartDefinition mane = body.addOrReplaceChild("mane",
				CubeListBuilder.create().texOffs(38, 19).addBox(-4.0F, -3.0F, -2.0F, 8.0F, 4.0F, 4.0F),
				PartPose.offset(0.0F, -3.0F, -4.5F));
		for (int i = 0; i < 4; i++) {
			mane.addOrReplaceChild("tongue_" + i, flame, PartPose.offset(-3.0F + i * 2.0F, -3.0F, i % 2 == 0 ? 0.0F : -0.5F));
		}

		// Thick fire tail: 4x4x4 base, a 3x3x4 and a 3x3x3 segment, a 2x2x2 tip and a plume.
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(0, 31).addBox(-2.0F, -2.0F, 0.0F, 4.0F, 4.0F, 4.0F),
				PartPose.offsetAndRotation(0.0F, -2.0F, 6.0F, TAIL_LIFT, 0.0F, 0.0F));
		tail.addOrReplaceChild("tongue", flame, PartPose.offset(0.0F, -2.0F, 2.0F));
		PartDefinition mid = tail.addOrReplaceChild("mid",
				CubeListBuilder.create().texOffs(16, 31).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 4.0F),
				PartPose.offset(0.0F, 0.0F, 4.0F));
		mid.addOrReplaceChild("tongue", flame, PartPose.offset(0.0F, -1.5F, 2.0F));
		PartDefinition mid2 = mid.addOrReplaceChild("mid2",
				CubeListBuilder.create().texOffs(30, 31).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 3.0F),
				PartPose.offset(0.0F, 0.0F, 4.0F));
		mid2.addOrReplaceChild("tongue", flame, PartPose.offset(0.0F, -1.5F, 1.5F));
		PartDefinition tip = mid2.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(42, 31).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F),
				PartPose.offset(0.0F, 0.0F, 3.0F));
		tip.addOrReplaceChild("plume",
				CubeListBuilder.create().texOffs(36, 6).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offset(0.0F, -1.0F, 1.0F));

		// Head (7 x 6 x 7) just in front of the body, a strong 4 x 3 x 3 jaw, small ears swept
		// out, and big horns: a 2x4x2 base curving into a 1x3x1 tip.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -6.0F, -7.0F, 7.0F, 6.0F, 7.0F),
				PartPose.offset(0.0F, 13.0F, -6.0F));
		head.addOrReplaceChild("snout",
				CubeListBuilder.create().texOffs(28, 0).addBox(-2.0F, -3.0F, -10.0F, 4.0F, 3.0F, 3.0F),
				PartPose.ZERO);
		head.addOrReplaceChild("right_ear",
				CubeListBuilder.create().texOffs(42, 0).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(-2.5F, -6.0F, -2.0F, 0.0F, 0.0F, -0.3F));
		head.addOrReplaceChild("left_ear",
				CubeListBuilder.create().texOffs(48, 0).addBox(-1.0F, -3.0F, 0.0F, 2.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(2.5F, -6.0F, -2.0F, 0.0F, 0.0F, 0.3F));
		addHorn(head, "right_horn", -2.0F, -0.55F);
		addHorn(head, "left_horn", 2.0F, 0.55F);

		// Thick legs (3 x 6 x 3).
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 39).addBox(-1.5F, 0.0F, -1.5F, 3.0F, 6.0F, 3.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-2.0F, 18.0F, 4.0F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(2.0F, 18.0F, 4.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-2.0F, 18.0F, -4.0F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(2.0F, 18.0F, -4.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	/** A big horn: a 2x4x2 base angled out and a little back, curving back into a 1x3x1 tip. */
	private static void addHorn(PartDefinition head, String name, float x, float zRot) {
		PartDefinition horn = head.addOrReplaceChild(name,
				CubeListBuilder.create().texOffs(54, 0).addBox(-1.0F, -4.0F, -1.0F, 2.0F, 4.0F, 2.0F),
				PartPose.offsetAndRotation(x, -6.0F, -4.5F, -0.25F, 0.0F, zRot));
		horn.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(28, 6).addBox(-0.5F, -3.0F, -0.5F, 1.0F, 3.0F, 1.0F),
				PartPose.offsetAndRotation(0.0F, -4.0F, 0.0F, -0.7F, 0.0F, 0.0F));
	}

	@Override
	public void setupAnim(AshenoxRenderState state) {
		// super.setupAnim resets every part to its default pose before we apply this frame's pose.
		super.setupAnim(state);
		float age = state.ageInTicks;

		this.head.xRot = state.xRot * Mth.DEG_TO_RAD;
		this.head.yRot = state.yRot * Mth.DEG_TO_RAD;

		// A heavier walk: shorter leg swing.
		float pos = state.walkAnimationPos;
		float speed = state.walkAnimationSpeed;
		this.rightHindLeg.xRot = Mth.cos(pos * 0.6662F) * 1.0F * speed;
		this.leftHindLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.0F * speed;
		this.rightFrontLeg.xRot = Mth.cos(pos * 0.6662F + Mth.PI) * 1.0F * speed;
		this.leftFrontLeg.xRot = Mth.cos(pos * 0.6662F) * 1.0F * speed;

		// Idle: a slow, slight head tilt and ear flicks.
		float idle = 1.0F - Math.min(1.0F, speed * 2.0F);
		this.head.zRot = Mth.sin(age * 0.03F) * 0.05F * idle;
		this.rightEar.zRot += Mth.sin(age * 0.17F) * 0.05F;
		this.leftEar.zRot -= Mth.sin(age * 0.17F + 1.3F) * 0.05F;

		this.tail.yRot = Mth.cos(age * 0.1F) * (0.1F + speed * 0.2F);

		if (state.isSitting) {
			this.setSittingPose();
		}

		this.animateFire(age, speed);
	}

	/** Every flame flickers on its own rhythm; the tail ripples; tongues and plume burn upward. */
	private void animateFire(float age, float speed) {
		float f = age * (0.9F + speed * 0.5F);

		this.tailMid.yRot = Mth.sin(f * 0.2F - 0.9F) * 0.22F;
		this.tailMid2.yRot = Mth.sin(f * 0.2F - 1.8F) * 0.27F;
		this.tailTip.yRot = Mth.sin(f * 0.2F - 2.7F) * 0.32F;
		this.tailMid.xRot = 0.15F + 0.05F * Mth.sin(f * 0.16F);
		this.tailMid2.xRot = 0.2F + 0.07F * Mth.sin(f * 0.16F + 0.6F);
		this.tailTip.xRot = 0.25F + 0.09F * Mth.sin(f * 0.16F + 1.2F);
		setScale(this.tail, 1.0F + 0.03F * Mth.sin(f * 0.8F));
		setScale(this.tailMid, 1.0F + 0.05F * Mth.sin(f * 1.0F + 1.0F));
		setScale(this.tailMid2, 1.0F + 0.06F * Mth.sin(f * 1.1F + 2.0F));
		setScale(this.tailTip, 0.9F * (1.0F + 0.1F * Mth.sin(f * 1.3F + 3.0F)));

		float tilt = this.body.xRot + this.tail.xRot;
		ModelPart[] chain = {this.tail, this.tailMid, this.tailMid2};
		for (int i = 0; i < this.tailTongues.length; i++) {
			if (i > 0) {
				tilt += chain[i].xRot;
			}
			this.tailTongues[i].xRot = -tilt;
			this.tailTongues[i].yScale = 1.1F + 0.45F * Mth.sin(f * 1.2F + i * 2.1F);
		}
		float tipTilt = tilt + this.tailTip.xRot;
		this.plume.xRot = -tipTilt + 0.15F * Mth.sin(f * 0.5F);
		this.plume.zRot = 0.15F * Mth.cos(f * 0.43F);
		this.plume.yScale = 1.4F + 0.4F * Mth.sin(f * 1.0F) + 0.15F * Mth.sin(f * 2.3F);

		setScale(this.mane, 1.0F + 0.03F * Mth.sin(f * 0.7F));
		for (int i = 0; i < this.maneTongues.length; i++) {
			this.maneTongues[i].xRot = -this.body.xRot;
			this.maneTongues[i].yScale = 1.1F + 0.5F * Mth.sin(f * 1.1F + i * 1.7F);
		}
	}

	private static void setScale(ModelPart part, float scale) {
		part.xScale = scale;
		part.yScale = scale;
		part.zScale = scale;
	}

	/**
	 * Sitting: the body tilts back so the chest is up and the rump is on the ground, the hind
	 * legs fold flat, the front legs stay straight, and the tail lies back. Offsets keep the
	 * head on the chest and the haunches clear of the body's sides.
	 */
	private void setSittingPose() {
		this.body.xRot = -0.4F;
		this.body.y += 4.0F;
		this.head.y += 1.9F;
		this.head.z += 1.6F;
		this.tail.xRot = 0.1F;
		this.rightHindLeg.xRot = -1.45F;
		this.leftHindLeg.xRot = -1.45F;
		this.rightHindLeg.y += 4.0F;
		this.leftHindLeg.y += 4.0F;
		this.rightHindLeg.z -= 0.5F;
		this.leftHindLeg.z -= 0.5F;
		this.rightHindLeg.x -= 0.6F;
		this.leftHindLeg.x += 0.6F;
		this.rightFrontLeg.xRot = 0.0F;
		this.leftFrontLeg.xRot = 0.0F;
	}
}
