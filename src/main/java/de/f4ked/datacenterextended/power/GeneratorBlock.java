package de.f4ked.datacenterextended.power;

import de.f4ked.datacenterextended.advancement.ModAdvancements;
import de.f4ked.datacenterextended.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/** A generator of one {@link GeneratorType}. {@link #ACTIVE} = producing right now (spins rotors, smoke). */
public class GeneratorBlock extends PowerBlock {
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;
    private static final VoxelShape PANEL = Block.box(0, 0, 0, 16, 6, 16);

    private final GeneratorType type;

    public GeneratorBlock(GeneratorType type, Properties properties) {
        super(properties);
        this.type = type;
        registerDefaultState(stateDefinition.any().setValue(FACING, net.minecraft.core.Direction.NORTH).setValue(ACTIVE, false));
    }

    public GeneratorType type() {
        return type;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        return level.isClientSide() ? null : createTickerHelper(beType, ModBlockEntities.GENERATOR.get(), GeneratorBlockEntity::serverTick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return slab() ? PANEL : super.getShape(state, level, pos, context);
    }

    @Override
    protected boolean useShapeForLightOcclusion(BlockState state) {
        return slab();
    }

    /** Null-safe: block states are created inside the super constructor, before {@link #type} is assigned. */
    private boolean slab() {
        return type != null && type.slab();
    }

    @Override
    protected void onPlacedByPlayer(ServerLevel level, Player player, PowerBlockEntity be) {
        if (player instanceof ServerPlayer serverPlayer) ModAdvancements.award(serverPlayer, ModAdvancements.FIRST_GENERATOR);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(ACTIVE)) return;
        if (type.family() == GeneratorFamily.COMBUSTION && random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.SMOKE, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.3, pos.getY() + 1.05,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.3, 0, 0.04, 0);
        } else if (type.family() == GeneratorFamily.GEOTHERMAL && random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 0, 0.06, 0);
        } else if (type.family() == GeneratorFamily.NUCLEAR && random.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.CLOUD, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 1.05,
                    pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0, 0.08, 0);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }
}
