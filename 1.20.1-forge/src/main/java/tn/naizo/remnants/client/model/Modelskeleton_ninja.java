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
public class Modelskeleton_ninja<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath("remnant_bosses", "modelskeleton_ninja"), "main");
	public final ModelPart torso;
	public final ModelPart chest;
	public final ModelPart h_head;
	public final ModelPart h_jaw;
	public final ModelPart left_arm;
	public final ModelPart right_arm;
	public final ModelPart sword;
	public final ModelPart hurtbox;
	public final ModelPart left_leg;
	public final ModelPart leftt;
	public final ModelPart right_leg;
	public final ModelPart rightt;
	public final ModelPart mid;
	public final ModelPart left_belt_part;
	public final ModelPart right_belt_part;
	public final ModelPart back_tunic;

	public Modelskeleton_ninja(ModelPart root) {
		this.torso = root.getChild("torso");
		this.chest = this.torso.getChild("chest");
		this.h_head = this.chest.getChild("h_head");
		this.h_jaw = this.h_head.getChild("h_jaw");
		this.left_arm = this.chest.getChild("left_arm");
		this.right_arm = this.chest.getChild("right_arm");
		this.sword = this.right_arm.getChild("sword");
		this.hurtbox = this.sword.getChild("hurtbox");
		this.left_leg = this.torso.getChild("left_leg");
		this.leftt = this.left_leg.getChild("leftt");
		this.right_leg = this.torso.getChild("right_leg");
		this.rightt = this.right_leg.getChild("rightt");
		this.mid = this.torso.getChild("mid");
		this.left_belt_part = this.mid.getChild("left_belt_part");
		this.right_belt_part = this.mid.getChild("right_belt_part");
		this.back_tunic = this.torso.getChild("back_tunic");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition torso = partdefinition.addOrReplaceChild("torso", CubeListBuilder.create().texOffs(35, 29).addBox(-3.75F, -1.25F, -2.5F, 7.5F, 2.5F, 5.0F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0002F, 5.8308F, -0.4328F, 0.0436F, 0.0F, 0.0F));
		PartDefinition chest = torso.addOrReplaceChild("chest",
				CubeListBuilder.create().texOffs(12, 0).addBox(-1.2525F, -13.7036F, 2.6625F, 2.505F, 2.505F, 0.005F, new CubeDeformation(0.01F)).texOffs(9, 0).addBox(-1.2525F, -13.7036F, 1.4125F, 0.005F, 2.505F, 1.255F, new CubeDeformation(0.01F))
						.texOffs(9, 0).addBox(1.2475F, -13.7036F, 1.4125F, 0.005F, 2.505F, 1.255F, new CubeDeformation(0.01F)).texOffs(0, 23).addBox(-4.9975F, -11.185F, -1.9563F, 9.995F, -0.005F, 4.62F, new CubeDeformation(-0.01F)).texOffs(0, 0)
						.addBox(-4.9975F, -11.21F, 2.6687F, 9.995F, 1.87F, -0.005F, new CubeDeformation(-0.01F)).texOffs(30, 0).addBox(-4.9725F, -11.21F, -1.9563F, -0.005F, 1.87F, 4.62F, new CubeDeformation(-0.01F)).texOffs(30, 0)
						.addBox(4.9775F, -11.21F, -1.9563F, -0.005F, 1.87F, 4.62F, new CubeDeformation(-0.01F)).texOffs(25, 53).addBox(-2.5025F, -3.6111F, -0.212F, 5.005F, 3.755F, 0.005F, new CubeDeformation(0.01F)).texOffs(18, 0)
						.addBox(-2.5025F, -3.6111F, -1.462F, 0.005F, 3.755F, 1.255F, new CubeDeformation(0.01F)).texOffs(18, 0).addBox(2.4975F, -3.6111F, -1.462F, 0.005F, 3.755F, 1.255F, new CubeDeformation(0.01F)),
				PartPose.offset(0.0F, -1.3914F, 2.7095F));
		PartDefinition chest_r1 = chest.addOrReplaceChild("chest_r1",
				CubeListBuilder.create().texOffs(10, 45).addBox(-8.8125F, -6.3125F, -1.3125F, 0.125F, 7.625F, 5.125F, new CubeDeformation(0.25F)).texOffs(10, 45).addBox(1.1875F, -6.3125F, -1.3125F, 0.125F, 7.625F, 5.125F, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(3.75F, -4.9485F, -3.1957F, -0.3927F, 0.0F, 0.0F));
		PartDefinition chest_r2 = chest.addOrReplaceChild(
				"chest_r2", CubeListBuilder.create().texOffs(49, 0).addBox(-11.2525F, -5.0025F, -2.5025F, 0.005F, 7.505F, 5.005F, new CubeDeformation(0.01F)).texOffs(49, 0)
						.addBox(-1.2525F, -5.0025F, -2.5025F, 0.005F, 7.505F, 5.005F, new CubeDeformation(0.01F)).texOffs(14, 35).addBox(-11.6525F, -5.0025F, -2.5025F, 10.005F, 7.505F, 0.005F, new CubeDeformation(0.01F)),
				PartPose.offsetAndRotation(6.25F, -5.625F, -1.5625F, -0.3927F, 0.0F, 0.0F));
		PartDefinition chest_r3 = chest.addOrReplaceChild("chest_r3", CubeListBuilder.create().texOffs(42, 0).addBox(-5.0025F, -5.0025F, 4.9975F, 10.005F, 7.505F, 0.005F, new CubeDeformation(0.01F)),
				PartPose.offsetAndRotation(0.3F, -6.5817F, -3.8722F, -0.3927F, 0.0F, 0.0F));
		PartDefinition h_head = chest.addOrReplaceChild("h_head",
				CubeListBuilder.create().texOffs(7, 15).addBox(-4.3625F, -7.1074F, -9.688F, 9.0F, 8.0F, 1.5F, new CubeDeformation(-0.25F)).texOffs(20, 8).addBox(-4.9375F, -7.1074F, -8.438F, 1.0F, 9.0F, 8.0F, new CubeDeformation(-0.25F))
						.texOffs(19, 6).addBox(3.9375F, -7.4824F, -9.688F, 1.0F, 9.0F, 10.0F, new CubeDeformation(-0.25F)).texOffs(7, 7).addBox(-4.9375F, -8.1062F, -9.688F, 10.0F, 1.5F, 10.0F, new CubeDeformation(-0.25F)).texOffs(9, 7)
						.addBox(-3.9375F, 0.3938F, -9.688F, 8.0F, 1.0F, 10.0F, new CubeDeformation(-0.25F)).texOffs(18, 7).addBox(-4.6375F, -7.4824F, -0.938F, 9.0F, 8.0F, 1.5F, new CubeDeformation(-0.25F)),
				PartPose.offsetAndRotation(0.0F, -13.75F, 2.5F, 0.0873F, 0.0F, 0.0F));
		PartDefinition h_jaw = h_head.addOrReplaceChild("h_jaw", CubeListBuilder.create(), PartPose.offsetAndRotation(0.0F, -0.9824F, -3.438F, 0.1745F, 0.0F, 0.0F));
		PartDefinition left_arm = chest.addOrReplaceChild("left_arm",
				CubeListBuilder.create().texOffs(6, 59).addBox(0.0F, -1.25F, -1.25F, 2.5F, 15.0F, 2.5F, new CubeDeformation(0.0F)).texOffs(41, 45).addBox(-0.0625F, -1.4375F, -1.3125F, 2.625F, 15.125F, 2.625F, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(6.25F, -10.625F, -0.9375F, 0.0F, 0.0F, -0.1309F));
		PartDefinition right_arm = chest.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(6, 59).addBox(-2.5F, -1.25F, -1.25F, 2.5F, 15.0F, 2.5F, new CubeDeformation(0.0F)).texOffs(39, 46).mirror()
				.addBox(-2.5625F, -1.4375F, -1.3125F, 2.625F, 15.125F, 2.625F, new CubeDeformation(0.25F)).mirror(false), PartPose.offsetAndRotation(-6.25F, -10.625F, -0.9375F, 0.0F, 0.0F, 0.1309F));
		PartDefinition sword = right_arm
				.addOrReplaceChild(
						"sword", CubeListBuilder.create().texOffs(2, 64).addBox(-1.25F, -1.25F, -2.5F, 1.25F, 2.5F, 8.75F, new CubeDeformation(0.0F)).texOffs(0, 67)
								.addBox(-0.6275F, -1.2525F, -33.7525F, 0.005F, 2.505F, 30.005F, new CubeDeformation(0.01F)).texOffs(0, 64).addBox(-1.875F, -1.25F, 6.25F, 2.5F, 2.5F, 2.5F, new CubeDeformation(0.0F)),
						PartPose.offsetAndRotation(-0.625F, 11.875F, 0.0F, 0.3491F, 0.0F, 0.0F));
		PartDefinition sword_r1 = sword.addOrReplaceChild("sword_r1", CubeListBuilder.create().texOffs(20, 95).addBox(-0.0027F, -1.2527F, -0.6278F, 0.0055F, 2.5055F, 2.5055F, new CubeDeformation(0.011F)),
				PartPose.offsetAndRotation(-0.625F, -0.9649F, -34.2044F, -0.7854F, 0.0F, 0.0F));
		PartDefinition sword_r2 = sword.addOrReplaceChild("sword_r2", CubeListBuilder.create().texOffs(0, 64).addBox(-1.875F, -1.875F, -0.625F, 3.75F, 3.75F, 1.25F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.625F, 0.0F, -3.125F, 0.0F, 0.0F, -0.7854F));
		PartDefinition hurtbox = sword.addOrReplaceChild("hurtbox", CubeListBuilder.create(), PartPose.offset(-0.625F, 0.0F, -29.125F));
		PartDefinition left_leg = torso.addOrReplaceChild("left_leg",
				CubeListBuilder.create().texOffs(38, 49).addBox(-0.6563F, 1.9997F, -1.2297F, 2.625F, 15.125F, 2.625F, new CubeDeformation(0.25F)).texOffs(6, 59).addBox(-0.5938F, 2.0622F, -1.1672F, 2.5F, 15.0F, 2.5F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(3.0938F, 0.9214F, 0.4393F, 0.0F, -0.0873F, -0.1047F));
		PartDefinition leftt = left_leg
				.addOrReplaceChild(
						"leftt", CubeListBuilder.create().texOffs(73, 22).addBox(-1.25F, 0.0F, -4.375F, 2.5F, 13.75F, 7.5F, new CubeDeformation(0.0F)).texOffs(94, 21)
								.addBox(-1.3125F, -0.1875F, -4.4375F, 2.625F, 2.0F, 7.625F, new CubeDeformation(0.25F)).texOffs(95, 21).addBox(-1.3125F, 11.8125F, -4.4375F, 2.625F, 2.0F, 7.625F, new CubeDeformation(0.25F)),
						PartPose.offsetAndRotation(1.2813F, -3.2503F, 1.0203F, 0.0F, 0.0F, -0.0873F));
		PartDefinition right_leg = torso.addOrReplaceChild("right_leg", CubeListBuilder.create().texOffs(36, 48).mirror().addBox(-1.9688F, 1.9997F, -1.2297F, 2.625F, 15.125F, 2.625F, new CubeDeformation(0.25F)).mirror(false).texOffs(6, 59)
				.addBox(-1.9063F, 2.0622F, -1.1672F, 2.5F, 15.0F, 2.5F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(-3.0938F, 0.9214F, 0.4393F, 0.0F, 0.0873F, 0.1047F));
		PartDefinition rightt = right_leg.addOrReplaceChild("rightt", CubeListBuilder.create().texOffs(73, 22).addBox(-1.25F, 0.0F, -4.375F, 2.5F, 13.75F, 7.5F, new CubeDeformation(0.0F)).texOffs(95, 21).mirror()
				.addBox(-1.3125F, 11.8125F, -4.4375F, 2.625F, 2.0F, 7.625F, new CubeDeformation(0.25F)).mirror(false).texOffs(95, 21).mirror().addBox(-1.3125F, -0.1875F, -4.4375F, 2.625F, 2.0F, 7.625F, new CubeDeformation(0.25F)).mirror(false),
				PartPose.offsetAndRotation(-1.2813F, -3.2503F, 1.0203F, 0.0F, 0.0F, 0.0873F));
		PartDefinition mid = torso.addOrReplaceChild("mid",
				CubeListBuilder.create().texOffs(74, 35).addBox(-3.75F, -0.2865F, -0.3409F, 7.5F, 8.75F, 1.25F, new CubeDeformation(0.0F)).texOffs(67, 8).addBox(-3.6125F, -0.349F, -0.4034F, 7.625F, 8.875F, 1.375F, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.001F, 0.0962F, -3.0052F, -0.0873F, 0.0F, 0.0F));
		PartDefinition left_belt_part = mid.addOrReplaceChild("left_belt_part", CubeListBuilder.create(), PartPose.offsetAndRotation(3.2508F, 0.073F, -1.312F, 0.0436F, 0.0F, -0.0873F));
		PartDefinition right_belt_part = mid.addOrReplaceChild("right_belt_part", CubeListBuilder.create(), PartPose.offsetAndRotation(-3.2492F, 0.073F, -1.312F, 0.0436F, 0.0F, 0.0873F));
		PartDefinition back_tunic = torso.addOrReplaceChild("back_tunic",
				CubeListBuilder.create().texOffs(73, 30).addBox(-3.75F, -0.625F, -0.625F, 7.5F, 8.75F, 1.25F, new CubeDeformation(0.0F)).texOffs(61, 17).addBox(-3.8125F, 0.3125F, -0.6875F, 7.625F, 8.875F, 1.375F, new CubeDeformation(0.25F)),
				PartPose.offsetAndRotation(-0.001F, -0.0549F, 4.2765F, 0.1745F, 0.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 128, 128);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
		torso.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
	}
}