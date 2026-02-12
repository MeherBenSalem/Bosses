package tn.naizo.remnants.client.model;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

public class Modelwraith<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(new ResourceLocation("remnant_bosses", "wraith"), "main");
	private final ModelPart body;
	private final ModelPart h_head;
	private final ModelPart upper_body;
	private final ModelPart upper_body_cloth;
	private final ModelPart left_arm;
	private final ModelPart left_arm_bone;
	private final ModelPart right_arm;
	private final ModelPart right_arm_bone;

	public Modelwraith(ModelPart root) {
		this.body = root.getChild("body");
		this.h_head = this.body.getChild("h_head");
		this.upper_body = this.body.getChild("upper_body");
		this.upper_body_cloth = this.upper_body.getChild("upper_body_cloth");
		this.left_arm = this.upper_body.getChild("left_arm");
		this.left_arm_bone = this.left_arm.getChild("left_arm_bone");
		this.right_arm = this.upper_body.getChild("right_arm");
		this.right_arm_bone = this.right_arm.getChild("right_arm_bone");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();

		PartDefinition body = partdefinition.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.offset(0.0F, 10.0F, 0.0F));

		PartDefinition h_head = body.addOrReplaceChild("h_head", CubeListBuilder.create().texOffs(13, 22).mirror().addBox(-3.9998F, -7.8271F, -4.3423F, 8.0F, 6.0F, 8.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(23, 42).mirror().addBox(-2.9998F, -1.8271F, -4.3423F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(23, 42).addBox(1.0002F, -1.8271F, -4.3423F, 2.0F, 2.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(14, 34).addBox(-3.9998F, -1.8271F, -2.3423F, 8.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(39, 35).addBox(4.5002F, -8.3271F, -4.8423F, 0.05F, 10.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(30, 21).addBox(-4.5008F, -8.3261F, -4.8423F, 9.0F, 0.05F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(39, 31).mirror().addBox(-4.5008F, -8.3261F, -4.8423F, 9.0F, 3.0F, 0.05F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(33, 9).addBox(-4.5008F, -8.3261F, 4.1577F, 9.0F, 10.0F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(60, 37).addBox(1.0002F, -4.8271F, -4.4423F, 2.0F, 2.0F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(20, 21).addBox(1.5002F, -4.8271F, -4.5173F, 1.0F, 1.0F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(39, 33).addBox(-4.4998F, -8.3271F, -4.8423F, 0.05F, 10.0F, 9.0F, new CubeDeformation(0.0F))
		.texOffs(58, 38).mirror().addBox(-2.9998F, -4.8271F, -4.4423F, 2.0F, 2.0F, 0.05F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(19, 21).mirror().addBox(-2.4998F, -4.8271F, -4.5173F, 1.0F, 1.0F, 0.05F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(-0.0002F, -2.1729F, 1.3423F));

		PartDefinition upper_body = body.addOrReplaceChild("upper_body", CubeListBuilder.create().texOffs(25, 0).addBox(-1.0F, -0.2188F, -0.9375F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F))
		.texOffs(32, 35).addBox(0.75F, 0.2813F, -3.9375F, 3.0F, 1.0F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(26, 47).addBox(3.75F, 0.2813F, -3.9375F, 0.05F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(24, 29).addBox(0.75F, 0.2813F, 0.0625F, 3.0F, 1.0F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(32, 36).mirror().addBox(-3.75F, 0.2813F, -3.9375F, 3.0F, 1.0F, 0.05F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(24, 29).mirror().addBox(-3.75F, 0.2813F, 0.0625F, 3.0F, 1.0F, 0.05F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(30, 54).addBox(-3.75F, 0.2813F, -3.9375F, 0.05F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(24, 29).addBox(0.75F, 2.2813F, 0.0625F, 3.0F, 1.0F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(24, 51).addBox(3.75F, 2.2813F, -3.9375F, 0.05F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(32, 36).addBox(0.75F, 2.2813F, -3.9375F, 3.0F, 1.0F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(24, 29).mirror().addBox(-3.75F, 2.2813F, 0.0625F, 3.0F, 1.0F, 0.05F, new CubeDeformation(0.0F)).mirror(false)
		.texOffs(27, 51).addBox(-3.75F, 2.2813F, -3.9375F, 0.05F, 1.0F, 4.0F, new CubeDeformation(0.0F))
		.texOffs(31, 37).mirror().addBox(-3.75F, 2.2813F, -3.9375F, 3.0F, 1.0F, 0.05F, new CubeDeformation(0.0F)).mirror(false), PartPose.offset(0.0F, -1.7859F, 1.8554F));

		PartDefinition upper_body_cloth = upper_body.addOrReplaceChild("upper_body_cloth", CubeListBuilder.create().texOffs(6, 19).addBox(-4.0F, 0.0F, -6.0F, 0.05F, 8.0F, 6.0F, new CubeDeformation(0.0F))
		.texOffs(40, 49).addBox(-4.0F, 0.0F, 0.0F, 8.0F, 8.0F, 0.05F, new CubeDeformation(0.0F))
		.texOffs(42, 26).addBox(4.0F, 0.0F, -6.0F, 0.05F, 8.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -0.2188F, 3.0625F));

		PartDefinition left_arm = upper_body.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.offset(5.4288F, 1.086F, 1.163F));

		PartDefinition cube_r1 = left_arm.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(8, 53).addBox(-1.5F, -2.5F, 0.0F, 3.0F, 5.0F, 0.05F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.0333F, 0.7F, -0.0055F, 0.0F, -0.3927F, 0.0F));

		PartDefinition cube_r2 = left_arm.addOrReplaceChild("cube_r2", CubeListBuilder.create().texOffs(41, 39).mirror().addBox(-1.5F, 0.0F, -3.0F, 3.0F, 0.05F, 6.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(1.1148F, -1.8F, -2.7772F, 0.0F, -0.3927F, 0.0F));

		PartDefinition cube_r3 = left_arm.addOrReplaceChild("cube_r3", CubeListBuilder.create().texOffs(45, 53).addBox(0.0F, -2.5F, -3.0F, 0.05F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(2.5006F, 0.7F, -2.2031F, 0.0F, -0.3927F, 0.0F));

		PartDefinition cube_r4 = left_arm.addOrReplaceChild("cube_r4", CubeListBuilder.create().texOffs(45, 53).addBox(0.0F, -2.5F, -3.0F, 0.05F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-0.271F, 0.7F, -3.3512F, 0.0F, -0.3927F, 0.0F));

		PartDefinition left_arm_bone = left_arm.addOrReplaceChild("left_arm_bone", CubeListBuilder.create(), PartPose.offset(0.4388F, -0.3F, -1.163F));

		PartDefinition cube_r5 = left_arm_bone.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(20, 46).addBox(-1.0F, -1.0F, -3.5F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(1.25F, 0.0F, -3.0F, 0.0F, -0.3927F, 0.0F));

		PartDefinition right_arm = upper_body.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.offset(-5.4288F, 1.086F, 1.163F));

		PartDefinition cube_r6 = right_arm.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(8, 53).mirror().addBox(-1.5F, -2.5F, 0.0F, 3.0F, 5.0F, 0.05F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(0.0333F, 0.7F, -0.0055F, 0.0F, 0.3927F, 0.0F));

		PartDefinition cube_r7 = right_arm.addOrReplaceChild("cube_r7", CubeListBuilder.create().texOffs(41, 39).addBox(-1.5F, 0.0F, -3.0F, 3.0F, 0.05F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-1.1148F, -1.8F, -2.7772F, 0.0F, 0.3927F, 0.0F));

		PartDefinition cube_r8 = right_arm.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(45, 53).addBox(0.0F, -2.5F, -3.0F, 0.05F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-2.5006F, 0.7F, -2.2031F, 0.0F, 0.3927F, 0.0F));

		PartDefinition cube_r9 = right_arm.addOrReplaceChild("cube_r9", CubeListBuilder.create().texOffs(45, 53).addBox(0.0F, -2.5F, -3.0F, 0.05F, 5.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.271F, 0.7F, -3.3512F, 0.0F, 0.3927F, 0.0F));

		PartDefinition right_arm_bone = right_arm.addOrReplaceChild("right_arm_bone", CubeListBuilder.create(), PartPose.offset(-0.4388F, -0.3F, -1.163F));

		PartDefinition cube_r10 = right_arm_bone.addOrReplaceChild("cube_r10", CubeListBuilder.create().texOffs(20, 46).mirror().addBox(-1.0F, -1.0F, -3.5F, 2.0F, 2.0F, 7.0F, new CubeDeformation(0.0F)).mirror(false), PartPose.offsetAndRotation(-1.25F, 0.0F, -3.0F, 0.0F, 0.3927F, 0.0F));

		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	@Override
	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {

	}

	@Override
	public void renderToBuffer(com.mojang.blaze3d.vertex.PoseStack poseStack, com.mojang.blaze3d.vertex.VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}
}
