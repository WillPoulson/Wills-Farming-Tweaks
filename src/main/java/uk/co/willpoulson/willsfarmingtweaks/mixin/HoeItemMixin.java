package uk.co.willpoulson.willsfarmingtweaks.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.HoeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
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
import uk.co.willpoulson.willsfarmingtweaks.config.ConfigManager;

@Mixin(HoeItem.class)
public class HoeItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void harvestCropsWithHoe(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();
        if (world.isClient() || !(world instanceof ServerWorld serverWorld)) return;

        PlayerEntity player = context.getPlayer();
        ItemStack stack = context.getStack();

        int cooldownTicks = ConfigManager.get().harvest.cooldownTicks;
        if (player != null && player.getItemCooldownManager().isCoolingDown(stack)) return;

        int radius = getRadiusFor(stack);
        if (radius < 0) return;

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
                    Block.dropStacks(state, serverWorld, pos, null, player, stack);
                    stack.damage(1, player, slot);
                } else {
                    Block.dropStacks(state, serverWorld, pos);
                }

                serverWorld.syncWorldEvent(WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(state));

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
            if (cooldownTicks > 0) player.getItemCooldownManager().set(stack, cooldownTicks);
        }

        cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Unique
    private int getRadiusFor(ItemStack stack) {
        String id = Registries.ITEM.getId(stack.getItem()).toString();

        Integer configured = ConfigManager.get().harvest.radiusByItemId.get(id);
        if (configured != null) return configured;

        return ConfigManager.get().harvest.defaultRadius;
    }
}
