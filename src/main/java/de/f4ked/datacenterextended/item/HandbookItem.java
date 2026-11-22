package de.f4ked.datacenterextended.item;

import de.f4ked.datacenterextended.client.HandbookScreens;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The Data Center Handbook: right-click to read the progression guide (pages come from the language files). */
public class HandbookItem extends Item {
    /** Pages are {@code book.datacenterextended.page.1} ... {@code .PAGES}. */
    public static final int PAGES = 22;

    public HandbookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            HandbookScreens.open();
        }
        return InteractionResult.SUCCESS;
    }
}
