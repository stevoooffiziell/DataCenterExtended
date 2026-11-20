package de.f4ked.datacenterextended.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves merged racks. A vertical column of identical rack blocks (same block, same facing) is split into groups
 * of {@link #MAX_HEIGHT}, counted from the bottom of the column. The bottom block of a group is its controller.
 */
public final class RackStack {
    public static final int MAX_HEIGHT = 3;
    private static final int MAX_SCAN = 384;

    private RackStack() {
    }

    public static boolean sameRack(BlockState a, BlockState b) {
        return a.getBlock() instanceof RackBlock && a.getBlock() == b.getBlock()
                && a.getValue(RackBlock.FACING) == b.getValue(RackBlock.FACING);
    }

    /** Positions of the merged rack containing {@code pos}, bottom (controller) first. */
    public static List<BlockPos> group(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof RackBlock)) {
            return List.of(pos);
        }
        return group(level, pos, state);
    }

    /** Variant for callers that already know the (possibly not yet placed) state at {@code pos}. */
    public static List<BlockPos> group(BlockGetter level, BlockPos pos, BlockState state) {
        BlockPos bottom = pos;
        for (int i = 0; i < MAX_SCAN && sameRack(level.getBlockState(bottom.below()), state); i++) {
            bottom = bottom.below();
        }
        int index = pos.getY() - bottom.getY();
        BlockPos start = bottom.above((index / MAX_HEIGHT) * MAX_HEIGHT);

        List<BlockPos> group = new ArrayList<>(MAX_HEIGHT);
        for (int k = 0; k < MAX_HEIGHT; k++) {
            BlockPos p = start.above(k);
            if (p.equals(pos) || sameRack(level.getBlockState(p), state)) {
                group.add(p);
            } else {
                break;
            }
        }
        return group;
    }

    public static RackPart partOf(BlockGetter level, BlockPos pos, BlockState state) {
        List<BlockPos> group = group(level, pos, state);
        if (group.size() == 1) return RackPart.SINGLE;
        int index = group.indexOf(pos);
        if (index == 0) return RackPart.BOTTOM;
        if (index == group.size() - 1) return RackPart.TOP;
        return RackPart.MIDDLE;
    }
}
