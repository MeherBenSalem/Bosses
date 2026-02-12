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
public class Modelskeleton_minion<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath("remnant_bosses", "modelskeleton_minion"), "main");
	public final ModelPart root;
	public final ModelPart torso;
	public final ModelPart main_torso;
	public final ModelPart hi_head;
	public final ModelPart left_arm;
	public final ModelPart right_arm;
	public final ModelPart right_leg;
	public final ModelPart left_leg;

	public Modelskeleton_minion(ModelPart root) {
		this.root = root.getChild("root");
		this.torso = this.root.getChild("torso");
		this.main_torso = this.torso.getChild("main_torso");
		this.hi_head = this.main_torso.getChild("hi_head");
		this.left_arm = this.torso.getChild("left_arm");
		this.right_arm = this.torso.getChild("right_arm");
		this.right_leg = this.root.getChild("right_leg");
		this.left_leg = this.root.getChild("left_leg");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition root = partdefinition.addOrReplaceChild("root", CubeListBuilder.create(),
				PartPose.offset(0.0F, 26.0F, 0.0F));
		PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create(),
				PartPose.offset(0.0F, -13.0F, 0.0F));
		PartDefinition main_torso = torso.addOrReplaceChild("main_torso",
				CubeListBuilder.create().texOffs(12, 1)
						.addBox(-4.0F, -2.0F, -3.0F, 8.0F, 2.0F, 6.0F, new CubeDeformation(0.0F)).texOffs(3, 27)
						.mirror().addBox(-1.0F, -13.0F, 1.0F, 2.0F, 11.0F, 2.0F, new CubeDeformation(0.0F))
						.mirror(false)
						.texOffs(16, 5).addBox(-4.0F, -10.0F, 1.5F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
						.texOffs(8, 44).addBox(-4.0F, -10.0F, -2.5F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F))
						.texOffs(22, 26)
						.addBox(2.0F, -10.0F, -2.5F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(26, 8)
						.addBox(2.0F, -10.0F, -2.35F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.3F)).texOffs(24, 2)
						.addBox(-4.0F, -10.0F, -2.35F, 2.0F, 2.0F, 3.0F, new CubeDeformation(0.3F)).texOffs(28, 8)
						.addBox(-4.0F, -6.0F, -3.35F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.3F)).texOffs(31, 8)
						.addBox(1.0F, -6.0F, -3.35F, 3.0F, 2.0F, 1.0F, new CubeDeformation(0.3F)).texOffs(49, 48)
						.addBox(-4.0F, -6.0F, -2.5F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(4, 49)
						.addBox(2.0F, -6.0F, -2.5F, 2.0F, 2.0F, 4.0F, new CubeDeformation(0.0F)).texOffs(38, 42)
						.addBox(-4.0F, -6.0F, 1.5F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).texOffs(16, 5)
						.addBox(-4.0F, -6.0F, 1.25F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.3F)).texOffs(32, 8)
						.addBox(2.0F, -6.0F, -1.75F, 2.0F, 2.0F, 2.4F, new CubeDeformation(0.3F)).texOffs(19, 5)
						.addBox(-4.0F, -6.0F, -1.75F, 2.0F, 2.0F, 2.4F, new CubeDeformation(0.3F)),
				PartPose.offset(0.0F, 0.0F, 0.0F));
		PartDefinition hi_head = main_torso.addOrReplaceChild("hi_head",
				CubeListBuilder.create().texOffs(11, 0)
						.addBox(-4.0F, -6.0F, -5.0F, 8.0F, 6.0F, 7.0F, new CubeDeformation(0.0F)).texOffs(27, 0)
						.addBox(-4.0F, -6.0F, -5.0F, 8.0F, 6.0F, 7.0F, new CubeDeformation(-0.3F)),
				PartPose.offset(0.0F, -13.0F, 1.0F));
		PartDefinition hi_head_r1 = hi_head.addOrReplaceChild("hi_head_r1",
				CubeListBuilder.create().texOffs(11, 0).addBox(-4.0F, 2.5F, -3.5F, 8.0F, 1.0F, 7.0F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, -2.2F, -1.6F, 0.0524F, 0.0F, 0.0F));
		PartDefinition left_arm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(1, 27)
				.addBox(-0.5F, -0.5F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(4.5F, -10.5F, 0.5F));
		PartDefinition right_arm = torso.addOrReplaceChild("right_arm",
				CubeListBuilder.create().texOffs(0, 27).mirror()
						.addBox(-2.5F, -0.5F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offset(-4.5F, -10.5F, 0.5F));
		PartDefinition right_leg = root.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(1, 27)
				.addBox(-1.5F, 1.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)),
				PartPose.offset(-2.5F, -14.0F, 0.0F));
		PartDefinition left_leg = root.addOrReplaceChild("left_leg", CubeListBuilder.create().texOffs(1, 27).addBox(
				-1.5F, 1.0F, -1.5F, 3.0F, 11.0F, 3.0F, new CubeDeformation(0.0F)), PartPose.offset(2.5F, -14.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			int color) {
		root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}