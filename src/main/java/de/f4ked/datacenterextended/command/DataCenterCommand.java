package de.f4ked.datacenterextended.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.Datacenterextended;
import de.f4ked.datacenterextended.economy.EconomyService;
import de.f4ked.datacenterextended.economy.MarketData;
import de.f4ked.datacenterextended.time.Calendar;
import de.f4ked.datacenterextended.time.CalendarData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * {@code /datacenter date} – shows the calendar date; {@code date set <YYYY-MM-DD>} jumps to a date and
 * {@code date speed <ticks per day>} changes the day length (both need op level 2).
 */
@EventBusSubscriber(modid = Datacenterextended.MODID)
public final class DataCenterCommand {
    private DataCenterCommand() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("datacenter")
                .then(Commands.literal("date")
                        .executes(DataCenterCommand::showDate)
                        .then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("date", StringArgumentType.word()).executes(DataCenterCommand::setDate)))
                        .then(Commands.literal("speed").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(Commands.argument("ticksPerDay", IntegerArgumentType.integer(20, 24_000_000))
                                        .executes(DataCenterCommand::setSpeed)))));
    }

    private static int showDate(CommandContext<CommandSourceStack> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        CalendarData calendar = CalendarData.get(server);
        int ticks = Config.safe(Config.TICKS_PER_DAY, 2400);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.datacenterextended.date.show",
                Calendar.format(calendar.today()), String.format("%.1f", ticks / 20.0 / 60.0)), false);
        return 1;
    }

    private static int setDate(CommandContext<CommandSourceStack> ctx) {
        String text = StringArgumentType.getString(ctx, "date");
        LocalDate date;
        try {
            date = LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            ctx.getSource().sendFailure(Component.translatable("command.datacenterextended.date.invalid", text));
            return 0;
        }
        MinecraftServer server = ctx.getSource().getServer();
        CalendarData.get(server).set(date);
        MarketData market = MarketData.get(server);
        market.clearHistory();
        market.newHalvings(date.toEpochDay()); // don't announce halvings that were jumped over
        market.update(date.toEpochDay());
        broadcastMarket(server);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.datacenterextended.date.set", Calendar.format(date)), true);
        return 1;
    }

    private static int setSpeed(CommandContext<CommandSourceStack> ctx) {
        int ticks = IntegerArgumentType.getInteger(ctx, "ticksPerDay");
        Config.TICKS_PER_DAY.set(ticks);
        broadcastMarket(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatable("command.datacenterextended.date.speed", ticks,
                String.format("%.1f", ticks / 20.0 / 60.0)), true);
        return 1;
    }

    private static void broadcastMarket(MinecraftServer server) {
        var payload = EconomyService.marketPayload(server);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) EconomyService.send(player, payload);
    }
}
