package za.co.neroland.neroruins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
//? if >=1.21.2 {
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
//?}
import net.minecraft.resources.Identifier;
import za.co.neroland.neroruins.NeroRuinsCommon;
import za.co.neroland.neroruins.guardian.Sentinel;

/** Renders one Sentinel mark: shared model, the mark's own texture and scale. */
//? if >=1.21.2 {
public class SentinelRenderer extends MobRenderer<Sentinel, LivingEntityRenderState, SentinelModel> {
//?} else {
/*public class SentinelRenderer extends MobRenderer<Sentinel, SentinelModel> {
*///?}
    private final Identifier texture;
    private final float scale;

    public SentinelRenderer(EntityRendererProvider.Context context, Sentinel.Mark mark) {
        super(context, new SentinelModel(SentinelModel.createBodyLayer().bakeRoot()), 0.6F * mark.scale());
        this.texture = Identifier.fromNamespaceAndPath(NeroRuinsCommon.MOD_ID,
                "textures/entity/sentinel_" + mark.name().toLowerCase(java.util.Locale.ROOT) + ".png");
        this.scale = mark.scale();
    }

    //? if >=1.21.2 {
    @Override
    protected void scale(LivingEntityRenderState state, PoseStack poseStack) {
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public LivingEntityRenderState createRenderState() { return new LivingEntityRenderState(); }

    @Override
    public Identifier getTextureLocation(LivingEntityRenderState state) { return texture; }
    //?} else {
    /*@Override
    protected void scale(Sentinel entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(scale, scale, scale);
    }

    @Override
    public Identifier getTextureLocation(Sentinel entity) { return texture; }
    *///?}
}
