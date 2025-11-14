package tn.naizo.remnants.client.model;

import net.minecraft.world.entity.Entity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.EntityModel;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;

// Made with Blockbench 4.12.6
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class Modelrat<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("remnant_bosses", "modelrat"), "main");
	public final ModelPart plague_rat_white;
	public final ModelPart body;
	public final ModelPart torso;
	public final ModelPart h_head;
	public final ModelPart h_jaw;
	public final ModelPart h_left_ear;
	public final ModelPart h_right_ear;
	public final ModelPart hurtbox;
	public final ModelPart left_front_leg;
	public final ModelPart left_front_elbow;
	public final ModelPart left_front_foot;
	public final ModelPart right_front_leg;
	public final ModelPart right_front_elbow;
	public final ModelPart right_front_foot;
	public final ModelPart tail;
	public final ModelPart tail2;
	public final ModelPart tail3;
	public final ModelPart left_back_leg;
	public final ModelPart left_back_heel;
	public final ModelPart left_back_foot;
	public final ModelPart right_back_leg;
	public final ModelPart right_back_heel;
	public final ModelPart right_back_foot;

	public Modelrat(ModelPart root) {
		this.plague_rat_white = root.getChild("plague_rat_white");
		this.body = this.plague_rat_white.getChild("body");
		this.torso = this.body.getChild("torso");
		this.h_head = this.torso.getChild("h_head");
		this.h_jaw = this.h_head.getChild("h_jaw");
		this.h_left_ear = this.h_head.getChild("h_left_ear");
		this.h_right_ear = this.h_head.getChild("h_right_ear");
		this.hurtbox = this.h_head.getChild("hurtbox");
		this.left_front_leg = this.torso.getChild("left_front_leg");
		this.left_front_elbow = this.left_front_leg.getChild("left_front_elbow");
		this.left_front_foot = this.left_front_elbow.getChild("left_front_foot");
		this.right_front_leg = this.torso.getChild("right_front_leg");
		this.right_front_elbow = this.right_front_leg.getChild("right_front_elbow");
		this.right_front_foot = this.right_front_elbow.getChild("right_front_foot");
		this.tail = this.torso.getChild("tail");
		this.tail2 = this.tail.getChild("tail2");
		this.tail3 = this.tail2.getChild("tail3");
		this.left_back_leg = this.body.getChild("left_back_leg");
		this.left_back_heel = this.left_back_leg.getChild("left_back_heel");
		this.left_back_foot = this.left_back_heel.getChild("left_back_foot");
		this.right_back_leg = this.body.getChild("right_back_leg");
		this.right_back_heel = this.right_back_leg.getChild("right_back_heel");
		this.right_back_foot = this.right_back_heel.getChild("right_back_foot");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition plague_rat_white = partdefinition.addOrReplaceChild("plague_rat_white", CubeListBuilder.create(), PartPose.offset(0.0F, 24.0F, 0.0F));
		PartDefinition body = plague_rat_white.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, -9.5F, 9.5F));
		PartDefinition torso = body.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(0, 0).addBox(-6.0F, -5.5F, -8.0F, 12.0F, 11.0F, 12.0F, new CubeDeformation(0.0F)).texOffs(0, 2)
				.addBox(-5.0F, -4.5F, -14.0F, 10.0F, 11.0F, 11.0F, new CubeDeformation(0.0F)).texOffs(3, 22).addBox(-3.5F, -3.5F, -17.0F, 7.0F, 8.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition h_head = torso.addOrReplaceChild("h_head",
				CubeListBuilder.create().texOffs(14, 34).mirror().addBox(-2.0F, 0.5F, -13.0F, 4.0F, 3.0F, 5.0F, new CubeDeformation(0.01F)).mirror(false).texOffs(57, 31).addBox(2.0F, -1.7F, -8.1F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F))
						.texOffs(58, 31).addBox(-2.6F, -1.7F, -8.1F, 1.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)).texOffs(0, 0).addBox(-4.0F, -3.0F, -8.0F, 8.0F, 7.0F, 8.0F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, -0.5F, -17.0F));
		PartDefinition h_head_r1 = h_head.addOrReplaceChild("h_head_r1",
				CubeListBuilder.create().texOffs(45, 0).addBox(-2.0F, 0.0F, 0.0F, 0.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(45, 0).addBox(2.0F, 0.0F, 0.0F, 0.0F, 3.0F, 6.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -4.0F, -3.0F, 0.3927F, 0.0F, 0.0F));
		PartDefinition h_head_r2 = h_head.addOrReplaceChild("h_head_r2", CubeListBuilder.create().texOffs(41, -3).addBox(0.0F, -5.0F, 0.0F, 0.0F, 5.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -3.0F, -3.0F, -0.7854F, 0.0F, 0.0F));
		PartDefinition h_head_r3 = h_head.addOrReplaceChild("h_head_r3", CubeListBuilder.create().texOffs(28, 0).addBox(0.0F, -3.0F, 0.0F, 0.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.0F, 0.0F, -3.0F, 0.0F, -0.3927F, 0.0F));
		PartDefinition h_head_r4 = h_head.addOrReplaceChild("h_head_r4", CubeListBuilder.create().texOffs(27, 0).addBox(0.0F, -3.0F, 0.0F, 0.0F, 6.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(4.0F, 0.0F, -3.0F, 0.0F, 0.3927F, 0.0F));
		PartDefinition h_head_r5 = h_head.addOrReplaceChild("h_head_r5", CubeListBuilder.create().texOffs(24, 37).addBox(-3.0F, 0.0F, 0.0F, 6.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 4.0F, -7.5F, 0.3927F, 0.0F, 0.0F));
		PartDefinition h_head_r6 = h_head.addOrReplaceChild("h_head_r6", CubeListBuilder.create().texOffs(56, 54).addBox(0.35F, 0.35F, 0.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.9F, 3.5F, -12.9F, 0.0F, 0.0F, -0.7854F));
		PartDefinition h_head_r7 = h_head.addOrReplaceChild("h_head_r7", CubeListBuilder.create().texOffs(56, 55).addBox(-1.35F, 0.35F, 0.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(1.9F, 3.5F, -12.9F, 0.0F, 0.0F, 0.7854F));
		PartDefinition h_head_r8 = h_head.addOrReplaceChild("h_head_r8", CubeListBuilder.create().texOffs(36, 57).addBox(-1.0F, -1.25F, -0.75F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 1.0F, -13.0F, -0.3927F, 0.0F, 0.0F));
		PartDefinition h_jaw = h_head.addOrReplaceChild("h_jaw", CubeListBuilder.create().texOffs(13, 24).addBox(-2.0F, -1.0F, -5.0F, 4.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 4.5F, -7.5F));
		PartDefinition h_jaw_r1 = h_jaw.addOrReplaceChild("h_jaw_r1", CubeListBuilder.create().texOffs(0, 0).addBox(0.25F, -1.25F, 0.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-1.75F, -1.0F, -4.75F, 0.0F, 0.0F, 0.7854F));
		PartDefinition h_jaw_r2 = h_jaw.addOrReplaceChild("h_jaw_r2", CubeListBuilder.create().texOffs(0, 0).addBox(-1.25F, -1.25F, 0.0F, 1.0F, 1.0F, 0.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(1.75F, -1.0F, -4.75F, 0.0F, 0.0F, -0.7854F));
		PartDefinition h_left_ear = h_head.addOrReplaceChild("h_left_ear", CubeListBuilder.create().texOffs(36, 22).mirror().addBox(-2.0F, -3.0F, -0.5F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(4.0F, -3.0F, -4.5F, 0.3927F, 0.0F, 0.3927F));
		PartDefinition h_right_ear = h_head.addOrReplaceChild("h_right_ear", CubeListBuilder.create().texOffs(63, 20).addBox(-2.0F, -3.0F, -0.5F, 4.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-4.0F, -3.0F, -4.5F, 0.3927F, 0.0F, -0.3927F));
		PartDefinition hurtbox = h_head.addOrReplaceChild("hurtbox", CubeListBuilder.create(), PartPose.offset(0.0F, 4.0F, -12.5F));
		PartDefinition left_front_leg = torso.addOrReplaceChild("left_front_leg", CubeListBuilder.create().texOffs(0, 24).addBox(-1.0F, -1.5F, -1.5F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(4.5F, -0.25F, -14.5F, 0.1745F, 0.0F, -0.1309F));
		PartDefinition left_front_elbow = left_front_leg.addOrReplaceChild("left_front_elbow", CubeListBuilder.create().texOffs(7, 47).mirror().addBox(-1.0F, 0.0F, -2.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(0.0F, 4.5F, 1.0F, -0.4363F, 0.0F, 0.0F));
		PartDefinition left_front_foot = left_front_elbow.addOrReplaceChild("left_front_foot", CubeListBuilder.create().texOffs(4, 55).addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 5.0F, -1.0F, 0.2618F, 0.0F, 0.1309F));
		PartDefinition right_front_leg = torso.addOrReplaceChild("right_front_leg", CubeListBuilder.create().texOffs(15, 27).mirror().addBox(-1.0F, -1.5F, -1.5F, 2.0F, 6.0F, 3.0F, new CubeDeformation(0.25F)).mirror(false),
				PartPose.offsetAndRotation(-4.5F, -0.25F, -14.5F, 0.1745F, 0.0F, 0.1309F));
		PartDefinition right_front_elbow = right_front_leg.addOrReplaceChild("right_front_elbow", CubeListBuilder.create().texOffs(7, 46).addBox(-1.0F, 0.0F, -2.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 4.5F, 1.0F, -0.4363F, 0.0F, 0.0F));
		PartDefinition right_front_foot = right_front_elbow.addOrReplaceChild("right_front_foot", CubeListBuilder.create().texOffs(2, 59).mirror().addBox(-1.5F, 0.0F, -3.0F, 3.0F, 1.0F, 4.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(0.0F, 5.0F, -1.0F, 0.2618F, 0.0F, -0.1309F));
		PartDefinition tail = torso.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(0, 53).mirror().addBox(-1.0F, -1.5F, 0.0F, 2.0F, 3.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, 2.0F, 3.0F));
		PartDefinition tail2 = tail.addOrReplaceChild("tail2", CubeListBuilder.create().texOffs(0, 55).addBox(-0.5F, -1.0F, 0.0F, 1.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 0.0F, 7.5F));
		PartDefinition tail3 = tail2.addOrReplaceChild("tail3", CubeListBuilder.create().texOffs(0, 51).addBox(-1.0F, -1.0F, 0.5F, 1.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.5F, 0.0F, 6.5F));
		PartDefinition left_back_leg = body.addOrReplaceChild("left_back_leg", CubeListBuilder.create().texOffs(0, 19).mirror().addBox(-2.0F, -2.5F, -2.5F, 4.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(6.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F));
		PartDefinition left_back_heel = left_back_leg.addOrReplaceChild("left_back_heel", CubeListBuilder.create().texOffs(7, 46).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 4.5F, 2.5F, -0.3927F, 0.0F, 0.0F));
		PartDefinition left_back_foot = left_back_heel.addOrReplaceChild("left_back_foot", CubeListBuilder.create().texOffs(3, 57).addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.7854F, 0.0F, 0.0F));
		PartDefinition right_back_leg = body.addOrReplaceChild("right_back_leg", CubeListBuilder.create().texOffs(0, 21).mirror().addBox(-2.0F, -2.5F, -2.5F, 4.0F, 8.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-6.0F, 0.0F, 0.0F, -0.3927F, 0.0F, 0.0F));
		PartDefinition right_back_heel = right_back_leg.addOrReplaceChild("right_back_heel", CubeListBuilder.create().texOffs(7, 46).mirror().addBox(-1.0F, -1.0F, -1.0F, 2.0F, 6.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(0.0F, 4.5F, 2.5F, -0.3927F, 0.0F, 0.0F));
		PartDefinition right_back_foot = right_back_heel.addOrReplaceChild("right_back_foot", CubeListBuilder.create().texOffs(2, 57).mirror().addBox(-1.5F, -1.0F, -3.0F, 3.0F, 2.0F, 5.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(0.0F, 5.0F, 0.0F, 0.7854F, 0.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		plague_rat_white.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}