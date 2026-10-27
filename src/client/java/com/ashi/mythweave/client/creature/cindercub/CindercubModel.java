package com.ashi.mythweave.client.creature.cindercub;

import com.ashi.mythweave.Mythweave;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Cindercub, a small ember-spirit fox drawn "chibi": an oversized head with big shining
 * eyes, a short muzzle, tall ember-tipped ears, two little amber crystal horns, crystal shards
 * along the spine, and a tail that shows its bond: a fluffy fox tail while wild, a tail of
 * living fire once bonded.
 *
 * <p>Fire animation (bonded): the fire tail ripples, curls and flickers, with flame tongues
 * and a plume that always burn upward; the head tilts and the ears twitch while idle. The glowing parts (runes, eye shine,
 * crystals, ember tips, flame) come from {@code cindercub_glow.png}, drawn with a slow pulse
 * by {@link com.ashi.mythweave.client.mythling.PulsingGlowLayer}.
 *
 * <p>Model space is in pixels (16 = one block). Y points down and the ground is at y = 24.
 * Texture sheet is 64 x 32; the layout is documented in
 * {@code tools/textures/cindercub.ps1}, which paints the matching PNG.
 */
public class CindercubModel extends EntityModel<CindercubRenderState> {
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Mythweave.id("cindercub"), "main");

	private static final float TAIL_LIFT = 0.6F;

	private final ModelPart body;
	private final ModelPart head;
	private final ModelPart rightEar;
	private final ModelPart leftEar;
	private final ModelPart tail;
	private final ModelPart fireTail;
	private final ModelPart fireTongue;
	private final ModelPart fireMid;
	private final ModelPart fireMidTongue;
	private final ModelPart fireTip;
	private final ModelPart firePlume;
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
		this.fireTail = this.body.getChild("fire_tail");
		this.fireTongue = this.fireTail.getChild("tongue");
		this.fireMid = this.fireTail.getChild("mid");
		this.fireMidTongue = this.fireMid.getChild("tongue");
		this.fireTip = this.fireMid.getChild("tip");
		this.firePlume = this.fireTip.getChild("plume");
		this.rightHindLeg = root.getChild("right_hind_leg");
		this.leftHindLeg = root.getChild("left_hind_leg");
		this.rightFrontLeg = root.getChild("right_front_leg");
		this.leftFrontLeg = root.getChild("left_front_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		// Body: small and short (4 wide, 4 tall, 6 long) on 3-pixel legs, y 17..21.
		PartDefinition body = root.addOrReplaceChild("body",
				CubeListBuilder.create().texOffs(0, 11).addBox(-2.0F, -2.0F, -3.0F, 4.0F, 4.0F, 6.0F),
				PartPose.offset(0.0F, 19.0F, 0.0F));
		// Two amber crystal shards along the spine, leaning back.
		CubeListBuilder spineShard = CubeListBuilder.create().texOffs(54, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		body.addOrReplaceChild("spine_crystal_front", spineShard, PartPose.offsetAndRotation(0.0F, -2.0F, -0.5F, -0.4F, 0.0F, 0.0F));
		body.addOrReplaceChild("spine_crystal_back", spineShard, PartPose.offsetAndRotation(0.0F, -2.0F, 1.5F, -0.4F, 0.0F, 0.0F));

		// Head: big for the body (7 x 5 x 5), set on the front of the body and sunk one pixel
		// into it so there's no gap. The odd width gives eyes a centre column between them.
		PartDefinition head = root.addOrReplaceChild("head",
				CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -5.0F, -4.0F, 7.0F, 5.0F, 5.0F),
				PartPose.offset(0.0F, 18.0F, -3.0F));
		// Short button muzzle (sticks out one pixel), on the lower middle of the face.
		head.addOrReplaceChild("snout",
				CubeListBuilder.create().texOffs(24, 0).addBox(-1.5F, -2.0F, -5.0F, 3.0F, 2.0F, 1.0F),
				PartPose.ZERO);
		// Tall ears at the back corners, tilted outward, with glowing ember tips. Each ear
		// pivots at its base so it can twitch.
		head.addOrReplaceChild("right_ear",
				CubeListBuilder.create().texOffs(34, 0).addBox(-1.0F, -4.0F, 0.0F, 2.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(-2.5F, -5.0F, -2.0F, 0.0F, 0.0F, -0.2F));
		head.addOrReplaceChild("left_ear",
				CubeListBuilder.create().texOffs(40, 0).addBox(-1.0F, -4.0F, 0.0F, 2.0F, 4.0F, 1.0F),
				PartPose.offsetAndRotation(2.5F, -5.0F, -2.0F, 0.0F, 0.0F, 0.2F));
		// Two little crystal horns on the brow, swept back and out like tiny antlers.
		CubeListBuilder horn = CubeListBuilder.create().texOffs(50, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		head.addOrReplaceChild("right_horn", horn, PartPose.offsetAndRotation(-1.0F, -5.0F, -3.0F, -0.6F, 0.0F, -0.35F));
		head.addOrReplaceChild("left_horn", horn, PartPose.offsetAndRotation(1.0F, -5.0F, -3.0F, -0.6F, 0.0F, 0.35F));

		// Two tails at the back-top of the body, lifted (positive xRot raises a part that points
		// backward along +z); setupAnim shows exactly one of them.
		// Wild: a plain fluffy fox tail with a cream tip.
		body.addOrReplaceChild("tail",
				CubeListBuilder.create().texOffs(22, 11).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 5.0F),
				PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, TAIL_LIFT, 0.0F, 0.0F));
		// Bonded: the tail becomes fire. Three tapering flame segments (3x3x3, 2x2x3, 2x2x2),
		// each the child of the one before so the tail can ripple like a whip; flame tongues
		// lick up from the first two, and a plume burns at the tip.
		CubeListBuilder tongue = CubeListBuilder.create().texOffs(48, 11).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F);
		PartDefinition fireTail = body.addOrReplaceChild("fire_tail",
				CubeListBuilder.create().texOffs(22, 20).addBox(-1.5F, -1.5F, 0.0F, 3.0F, 3.0F, 3.0F),
				PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, TAIL_LIFT, 0.0F, 0.0F));
		fireTail.addOrReplaceChild("tongue", tongue, PartPose.offset(0.0F, -1.5F, 1.5F));
		PartDefinition fireMid = fireTail.addOrReplaceChild("mid",
				CubeListBuilder.create().texOffs(36, 20).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 3.0F),
				PartPose.offset(0.0F, 0.0F, 3.0F));
		fireMid.addOrReplaceChild("tongue", tongue, PartPose.offset(0.0F, -1.0F, 1.5F));
		PartDefinition fireTip = fireMid.addOrReplaceChild("tip",
				CubeListBuilder.create().texOffs(40, 11).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 2.0F),
				PartPose.offset(0.0F, 0.0F, 3.0F));
		fireTip.addOrReplaceChild("plume",
				CubeListBuilder.create().texOffs(58, 0).addBox(-0.5F, -2.0F, -0.5F, 1.0F, 2.0F, 1.0F),
				PartPose.offset(0.0F, -1.0F, 1.0F));

		// All four legs share one texture region. Each is 2 x 3 x 2, standing on the ground.
		CubeListBuilder leg = CubeListBuilder.create().texOffs(0, 23).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 3.0F, 2.0F);
		root.addOrReplaceChild("right_hind_leg", leg, PartPose.offset(-1.0F, 21.0F, 2.0F));
		root.addOrReplaceChild("left_hind_leg", leg, PartPose.offset(1.0F, 21.0F, 2.0F));
		root.addOrReplaceChild("right_front_leg", leg, PartPose.offset(-1.0F, 21.0F, -2.0F));
		root.addOrReplaceChild("left_front_leg", leg, PartPose.offset(1.0F, 21.0F, -2.0F));

		return LayerDefinition.create(mesh, 64, 32);
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

		// Wild: fur tail. Bonded: fire tail. Both sway gently, livelier while walking.
		this.tail.visible = !state.isTame;
		this.fireTail.visible = state.isTame;
		float sway = Mth.cos(age * 0.15F) * (0.15F + speed * 0.3F);
		this.tail.yRot = sway;
		this.fireTail.yRot = sway;

		if (state.isSitting) {
			this.setSittingPose();
		}

		if (state.isTame) {
			this.animateFireTail(age, speed);
		}
	}

	/**
	 * Fire tail animation. A travelling wave ripples down the segments (each lags the one
	 * before) while they curl upward; every segment, tongue and the plume flickers in scale
	 * on its own rhythm so the fire never pulses in sync; and tongues and plume are
	 * counter-rotated so they always burn straight up, however the tail is bent.
	 */
	private void animateFireTail(float age, float speed) {
		float f = age * (1.0F + speed * 0.5F); // a running Cindercub's fire dances faster

		this.fireMid.yRot = Mth.sin(f * 0.25F - 0.9F) * 0.3F;
		this.fireTip.yRot = Mth.sin(f * 0.25F - 1.8F) * 0.35F;
		this.fireMid.xRot = 0.2F + 0.08F * Mth.sin(f * 0.2F);
		this.fireTip.xRot = 0.3F + 0.1F * Mth.sin(f * 0.2F + 0.7F);

		setScale(this.fireTail, 1.0F + 0.04F * Mth.sin(f * 0.9F));
		setScale(this.fireMid, 1.0F + 0.07F * Mth.sin(f * 1.1F + 1.0F));
		setScale(this.fireTip, 0.85F * (1.0F + 0.1F * Mth.sin(f * 1.4F + 2.0F)));

		float baseTilt = this.body.xRot + this.fireTail.xRot;
		float midTilt = baseTilt + this.fireMid.xRot;
		float tipTilt = midTilt + this.fireTip.xRot;
		this.fireTongue.xRot = -baseTilt;
		this.fireMidTongue.xRot = -midTilt;
		this.fireTongue.yScale = 1.0F + 0.45F * Mth.sin(f * 1.3F);
		this.fireMidTongue.yScale = 1.0F + 0.45F * Mth.sin(f * 1.3F + 2.4F);

		this.firePlume.xRot = -tipTilt + 0.2F * Mth.sin(f * 0.5F);
		this.firePlume.zRot = 0.2F * Mth.cos(f * 0.43F);
		this.firePlume.yScale = 1.2F + 0.4F * Mth.sin(f * 1.1F) + 0.15F * Mth.sin(f * 2.3F);
	}

	private static void setScale(ModelPart part, float scale) {
		part.xScale = scale;
		part.yScale = scale;
		part.zScale = scale;
	}

	/**
	 * Sitting: the body tilts back so the chest is up and the rump touches the ground,
	 * the hind legs fold forward flat, the front legs stay straight, and the tail rests low.
	 * Offsets were worked out from the rest pose so the parts stay connected.
	 */
	private void setSittingPose() {
		this.body.xRot = -0.5F;            // tilt: front up, back down
		this.body.y += 2.0F;               // lower so the rump meets the ground
		this.head.y += 0.8F;               // follow the chest's front-top corner as it tilts
		this.head.z += 1.3F;
		this.tail.xRot = 0.3F;             // relative to the tilted body: lies along the ground
		this.fireTail.xRot = 0.3F;
		this.rightHindLeg.xRot = -1.4F;    // fold forward, nearly flat
		this.leftHindLeg.xRot = -1.4F;
		this.rightHindLeg.y += 2.0F;
		this.leftHindLeg.y += 2.0F;
		// Folded legs would sit exactly flush with the body's sides (both at x = +/-2) and
		// flicker where the faces overlap; half a pixel outward reads as haunches instead.
		this.rightHindLeg.x -= 0.5F;
		this.leftHindLeg.x += 0.5F;
		this.rightFrontLeg.xRot = 0.0F;    // no walk swing while sitting
		this.leftFrontLeg.xRot = 0.0F;
	}
}
