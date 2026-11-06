package za.co.neroland.neroruins.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import za.co.neroland.neroruins.registry.ModItems;
import za.co.neroland.neroruins.terminal.LogTerminalBlockEntity;

//? if >=1.21.9 {
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemStackRenderState;
//? if >=26.1 {
import net.minecraft.client.renderer.state.level.CameraRenderState;
//?} else {
/*import net.minecraft.client.renderer.state.CameraRenderState;
*///?}
import net.minecraft.world.phys.Vec3;
//?} else {
/*import net.minecraft.client.renderer.MultiBufferSource;
//? if >=1.21.5 {
import net.minecraft.world.phys.Vec3;
//?}
*///?}

//? if >=1.21.9 {
public final class LogTerminalRenderer implements BlockEntityRenderer<LogTerminalBlockEntity, LogTerminalRenderer.State> {
    public static final class State extends BlockEntityRenderState {
        private final ItemStackRenderState item = new ItemStackRenderState();
        private float spin;
        private float bob;
        private boolean visible;
    }

    @Override
    public State createRenderState() { return new State(); }

    @Override
    public void extractRenderState(LogTerminalBlockEntity terminal, State state, float partialTick,
            Vec3 camera, ModelFeatureRenderer.CrumblingOverlay breaking) {
        BlockEntityRenderer.super.extractRenderState(terminal, state, partialTick, camera, breaking);
        state.visible = terminal.getLevel() != null;
        if (!state.visible) return;
        long ticks = terminal.getLevel().getGameTime();
        state.spin = (ticks % 240 + partialTick) * 1.5F;
        state.bob = Mth.sin((ticks % 104720 + partialTick) * 0.06F) * 0.05F;
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(state.item,
                new ItemStack(ModItems.DATA_SHARD.get()), ItemDisplayContext.GROUND,
                terminal.getLevel(), null, (int) terminal.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) return;
        pose.pushPose();
        pose.translate(0.5F, 1.3F + state.bob, 0.5F);
        //? if >=26.3 {
        pose.rotate(Axis.YP.rotationDegrees(state.spin));
        //?} else {
        /*pose.mulPose(Axis.YP.rotationDegrees(state.spin));
        *///?}
        pose.scale(0.65F, 0.65F, 0.65F);
        state.item.submit(pose, collector, 0x00F000F0, OverlayTexture.NO_OVERLAY, 0);
        pose.popPose();
    }
//?} else {
/*public final class LogTerminalRenderer implements BlockEntityRenderer<LogTerminalBlockEntity> {
    @Override
    //? if >=1.21.5 {
    public void render(LogTerminalBlockEntity terminal, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay, Vec3 camera) {
    //?} else {
    /^public void render(LogTerminalBlockEntity terminal, float partialTick, PoseStack pose,
            MultiBufferSource buffers, int light, int overlay) {
    ^///?}
        if (terminal.getLevel() == null) return;
        long ticks = terminal.getLevel().getGameTime();
        pose.pushPose();
        pose.translate(0.5F, 1.3F + Mth.sin((ticks % 104720 + partialTick) * 0.06F) * 0.05F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees((ticks % 240 + partialTick) * 1.5F));
        pose.scale(0.65F, 0.65F, 0.65F);
        Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(ModItems.DATA_SHARD.get()),
                ItemDisplayContext.GROUND, 0x00F000F0, OverlayTexture.NO_OVERLAY, pose, buffers,
                terminal.getLevel(), (int) terminal.getBlockPos().asLong());
        pose.popPose();
    }
*///?}

    public AABB getRenderBoundingBox(LogTerminalBlockEntity terminal) {
        BlockPos pos = terminal.getBlockPos();
        return new AABB(pos.getX(), pos.getY(), pos.getZ(), pos.getX() + 1, pos.getY() + 2, pos.getZ() + 1);
    }
}
