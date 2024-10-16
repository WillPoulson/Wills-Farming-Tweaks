package uk.co.willpoulson.willsfarmingtweaks.mixin;


import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HoeItem.class)
public class HoeItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void harvestCropsWithHoe(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();

        // Exit if not on the server or the world is not a ServerWorld
        if (world.isClient() || !(world instanceof ServerWorld)) {
            return;
        }

        BlockPos pos = context.getBlockPos();
        Block block = world.getBlockState(pos).getBlock();

        // Exit if the block is not a crop or the crop is not mature
        if (
            !(block instanceof CropBlock cropBlock) ||
            !cropBlock.isMature(world.getBlockState(pos)))
        {
            return;
        }

        // Get the radius for the hoe, exit if it's not a valid hoe
        Item hoeItem = context.getStack().getItem();
        int radius = getRadiusForHoe(hoeItem);
        if (radius < 0) {
            return;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        boolean harvested = false;

        PlayerEntity player = context.getPlayer();
        EquipmentSlot slot = context.getHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

        // Iterate through nearby blocks within the specified radius
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos nearbyPos = pos.add(dx, 0, dz);
                BlockState blockState = world.getBlockState(nearbyPos);
                Block nearbyBlock = blockState.getBlock();

                // Continue if the block is not a mature crop
                if (
                    !(nearbyBlock instanceof CropBlock crop) ||
                    !crop.isMature(blockState)
                ) {
                    continue;
                }

                // Harvest the crop and set the block to its default state (replant)
                serverWorld.setBlockState(nearbyPos, crop.getDefaultState());

                // Drop the crop items as if the player harvested them manually
                Block.dropStacks(blockState, serverWorld, nearbyPos);

                // Damage the hoe for each successful harvest
                context.getStack().damage(1, player, slot);

                // Mark that at least one crop was harvested
                harvested = true;
            }
        }

        // If any crops were harvested, consume the hoe's use and cancel further processing
        if (harvested) {
            serverWorld.playSound(
                null,
                pos,
                SoundEvents.BLOCK_CROP_BREAK,
                SoundCategory.BLOCKS,
                1.0F,
                1.0F
            );

            cir.setReturnValue(ActionResult.SUCCESS);
        }
    }

    @Unique
    private int getRadiusForHoe(Item hoeItem) {
        if (hoeItem == Items.WOODEN_HOE) {
            return 0; // Single block
        } else if (hoeItem == Items.STONE_HOE) {
            return 1; // 3x3 area
        } else if (hoeItem == Items.IRON_HOE || hoeItem == Items.GOLDEN_HOE) {
            return 2; // 5x5 area
        } else if (hoeItem == Items.DIAMOND_HOE) {
            return 3; // 7x7 area
        } else if (hoeItem == Items.NETHERITE_HOE) {
            return 4; // 9x9 area
        } else {
            return -1; // Not a valid hoe
        }
    }
}