package de.f4ked.datacenterextended.power;

import de.f4ked.datacenterextended.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** Grid connection: exports the FE it receives for money (see {@link GridConnectionBlockEntity}). */
public class GridConnectionBlock extends PowerBlock {
    private final GridTier tier;

    public GridConnectionBlock(GridTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public GridTier tier() {
        return tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GridConnectionBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        return level.isClientSide() ? null : createTickerHelper(beType, ModBlockEntities.GRID_CONNECTION.get(), GridConnectionBlockEntity::serverTick);
    }
}
