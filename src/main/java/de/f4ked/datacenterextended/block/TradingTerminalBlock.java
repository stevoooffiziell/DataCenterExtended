package de.f4ked.datacenterextended.block;

import de.f4ked.datacenterextended.Config;
import de.f4ked.datacenterextended.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Sells Bitcoin for credits. Right-click with Bitcoin to sell the held stack, right-click empty-handed to sell every
 * Bitcoin in your inventory (or see the current rate if you have none).
 */
public class TradingTerminalBlock extends Block {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;

    public TradingTerminalBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(ModItems.BITCOIN.get())) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        int sold = stack.getCount();
        stack.setCount(0);
        sell(level, pos, player, sold);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        // Empty hand: sell every Bitcoin in the inventory, or show the rate if there is none.
        int sold = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(ModItems.BITCOIN.get())) {
                sold += s.getCount();
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        if (sold > 0) {
            sell(level, pos, player, sold);
        } else {
            player.sendSystemMessage(Component.translatable("message.datacenterextended.bitcoin_rate", Config.CREDITS_PER_BITCOIN.get()));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static void sell(Level level, BlockPos pos, Player player, int bitcoins) {
        long credits = (long) bitcoins * Config.CREDITS_PER_BITCOIN.get();
        payOut(player, credits);
        level.playSound(null, pos, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.6F, 1.4F);
        player.sendSystemMessage(Component.translatable("message.datacenterextended.sold_bitcoin", bitcoins, credits));
    }

    /** Pays credits using the largest denominations first (crate = 81, bundle = 9, credit = 1). */
    private static void payOut(Player player, long credits) {
        long crates = credits / 81;
        long bundles = (credits % 81) / 9;
        long singles = credits % 9;
        give(player, new ItemStack(ModItems.CREDIT_CRATE.get()), crates);
        give(player, new ItemStack(ModItems.CREDIT_BUNDLE.get()), bundles);
        give(player, new ItemStack(ModItems.CREDIT.get()), singles);
    }

    private static void give(Player player, ItemStack template, long amount) {
        while (amount > 0) {
            int n = (int) Math.min(amount, template.getMaxStackSize());
            ItemStack stack = template.copyWithCount(n);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
            amount -= n;
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
