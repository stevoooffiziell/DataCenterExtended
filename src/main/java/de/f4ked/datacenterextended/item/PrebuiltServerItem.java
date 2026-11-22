package de.f4ked.datacenterextended.item;

import de.f4ked.datacenterextended.hardware.Prebuilt;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A boxed prebuilt server. The shop hands out the assembled server directly; the box itself (creative, loot) unpacks
 * into the server on right-click.
 */
public class PrebuiltServerItem extends Item {
    private final Prebuilt prebuilt;

    public PrebuiltServerItem(Prebuilt prebuilt, Properties properties) {
        super(properties);
        this.prebuilt = prebuilt;
    }

    public Prebuilt prebuilt() {
        return prebuilt;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            ItemStack box = player.getItemInHand(hand);
            box.shrink(1);
            ItemStack server = prebuilt.assemble();
            if (box.isEmpty()) {
                player.setItemInHand(hand, server);
            } else if (!player.getInventory().add(server)) {
                player.drop(server, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
