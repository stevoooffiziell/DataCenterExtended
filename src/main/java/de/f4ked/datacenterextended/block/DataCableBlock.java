package de.f4ked.datacenterextended.block;

import de.f4ked.datacenterextended.network.CableTier;
import de.f4ked.datacenterextended.network.ComputeNetworkManager;
import de.f4ked.datacenterextended.network.ComputeNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Data cable. Connects to every other data cable (any tier) and to blocks that expose a {@link ComputeNode} on that
 * face. A network's bandwidth is the capacity of its slowest cable.
 */
public class DataCableBlock extends PipeBlock {
    private final CableTier tier;

    public DataCableBlock(CableTier tier, Properties properties) {
        super(6.0F, properties);
        this.tier = tier;
        BlockState state = stateDefinition.any();
        for (var property : PROPERTY_BY_DIRECTION.values()) state = state.setValue(property, false);
        registerDefaultState(state);
    }

    public CableTier tier() {
        return tier;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = defaultBlockState();
        for (Direction dir : Direction.values()) {
            BlockPos n = pos.relative(dir);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(dir), connects(level, n, level.getBlockState(n), dir));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connects(level, neighbourPos, neighbourState, direction));
    }

    private static boolean connects(LevelReader level, BlockPos neighbourPos, BlockState neighbour, Direction direction) {
        if (neighbour.getBlock() instanceof DataCableBlock) return true;
        if (neighbour.getBlock() instanceof RackBlock rack) return rack.type().networked();
        return level instanceof Level l && !l.isClientSide()
                && l.getCapability(ComputeNode.CAPABILITY, neighbourPos, direction.getOpposite()) != null;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this) && level instanceof ServerLevel serverLevel) {
            ComputeNetworkManager.markDirty(serverLevel);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        ComputeNetworkManager.markDirty(level);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }
}
