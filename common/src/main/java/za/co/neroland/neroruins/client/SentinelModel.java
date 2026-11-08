package za.co.neroland.neroruins.client;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//?}
import net.minecraft.util.Mth;

/**
 * The Sentinel: a squat Survey guard frame with a visor head, a claw arm, an arc-cannon arm and
 * piston legs. Cube geometry; the legs and claw swing with the walk and the head scans while idle.
 */
//? if >=1.21.2 {
public class SentinelModel extends EntityModel<LivingEntityRenderState> {
//?} else {
/*public class SentinelModel extends net.minecraft.client.model.HierarchicalModel<za.co.neroland.neroruins.guardian.Sentinel> {
*///?}
    private final ModelPart head;
    private final ModelPart claw;
    private final ModelPart cannon;
    private final ModelPart legLeft;
    private final ModelPart legRight;
    //? if <1.21.2 {
    /*private final ModelPart rootPart;
    *///?}

    public SentinelModel(ModelPart root) {
        //? if >=1.21.2 {
        super(root);
        //?} else {
        /*this.rootPart = root;
        *///?}
        this.head = root.getChild("head");
        this.claw = root.getChild("claw");
        this.cannon = root.getChild("cannon");
        this.legLeft = root.getChild("leg_left");
        this.legRight = root.getChild("leg_right");
    }

    //? if <1.21.2 {
    /*@Override
    public ModelPart root() { return this.rootPart; }
    *///?}

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-5F, -12F, -3F, 10F, 12F, 6F)
                .texOffs(16, 34).addBox(-8F, -13F, -3F, 3F, 3F, 6F)
                .texOffs(16, 34).addBox(5F, -13F, -3F, 3F, 3F, 6F),
                PartPose.offset(0F, 11F, 0F));
        root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(32, 0).addBox(-4F, -6F, -4F, 8F, 6F, 8F)
                .texOffs(32, 14).addBox(-4F, -4F, -4.6F, 8F, 2F, 1F)
                .texOffs(56, 14).addBox(2F, -10F, 1F, 1F, 4F, 1F),
                PartPose.offset(0F, -1F, 0F));
        root.addOrReplaceChild("claw", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-2F, -1F, -2F, 4F, 12F, 4F),
                PartPose.offset(-7F, 0F, 0F));
        root.addOrReplaceChild("cannon", CubeListBuilder.create()
                .texOffs(16, 18).addBox(-2F, -1F, -2F, 4F, 10F, 4F)
                .texOffs(32, 18).addBox(-1.5F, 6F, -7F, 3F, 3F, 5F),
                PartPose.offset(7F, 0F, 0F));
        root.addOrReplaceChild("leg_left", CubeListBuilder.create()
                .texOffs(0, 34).addBox(-2F, 0F, -2F, 4F, 13F, 4F),
                PartPose.offset(-2.5F, 11F, 0F));
        root.addOrReplaceChild("leg_right", CubeListBuilder.create()
                .texOffs(0, 34).addBox(-2F, 0F, -2F, 4F, 13F, 4F),
                PartPose.offset(2.5F, 11F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    //? if >=1.21.2 {
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        float walkPos = state.walkAnimationPos;
        float walkSpeed = Math.min(1.0F, state.walkAnimationSpeed);
        float age = state.ageInTicks;
        float yaw = state.yRot;
    //?} else {
    /*public void setupAnim(za.co.neroland.neroruins.guardian.Sentinel entity, float limbSwing, float limbSwingAmount,
            float age, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        float walkPos = limbSwing;
        float walkSpeed = Math.min(1.0F, limbSwingAmount);
        float yaw = netHeadYaw;
    *///?}
        float swing = Mth.cos(walkPos * 0.6662F) * 0.6F * walkSpeed;
        legLeft.xRot = swing;
        legRight.xRot = -swing;
        claw.xRot = -swing * 0.7F;
        cannon.xRot = -0.35F;
        head.yRot = yaw * Mth.DEG_TO_RAD + Mth.sin(age * 0.07F) * 0.25F * (1.0F - walkSpeed);
    }
}
