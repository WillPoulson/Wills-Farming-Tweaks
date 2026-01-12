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
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(HoeItem.class)
public class HoeItemMixin {

    @Unique
    private static final int HARVEST_COOLDOWN_TICKS = 8;

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void harvestCropsWithHoe(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return;

        PlayerEntity player = context.getPlayer();
        Item hoeItem = context.getStack().getItem();

        int radius = getRadiusForHoe(hoeItem);
        if (radius == -1) return;

        if (player != null && player.getItemCooldownManager().isCoolingDown(context.getStack())) return;

        BlockPos originPos = context.getBlockPos();
        BlockState originState = world.getBlockState(originPos);

        if (!(originState.getBlock() instanceof CropBlock crop) || !crop.isMature(originState)) return;

        boolean harvested = false;
        EquipmentSlot slot = context.getHand() == Hand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos pos = originPos.add(dx, 0, dz);
                BlockState state = world.getBlockState(pos);

                if (!(state.getBlock() instanceof CropBlock nearbyCrop) || !nearbyCrop.isMature(state)) continue;

                serverWorld.setBlockState(pos, nearbyCrop.getDefaultState(), Block.NOTIFY_ALL);

                if (player != null) {
                    Block.dropStacks(state, serverWorld, pos, null, player, context.getStack());
                    context.getStack().damage(1, player, slot);
                } else {
                    Block.dropStacks(state, serverWorld, pos);
                }

                serverWorld.syncWorldEvent(
                        WorldEvents.BLOCK_BROKEN,
                        pos,
                        Block.getRawIdFromState(state)
                );

                serverWorld.playSound(
                        null,
                        pos,
                        state.getSoundGroup().getBreakSound(),
                        SoundCategory.BLOCKS,
                        (state.getSoundGroup().getVolume() + 1.0F) / 2.0F,
                        state.getSoundGroup().getPitch() * 0.8F
                );

                harvested = true;
            }
        }

        if (!harvested) return;

        if (player != null) {
            player.swingHand(context.getHand(), true);
            player.getItemCooldownManager().set(context.getStack(), HARVEST_COOLDOWN_TICKS);
        }

        cir.setReturnValue(ActionResult.SUCCESS);
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
