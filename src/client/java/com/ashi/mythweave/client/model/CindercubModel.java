package com.ashi.mythweave.client.model;

import com.ashi.mythweave.Mythweave;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/**
 * Blocky Cindercub model: small body, big head, oversized ears, short legs, and a
 * fluffy tail ending in an ember tip.
 *
 * <p>Model space is in pixels (16 = one block). Y points down and the ground is at
 * y = 24, so the numbers below read "how far above the feet" as 24 - y.
 * Texture sheet is 64 x 32; the layout is documented in
 * {@code tools/generate_cindercub_texture.ps1}, which paints the matching PNG.
 */
public class CindercubModel extends EntityModel<LivingEntityRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Mythweave.id("cindercub"), "main");

	private final ModelPart head;
	private final ModelPart tail;
	private final ModelPart rightHindLeg;
	private final ModelPart leftHindLeg;
	private final ModelPart rightFrontLeg;
	private final ModelPart leftFrontLeg;

	public CindercubModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
		this.tail = root.getChild("body").getChild("tail");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Body: 4 wide, 4 tall, 7 long, sitting on 3-pixel legs (y 17..21).
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 10).addBox(-2.0F, -2.0F, -3.5F, 4.0F, 4.0F, 7.0F),
				PartPose.offset(0.0F, 19.0F, 0.0F));

		// Head pivots at the front-top of the body so it can turn to look around.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -4.0F, -4.0F, 5.0F, 4.0F, 4.0F),
				PartPose.offset(0.0F, 17.5F, -3.5F));
		head.addOrReplaceChild("snout",
				CubeListBuilder.create().texOffs(20, 0).addBox(-1.5F, -1.5F, -6.0F, 3.0F, 2.0F, 2.0F),
				PartPose.ZERO);
		// Ears are deliberately oversized: 3 pixels tall on a 4-pixel head.
		head.addOrReplaceChild("right_ear",
				CubeListBuilder.create().texOffs(32, 0).addBox(-2.5F, -7.0F, -2.0F, 2.0F, 3.0F, 1.0F),
				PartPose.ZERO);
		head.addOrReplaceChild("left_ear",
				CubeListBuilder.create().texOffs(40, 0).addBox(0.5F, -7.0F, -2.0F, 2.0F, 3.0F, 1.0F),
				PartPose.ZERO);

		// Tail attaches to the back-top of the body and is tilted upward (positive xRot lifts
		// a part that points backward along +z).
		PartDefinition tail = body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(24, 10).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, -1.0F, 3.5F, 0.6F, 0.0F, 0.0F));
		tail.addOrReplaceChild("ember_tip",
				CubeListBuilder.create().texOffs(42, 10).addBox(-1.0F, -1.0F, 5.0F, 2.0F, 2.0F, 2.0F),
				PartPose.ZERO);

		// All four legs share one texture region. Each is 2 x 3 x 2, standing on the ground.
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-1.0F, 21.0F, 2.5F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(1.0F, 21.0F, 2.5F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-1.0F, 21.0F, -2.5F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(1.0F, 21.0F, -2.5F));

		return LayerDefinition.create(mesh, 64, 32);
	}

	@Override
	public void setupAnim(LivingEntityRenderState state) {
		// super.setupAnim resets every part to its default pose before we apply this frame's pose.
		super.setupAnim(state);

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

		// Gentle idle tail sway, a little livelier while walking.
		this.tail.yRot = Mth.cos(state.ageInTicks * 0.15F) * (0.15F + speed * 0.3F);
	}
}
