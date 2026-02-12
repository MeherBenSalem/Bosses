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

// Made with Blockbench 4.12.3
// Exported for Minecraft version 1.17 or later with Mojang mappings
// Paste this class into your mod and generate all required imports
public class Modelshuriken<T extends Entity> extends EntityModel<T> {
	// This layer location should be baked with EntityRendererProvider.Context in
	// the entity renderer and passed into this model's constructor
	public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath("remnant_bosses", "modelshuriken"), "main");
	public final ModelPart group;

	public Modelshuriken(ModelPart root) {
		this.group = root.getChild("group");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		PartDefinition group = partdefinition.addOrReplaceChild("group",
				CubeListBuilder.create().texOffs(6, 15)
						.addBox(-0.0788F, 9.2916F, -0.6574F, 0.1575F, 0.14F, 2.73F, new CubeDeformation(0.0F))
						.texOffs(13, 6)
						.addBox(-0.35F, 9.3003F, -0.4037F, 0.28F, 0.1225F, 1.82F, new CubeDeformation(0.0F))
						.texOffs(16, 15)
						.addBox(-0.63F, 9.296F, -0.1237F, 0.28F, 0.1312F, 0.91F, new CubeDeformation(0.0F))
						.texOffs(9, 17)
						.addBox(-0.7262F, 9.2916F, 0.1563F, 0.2756F, 0.14F, 0.35F, new CubeDeformation(0.0F))
						.texOffs(14, 6)
						.addBox(0.07F, 9.3003F, -0.4037F, 0.28F, 0.1225F, 1.82F, new CubeDeformation(0.0F))
						.texOffs(12, 7)
						.addBox(0.35F, 9.296F, -0.1237F, 0.28F, 0.1312F, 0.91F, new CubeDeformation(0.0F))
						.texOffs(11, 1)
						.addBox(0.4506F, 9.2916F, 0.1563F, 0.2756F, 0.14F, 0.35F, new CubeDeformation(0.0F))
						.texOffs(1, 15)
						.addBox(0.1826F, 9.2916F, -0.9188F, 2.73F, 0.14F, 0.1575F, new CubeDeformation(0.0F))
						.texOffs(1, 6)
						.addBox(0.4363F, 9.3003F, -0.77F, 1.82F, 0.1225F, 0.28F, new CubeDeformation(0.0F))
						.texOffs(19, 15).mirror()
						.addBox(0.7163F, 9.296F, -0.49F, 0.91F, 0.1312F, 0.28F, new CubeDeformation(0.0F)).mirror(false)
						.texOffs(7, 17)
						.addBox(0.9963F, 9.2916F, -0.3894F, 0.35F, 0.14F, 0.2756F, new CubeDeformation(0.0F))
						.texOffs(19, 6).mirror()
						.addBox(0.4363F, 9.3003F, -1.19F, 1.82F, 0.1225F, 0.28F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(13, 7)
						.addBox(0.7163F, 9.296F, -1.47F, 0.91F, 0.1312F, 0.28F, new CubeDeformation(0.0F)).texOffs(8, 1)
						.addBox(0.9963F, 9.2916F, -1.5663F, 0.35F, 0.14F, 0.2756F, new CubeDeformation(0.0F))
						.texOffs(1, 6).mirror()
						.addBox(-2.9126F, 9.2916F, -0.9188F, 2.73F, 0.14F, 0.1575F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(1, 0).mirror()
						.addBox(-2.2563F, 9.3003F, -0.77F, 1.82F, 0.1225F, 0.28F, new CubeDeformation(0.0F))
						.mirror(false)
						.texOffs(19, 12).mirror()
						.addBox(-1.6263F, 9.296F, -0.49F, 0.91F, 0.1312F, 0.28F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(7, 16).mirror()
						.addBox(-1.3463F, 9.2916F, -0.3894F, 0.35F, 0.14F, 0.2756F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(19, 0).mirror()
						.addBox(-2.2563F, 9.3003F, -1.19F, 1.82F, 0.1225F, 0.28F, new CubeDeformation(0.0F))
						.mirror(false)
						.texOffs(13, 4).mirror()
						.addBox(-1.6263F, 9.296F, -1.47F, 0.91F, 0.1312F, 0.28F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(8, 0).mirror()
						.addBox(-1.3463F, 9.2916F, -1.5663F, 0.35F, 0.14F, 0.2756F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(6, 6)
						.addBox(-0.0788F, 9.2916F, -3.7112F, 0.1575F, 0.14F, 2.73F, new CubeDeformation(0.0F))
						.texOffs(13, 0)
						.addBox(-0.35F, 9.3003F, -3.0549F, 0.28F, 0.1225F, 1.82F, new CubeDeformation(0.0F))
						.texOffs(16, 12)
						.addBox(-0.63F, 9.296F, -2.4249F, 0.28F, 0.1312F, 0.91F, new CubeDeformation(0.0F))
						.texOffs(9, 16)
						.addBox(-0.7262F, 9.2916F, -2.1449F, 0.2756F, 0.14F, 0.35F, new CubeDeformation(0.0F))
						.texOffs(14, 0)
						.addBox(0.07F, 9.3003F, -3.0549F, 0.28F, 0.1225F, 1.82F, new CubeDeformation(0.0F))
						.texOffs(12, 4)
						.addBox(0.35F, 9.296F, -2.4249F, 0.28F, 0.1312F, 0.91F, new CubeDeformation(0.0F))
						.texOffs(11, 0)
						.addBox(0.4506F, 9.2916F, -2.1449F, 0.2756F, 0.14F, 0.35F, new CubeDeformation(0.0F)),
				PartPose.offset(0.0F, 13.9261F, -0.0007F));
		PartDefinition cube_r1 = group.addOrReplaceChild(
				"cube_r1",
				CubeListBuilder.create().texOffs(17, 0)
						.addBox(0.5977F, -0.0654F, -1.6583F, 0.2275F, 0.1269F, 0.49F, new CubeDeformation(0.0F))
						.texOffs(2, 16)
						.addBox(0.6159F, -0.0567F, -0.9724F, 0.21F, 0.1138F, 0.49F, new CubeDeformation(0.0F))
						.texOffs(10, 10)
						.addBox(0.7707F, -0.0698F, -2.0184F, 0.1181F, 0.14F, 2.31F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 9.3614F, -1.6386F, 0.0F, 0.3927F, 0.0F));
		PartDefinition cube_r2 = group.addOrReplaceChild("cube_r2",
				CubeListBuilder.create().texOffs(5, 4)
						.addBox(0.3295F, -0.0698F, -0.7096F, 0.28F, 0.14F, 1.12F, new CubeDeformation(0.0F))
						.texOffs(5, 4)
						.addBox(-1.5923F, -0.0698F, -1.5923F, 0.07F, 0.14F, 0.07F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 9.3614F, -1.6386F, 0.0F, -0.7854F, 0.0F));
		PartDefinition cube_r3 = group.addOrReplaceChild("cube_r3",
				CubeListBuilder.create().texOffs(7, 0).mirror()
						.addBox(-0.8888F, -0.0698F, -2.0184F, 0.1181F, 0.14F, 2.31F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(17, 12)
						.addBox(-0.8252F, -0.0654F, -1.6583F, 0.2275F, 0.1269F, 0.49F, new CubeDeformation(0.0F))
						.texOffs(3, 8)
						.addBox(-0.8259F, -0.0567F, -0.9724F, 0.21F, 0.1138F, 0.49F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 9.3614F, -1.6386F, 0.0F, -0.3927F, 0.0F));
		PartDefinition cube_r4 = group.addOrReplaceChild("cube_r4",
				CubeListBuilder.create().texOffs(15, 0).addBox(-0.6095F, -0.0698F, -0.7096F, 0.28F, 0.14F, 1.12F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 9.3614F, -1.6386F, 0.0F, 0.7854F, 0.0F));
		PartDefinition cube_r5 = group.addOrReplaceChild("cube_r5", CubeListBuilder.create().texOffs(19, 0).mirror()
				.addBox(-1.6583F, -0.0654F, -0.8252F, 0.49F, 0.1269F, 0.2275F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(19, 16).mirror()
				.addBox(-0.9724F, -0.0567F, -0.8259F, 0.49F, 0.1138F, 0.21F, new CubeDeformation(0.0F)).mirror(false)
				.texOffs(3, 10).mirror()
				.addBox(-2.0184F, -0.0698F, -0.8888F, 2.31F, 0.14F, 0.1181F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-0.84F, 9.3614F, -0.84F, 0.0F, 0.3927F, 0.0F));
		PartDefinition cube_r6 = group.addOrReplaceChild("cube_r6", CubeListBuilder.create().texOffs(15, 4)
				.addBox(-0.7096F, -0.0698F, -0.6095F, 1.12F, 0.14F, 0.28F, new CubeDeformation(0.0F)).texOffs(7, 4)
				.mirror()
				.addBox(-1.5923F, -0.0698F, 1.5223F, 0.07F, 0.14F, 0.07F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-0.84F, 9.3614F, -0.84F, 0.0F, -0.7854F, 0.0F));
		PartDefinition cube_r7 = group.addOrReplaceChild("cube_r7",
				CubeListBuilder.create().texOffs(2, 0).mirror()
						.addBox(-2.0184F, -0.0698F, 0.7707F, 2.31F, 0.14F, 0.1181F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(19, 12).mirror()
						.addBox(-1.6583F, -0.0654F, 0.5977F, 0.49F, 0.1269F, 0.2275F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(3, 8)
						.addBox(-0.9724F, -0.0567F, 0.6159F, 0.49F, 0.1138F, 0.21F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(-0.84F, 9.3614F, -0.84F, 0.0F, -0.3927F, 0.0F));
		PartDefinition cube_r8 = group.addOrReplaceChild("cube_r8", CubeListBuilder.create().texOffs(0, 0).mirror()
				.addBox(-0.7096F, -0.0698F, 0.3295F, 1.12F, 0.14F, 0.28F, new CubeDeformation(0.0F)).mirror(false),
				PartPose.offsetAndRotation(-0.84F, 9.3614F, -0.84F, 0.0F, 0.7854F, 0.0F));
		PartDefinition cube_r9 = group.addOrReplaceChild("cube_r9",
				CubeListBuilder.create().texOffs(19, 1).mirror()
						.addBox(1.1683F, -0.0654F, -0.8252F, 0.49F, 0.1269F, 0.2275F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(19, 17).mirror()
						.addBox(0.4824F, -0.0567F, -0.8259F, 0.49F, 0.1138F, 0.21F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(3, 18)
						.addBox(-0.2916F, -0.0698F, -0.8888F, 2.31F, 0.14F, 0.1181F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.84F, 9.3614F, -0.84F, 0.0F, -0.3927F, 0.0F));
		PartDefinition cube_r10 = group.addOrReplaceChild("cube_r10",
				CubeListBuilder.create().texOffs(15, 7).mirror()
						.addBox(-0.4104F, -0.0698F, -0.6095F, 1.12F, 0.14F, 0.28F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(7, 4).addBox(1.5223F,
								-0.0698F, 1.5223F, 0.07F, 0.14F, 0.07F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.84F, 9.3614F, -0.84F, 0.0F, 0.7854F, 0.0F));
		PartDefinition cube_r11 = group.addOrReplaceChild("cube_r11",
				CubeListBuilder.create().texOffs(2, 8)
						.addBox(-0.2916F, -0.0698F, 0.7707F, 2.31F, 0.14F, 0.1181F, new CubeDeformation(0.0F))
						.texOffs(19, 13).mirror()
						.addBox(1.1683F, -0.0654F, 0.5977F, 0.49F, 0.1269F, 0.2275F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(3, 9).mirror()
						.addBox(0.4824F, -0.0567F, 0.6159F, 0.49F, 0.1138F, 0.21F, new CubeDeformation(0.0F))
						.mirror(false),
				PartPose.offsetAndRotation(0.84F, 9.3614F, -0.84F, 0.0F, 0.3927F, 0.0F));
		PartDefinition cube_r12 = group.addOrReplaceChild("cube_r12",
				CubeListBuilder.create().texOffs(0, 3).addBox(-0.4104F, -0.0698F, 0.3295F, 1.12F, 0.14F, 0.28F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.84F, 9.3614F, -0.84F, 0.0F, -0.7854F, 0.0F));
		PartDefinition cube_r13 = group.addOrReplaceChild(
				"cube_r13",
				CubeListBuilder.create().texOffs(17, 1)
						.addBox(0.5977F, -0.0654F, 1.1683F, 0.2275F, 0.1269F, 0.49F, new CubeDeformation(0.0F))
						.texOffs(2, 17)
						.addBox(0.6159F, -0.0567F, 0.4824F, 0.21F, 0.1138F, 0.49F, new CubeDeformation(0.0F))
						.texOffs(10, 18)
						.addBox(0.7707F, -0.0698F, -0.2916F, 0.1181F, 0.14F, 2.31F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 9.3614F, 0.0F, 0.0F, -0.3927F, 0.0F));
		PartDefinition cube_r14 = group.addOrReplaceChild("cube_r14",
				CubeListBuilder.create().texOffs(5, 7)
						.addBox(0.3295F, -0.0698F, -0.4104F, 0.28F, 0.14F, 1.12F, new CubeDeformation(0.0F))
						.texOffs(5, 4)
						.addBox(-1.5923F, -0.0698F, 1.5223F, 0.07F, 0.14F, 0.07F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 9.3614F, 0.0F, 0.0F, 0.7854F, 0.0F));
		PartDefinition cube_r15 = group.addOrReplaceChild("cube_r15",
				CubeListBuilder.create().texOffs(7, 8).mirror()
						.addBox(-0.8888F, -0.0698F, -0.2916F, 0.1181F, 0.14F, 2.31F, new CubeDeformation(0.0F))
						.mirror(false).texOffs(17, 13)
						.addBox(-0.8252F, -0.0654F, 1.1683F, 0.2275F, 0.1269F, 0.49F, new CubeDeformation(0.0F))
						.texOffs(3, 9)
						.addBox(-0.8259F, -0.0567F, 0.4824F, 0.21F, 0.1138F, 0.49F, new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 9.3614F, 0.0F, 0.0F, 0.3927F, 0.0F));
		PartDefinition cube_r16 = group.addOrReplaceChild("cube_r16",
				CubeListBuilder.create().texOffs(15, 3).addBox(-0.6095F, -0.0698F, -0.4104F, 0.28F, 0.14F, 1.12F,
						new CubeDeformation(0.0F)),
				PartPose.offsetAndRotation(0.0F, 9.3614F, 0.0F, 0.0F, -0.7854F, 0.0F));
		return LayerDefinition.create(meshdefinition, 32, 32);
	}

	@Override
	public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
			int color) {
		group.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
	}

	public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw,
			float headPitch) {
	}
}