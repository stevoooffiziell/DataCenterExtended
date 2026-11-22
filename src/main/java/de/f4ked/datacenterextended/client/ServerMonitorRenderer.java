package de.f4ked.datacenterextended.client;

import com.mojang.blaze3d.vertex.PoseStack;
import de.f4ked.datacenterextended.block.RackStatus;
import de.f4ked.datacenterextended.block.RackType;
import de.f4ked.datacenterextended.block.ServerMonitorBlock;
import de.f4ked.datacenterextended.block.ServerMonitorBlockEntity;
import de.f4ked.datacenterextended.client.ui.Theme;
import de.f4ked.datacenterextended.economy.Coin;
import de.f4ked.datacenterextended.economy.Money;
import de.f4ked.datacenterextended.hosting.HostingPlan;
import de.f4ked.datacenterextended.time.Calendar;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Draws the rack snapshot of a Server Monitor as glowing text on its front face. */
public class ServerMonitorRenderer implements BlockEntityRenderer<ServerMonitorBlockEntity, ServerMonitorRenderer.State> {
    private static final int FULL_BRIGHT = 15728880;
    /** Screen area: 14 of 16 pixels wide, text scaled so 6 lines fit. */
    private static final float SCALE = 0.0095F;
    private static final int MAX_WIDTH = (int) (0.84F / SCALE);
    private static final int LINE = 10;

    private final Font font;

    public static class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        ServerMonitorBlockEntity.Snapshot snapshot = ServerMonitorBlockEntity.Snapshot.NONE;
    }

    private record Line(Component text, int color) {
    }

    public ServerMonitorRenderer(BlockEntityRendererProvider.Context context) {
        this.font = context.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ServerMonitorBlockEntity be, State state, float partialTicks, Vec3 cameraPosition,
                                   ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partialTicks, cameraPosition, breakProgress);
        state.facing = be.getBlockState().hasProperty(ServerMonitorBlock.FACING) ? be.getBlockState().getValue(ServerMonitorBlock.FACING) : Direction.NORTH;
        state.snapshot = be.snapshot();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        List<Line> lines = lines(state.snapshot);
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(new org.joml.Matrix4f().rotateY((float) Math.toRadians(-state.facing.toYRot())));
        poseStack.translate(0.0F, 0.06F, 0.502F); // screen area sits slightly above the stand
        poseStack.scale(SCALE, -SCALE, SCALE);
        float top = -lines.size() * LINE / 2.0F;
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            FormattedCharSequence text = fit(line.text());
            collector.submitText(poseStack, -MAX_WIDTH / 2.0F, top + i * LINE, text, false, Font.DisplayMode.POLYGON_OFFSET,
                    FULL_BRIGHT, line.color(), 0, 0);
        }
        poseStack.popPose();
    }

    private FormattedCharSequence fit(Component text) {
        if (font.width(text) <= MAX_WIDTH) return text.getVisualOrderText();
        String plain = font.plainSubstrByWidth(text.getString(), MAX_WIDTH - font.width("…")) + "…";
        return Component.literal(plain).withStyle(text.getStyle()).getVisualOrderText();
    }

    private static List<Line> lines(ServerMonitorBlockEntity.Snapshot s) {
        List<Line> lines = new ArrayList<>();
        if (!s.connected()) {
            lines.add(new Line(Component.translatable("monitor.datacenterextended.title"), Theme.ACCENT));
            lines.add(new Line(Component.translatable("monitor.datacenterextended.no_rack"), Theme.MUTED));
            return lines;
        }
        RackStatus status = RackStatus.byId(s.status());
        RackType type = RackType.byId(s.type());
        int statusColor = !status.ok() ? Theme.ERR : status.warning() ? Theme.WARN : Theme.OK;
        lines.add(new Line(Component.translatable("block.datacenterextended." + type.id()).append(" · " + Calendar.format(s.epochDay())), Theme.ACCENT));
        lines.add(new Line(Component.translatable(status.translationKey()), statusColor));
        Component temp = Component.literal(String.format(Locale.ROOT, "%.1f°C", s.temperature()));
        if (s.speed() < 0.999) temp = temp.copy().append(Component.translatable("gui.datacenterextended.rack.throttled", Math.round(s.speed() * 100)));
        lines.add(new Line(temp, s.speed() < 0.999 ? Theme.ERR : Theme.TEXT));
        switch (type) {
            case MINING -> {
                Coin coin = Coin.byId(s.coin());
                lines.add(new Line(Component.literal(Money.formatCoin(s.income(), coin) + "/h"), coin.color()));
                lines.add(new Line(Component.literal("≈ " + Money.format(Math.round(s.income() * ClientEconomy.price(coin) * 100)) + "/h"), Theme.OK));
            }
            case HOSTING -> {
                lines.add(new Line(Component.literal(Money.format(Math.round(s.income())) + "/h"), Theme.OK));
                StringBuilder plans = new StringBuilder();
                for (HostingPlan plan : HostingPlan.values()) {
                    if (!plans.isEmpty()) plans.append(' ');
                    plans.append(plan.shortName()).append(s.customers()[plan.ordinal()]);
                }
                plans.append(" · ").append(Math.round(s.satisfaction() * 100)).append('%');
                lines.add(new Line(Component.literal(plans.toString()), Theme.TEXT));
            }
            case FABRICATING -> lines.add(new Line(Component.translatable("monitor.datacenterextended.fabricating"), Theme.MUTED));
        }
        return lines;
    }
}
