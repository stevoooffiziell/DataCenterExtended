package de.f4ked.datacenterextended.block;

import de.f4ked.datacenterextended.network.ComputeNetworkManager;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * A 4U server rack segment holding 4 servers. Up to {@link RackStack#MAX_HEIGHT} identical racks stacked on top of
 * each other form one merged rack, controlled by its bottom block.
 */
public class RackBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<RackPart> PART = EnumProperty.create("part", RackPart.class);
    public static final BooleanProperty RUNNING = BooleanProperty.create("running");
    public static final BooleanProperty[] SERVER = {
            BooleanProperty.create("server_0"), BooleanProperty.create("server_1"),
            BooleanProperty.create("server_2"), BooleanProperty.create("server_3")};

    private final RackType type;

    public RackBlock(RackType type, Properties properties) {
        super(properties);
        this.type = type;
        BlockState state = stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, RackPart.SINGLE).setValue(RUNNING, false);
        for (BooleanProperty p : SERVER) state = state.setValue(p, false);
        registerDefaultState(state);
    }

    public RackType type() {
        return type;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        return level.isClientSide() ? null : createTickerHelper(beType, ModBlockEntities.RACK.get(), RackBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            RackBlockEntity controller = RackBlockEntity.controllerOf(level, pos);
            if (controller != null) {
                controller.openMenu(player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
        // join the rack below/above: copy its facing so stacking "just works"
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        if (below.is(this)) {
            state = state.setValue(FACING, below.getValue(FACING));
        }
        return state.setValue(PART, RackStack.partOf(context.getLevel(), context.getClickedPos(), state));
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
                                     BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (direction.getAxis() == Direction.Axis.Y) {
            return state.setValue(PART, RackStack.partOf(level, pos, state));
        }
        return state;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this) && level instanceof ServerLevel serverLevel) {
            // also covers /setblock and structures, which skip getStateForPlacement
            RackPart part = RackStack.partOf(level, pos, state);
            if (state.getValue(PART) != part) {
                level.setBlock(pos, state.setValue(PART, part), Block.UPDATE_ALL);
            }
            ComputeNetworkManager.markDirty(serverLevel);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        Containers.updateNeighboursAfterDestroy(state, level, pos);
        ComputeNetworkManager.markDirty(level);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(RUNNING) && random.nextInt(6) == 0) {
            Direction facing = state.getValue(FACING);
            double x = pos.getX() + 0.5 + facing.getStepX() * 0.52 + (random.nextDouble() - 0.5) * 0.6;
            double y = pos.getY() + 0.1 + random.nextDouble() * 0.8;
            double z = pos.getZ() + 0.5 + facing.getStepZ() * 0.52 + (random.nextDouble() - 0.5) * 0.6;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, 0, 0, 0);
        }
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
        builder.add(FACING, PART, RUNNING, SERVER[0], SERVER[1], SERVER[2], SERVER[3]);
    }
}
