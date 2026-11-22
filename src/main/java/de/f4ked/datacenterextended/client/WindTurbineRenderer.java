package de.f4ked.datacenterextended.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.f4ked.datacenterextended.power.GeneratorBlock;
import de.f4ked.datacenterextended.power.GeneratorBlockEntity;
import de.f4ked.datacenterextended.power.GeneratorFamily;
import de.f4ked.datacenterextended.power.GeneratorType;
import de.f4ked.datacenterextended.registry.ModItems;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * Draws the rotor of a wind turbine (the rotor item, scaled to the free radius the turbine needs) spinning in front
 * of its nacelle while it produces power. Other generators render nothing extra.
 */
public class WindTurbineRenderer implements BlockEntityRenderer<GeneratorBlockEntity, WindTurbineRenderer.State> {
    /** Rotor angle per turbine, so it slows down smoothly instead of jumping when the wind stops. */
    private static final Map<GeneratorBlockEntity, float[]> ANGLES = new WeakHashMap<>();

    private final ItemModelResolver itemModels;

    public static class State extends BlockEntityRenderState {
        boolean wind;
        Direction facing = Direction.NORTH;
        float angle;
        float diameter;
        final ItemStackRenderState rotor = new ItemStackRenderState();
    }

    public WindTurbineRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModels = context.itemModelResolver();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(GeneratorBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        GeneratorType type = be.type();
        state.wind = type.family() == GeneratorFamily.WIND;
        if (!state.wind || be.getLevel() == null) return;
        state.facing = be.getBlockState().hasProperty(GeneratorBlock.FACING) ? be.getBlockState().getValue(GeneratorBlock.FACING) : Direction.NORTH;
        boolean active = be.getBlockState().hasProperty(GeneratorBlock.ACTIVE) && be.getBlockState().getValue(GeneratorBlock.ACTIVE);
        // [angle, speed, last time]
        float[] spin = ANGLES.computeIfAbsent(be, k -> new float[]{(be.getBlockPos().hashCode() & 0xFF) * 1.4F, 0, -1});
        float now = be.getLevel().getGameTime() + partialTicks;
        float dt = spin[2] < 0 ? 0 : Math.max(0, Math.min(5, now - spin[2]));
        spin[2] = now;
        float target = active ? 9.0F / type.tier() : 0; // big rotors turn slower
        spin[1] += (target - spin[1]) * Math.min(1, dt * 0.02F);
        spin[0] = (spin[0] + spin[1] * dt) % 360.0F;
        state.angle = spin[0];
        state.diameter = 2 * type.rotorRadius() + 0.6F;
        ItemStack stack = new ItemStack(type.tier() >= 3 ? ModItems.CARBON_FIBRE_ROTOR.get() : ModItems.FIBREGLASS_ROTOR.get());
        itemModels.updateForTopItem(state.rotor, stack, ItemDisplayContext.FIXED, be.getLevel(), null, 0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.wind) return;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(new Matrix4f().rotateY((float) Math.toRadians(-state.facing.toYRot())));
        poseStack.translate(0.0F, 0.12F, 0.62F); // hub in front of the nacelle
        poseStack.mulPose(new Matrix4f().rotateZ((float) Math.toRadians(state.angle)));
        poseStack.scale(state.diameter, state.diameter, state.diameter);
        state.rotor.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GeneratorBlockEntity be) {
        int r = be.type().rotorRadius() + 1;
        return new AABB(be.getBlockPos()).inflate(r);
    }

    @Override
    public int getViewDistance() {
        return 128;
    }
}
