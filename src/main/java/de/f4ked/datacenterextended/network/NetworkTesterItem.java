package de.f4ked.datacenterextended.network;

import de.f4ked.datacenterextended.block.DataCableBlock;
import de.f4ked.datacenterextended.block.RackBlock;
import de.f4ked.datacenterextended.hardware.DataUnits;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/** Right-click a data cable or rack to print a report about its compute network. */
public class NetworkTesterItem extends Item {
    public NetworkTesterItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockPos pos = context.getClickedPos();
        Block block = context.getLevel().getBlockState(pos).getBlock();
        if (!(block instanceof DataCableBlock) && !(block instanceof RackBlock)) {
            return InteractionResult.PASS;
        }
        if (!(context.getLevel() instanceof ServerLevel level) || context.getPlayer() == null) {
            return InteractionResult.SUCCESS;
        }
        Player player = context.getPlayer();
        if (block instanceof RackBlock rack && !rack.type().networked()) {
            player.sendSystemMessage(Component.translatable("message.datacenterextended.tester.standalone").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS_SERVER;
        }
        NetworkInfo info = ComputeNetworkManager.get(level).describe(level, pos);
        if (info == null) {
            player.sendSystemMessage(Component.translatable("message.datacenterextended.tester.none").withStyle(ChatFormatting.YELLOW));
            return InteractionResult.SUCCESS_SERVER;
        }
        player.sendSystemMessage(Component.translatable("message.datacenterextended.tester.header", info.cables(), info.nodes())
                .withStyle(ChatFormatting.AQUA));
        Component tier = info.bottleneck() == null ? Component.literal("-") : Component.translatable(info.bottleneck().translationKey());
        player.sendSystemMessage(Component.translatable("message.datacenterextended.tester.bandwidth",
                DataUnits.formatBytesPerSecond(info.bandwidth()), tier));
        player.sendSystemMessage(Component.translatable("message.datacenterextended.tester.flow",
                DataUnits.formatBytesPerSecond(info.totalSupply()), DataUnits.formatBytesPerSecond(info.totalDemand()),
                DataUnits.formatBytesPerSecond(info.transferred())));
        if (info.bandwidthLimited()) {
            player.sendSystemMessage(Component.translatable("message.datacenterextended.tester.limited", tier).withStyle(ChatFormatting.RED));
        }
        if (info.truncated()) {
            player.sendSystemMessage(Component.translatable("message.datacenterextended.tester.truncated").withStyle(ChatFormatting.RED));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
