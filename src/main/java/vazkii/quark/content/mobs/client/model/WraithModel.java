package vazkii.quark.content.mobs.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import vazkii.quark.content.mobs.entity.Wraith;

import javax.annotation.Nonnull;
import java.util.Random;

public class WraithModel extends EntityModel<Wraith> {

	public final ModelPart body;
	public final ModelPart rightArm;
	public final ModelPart leftArm;

	private double offset;
	private float alphaMult;

	public WraithModel(ModelPart root) {
		super(RenderType::entityTranslucent);

		body = root.getChild("body");
		rightArm = root.getChild("rightArm");
		leftArm = root.getChild("leftArm");
	}

	public static LayerDefinition createBodyLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();

		root.addOrReplaceChild("body",
				CubeListBuilder.create()
				.texOffs(0, 0)
				.addBox(-4.0F, -8.0F, -4.0F, 8, 24, 8),
				PartPose.offset(0.0F, 0.0F, 0.0F));

		root.addOrReplaceChild("leftArm",
				CubeListBuilder.create()
				.mirror()
				.texOffs(32, 16)
				.addBox(-1.0F, -2.0F, -2.0F, 4, 12, 4),
				PartPose.offset(5.0F, 2.0F, 0.0F));

		root.addOrReplaceChild("rightArm",
				CubeListBuilder.create()
				.texOffs(32, 16)
				.addBox(-3.0F, -2.0F, -2.0F, 4, 12, 4),
				PartPose.offset(-5.0F, 2.0F, 0.0F));

		return LayerDefinition.create(mesh, 64, 64);
	}

	@Override
	public void setupAnim(Wraith entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
		Random rng = new Random(entity.getId());
		float time = ageInTicks + rng.nextInt(10000000);

		leftArm.xRot = (float) Math.toRadians(-50F + rng.nextFloat() * 20F);
		rightArm.xRot = (float) Math.toRadians(-50F + rng.nextFloat() * 20F);
		leftArm.zRot = (float) Math.toRadians(-110F + (float) Math.cos(time / (8 + rng.nextInt(2))) * (8F + rng.nextFloat() * 8F));
		rightArm.zRot = (float) Math.toRadians(110F + (float) Math.cos((time + 300) / (8 + rng.nextInt(2))) * (8F + rng.nextFloat() * 8F));

		offset = Math.sin(time / 16) * 0.1 + 0.15;
		alphaMult = 0.5F + (float) Math.sin(time / 20) * 0.3F;
	}

	@Override
	public void renderToBuffer(PoseStack matrix, @Nonnull VertexConsumer bufferIn, int packedLightIn, int packedOverlayIn, float red, float green, float blue, float alpha) {
		alpha *= alphaMult;

		matrix.pushPose();
		matrix.translate(0, offset, 0);
		body.render(matrix, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		leftArm.render(matrix, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		rightArm.render(matrix, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);

		for(int i = 0; i < 6; i++) {
			alpha *= 0.6;
			matrix.translate(0, 0, 1.5 * offset + 0.1);
			matrix.scale(0.8F, 0.8F, 0.8F);
			matrix.mulPose(Vector3f.XP.rotationDegrees(60F * (float) offset));
			body.render(matrix, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
			leftArm.render(matrix, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
			rightArm.render(matrix, bufferIn, packedLightIn, packedOverlayIn, red, green, blue, alpha);
		}
		matrix.popPose();

	}

}
