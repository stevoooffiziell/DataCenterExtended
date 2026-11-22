package de.f4ked.datacenterextended.power;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;

import java.util.Locale;

/** Chat report of the network tester ("multimeter") for power cables, generators, batteries and grid connections. */
public final class PowerReport {
    private PowerReport() {
    }

    public static void send(ServerLevel level, BlockPos pos, Player player) {
        if (level.getBlockEntity(pos) instanceof GeneratorBlockEntity gen) {
            ChatFormatting color = !gen.status().producing() ? ChatFormatting.RED : gen.status().warning() ? ChatFormatting.YELLOW : ChatFormatting.GREEN;
            player.sendSystemMessage(Component.translatable("message.datacenterextended.meter.generator",
                    fe(gen.output()), fe(gen.peak())).append(" · ").append(Component.translatable(gen.status().translationKey())).withStyle(color));
        }
        PowerNetworkManager.PowerInfo info = PowerNetworkManager.get(level).describe(level, pos);
        if (info == null) {
            player.sendSystemMessage(Component.translatable("message.datacenterextended.meter.none").withStyle(ChatFormatting.YELLOW));
            return;
        }
        player.sendSystemMessage(Component.translatable("message.datacenterextended.meter.header", info.cables(), info.generators(),
                info.batteries(), info.consumers(), info.grids()).withStyle(ChatFormatting.AQUA));
        Component tier = info.bottleneck() == null ? Component.literal("-") : Component.translatable(info.bottleneck().translationKey());
        player.sendSystemMessage(Component.translatable("message.datacenterextended.meter.limit", fe(info.transferLimit()), tier)
                .withStyle(ChatFormatting.GRAY));
        player.sendSystemMessage(Component.translatable("message.datacenterextended.meter.flow", fe(info.production()),
                fe(info.toConsumers()), fe(info.toBatteries()), fe(info.toGrid())).withStyle(ChatFormatting.GRAY));
        if (info.capacity() > 0) {
            player.sendSystemMessage(Component.translatable("message.datacenterextended.meter.storage",
                    String.format(Locale.ROOT, "%,d", info.stored()), String.format(Locale.ROOT, "%,d", info.capacity()),
                    Math.round(100.0 * info.stored() / info.capacity())).withStyle(ChatFormatting.GRAY));
        }
        if (info.limited()) {
            player.sendSystemMessage(Component.translatable("message.datacenterextended.meter.limited", tier).withStyle(ChatFormatting.RED));
        }
        if (info.truncated()) {
            player.sendSystemMessage(Component.translatable("message.datacenterextended.tester.truncated").withStyle(ChatFormatting.RED));
        }
    }

    private static String fe(double value) {
        return Math.abs(value) >= 100 ? String.format(Locale.ROOT, "%,d", Math.round(value)) : String.format(Locale.ROOT, "%.1f", value);
    }
}
