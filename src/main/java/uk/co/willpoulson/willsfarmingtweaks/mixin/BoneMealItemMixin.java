package uk.co.willpoulson.willsfarmingtweaks.mixin;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropBlock;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {
    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    private void applyBoneMealToNearbyCrops(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        World world = context.getWorld();

        // Exit if not on the server or the world is not a ServerWorld
        if (world.isClient() || !(world instanceof ServerWorld)) {
            return;
        }

        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);
        Block block = state.getBlock();

        // Exit if the block is not a crop or the crop is already mature
        if (!(block instanceof CropBlock cropBlock) || cropBlock.isMature(state)) {
            return;
        }

        int radius = 2;

        // Iterate through nearby blocks within the specified radius
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                BlockPos nearbyPos = pos.add(dx, 0, dz);
                BlockState nearbyState = world.getBlockState(nearbyPos);
                Block nearbyBlock = nearbyState.getBlock();

                // If the nearby block is not a crop, or it is already mature, skip to the next block
                if (!(nearbyBlock instanceof CropBlock nearbyCropBlock) || nearbyCropBlock.isMature(nearbyState)) {
                    continue;
                }

                double distance = Math.sqrt(dx * dx + dz * dz);
                double maxDistance = Math.sqrt(2 * (radius * radius));

                // Calculate the probability of applying the bonemeal effect based on distance
                double probability = 1.0 - (distance / maxDistance);

                if (world.random.nextDouble() > probability) {
                    continue;
                }

                BoneMealItem.useOnFertilizable(context.getStack(), world, nearbyPos);
                ((ServerWorld) world).getChunkManager().markForUpdate(nearbyPos);
                spawnBoneMealParticles((ServerWorld) world, nearbyPos);
            }
        }

        world.playSound(
            null,
            pos,
            SoundEvents.ITEM_BONE_MEAL_USE,
            SoundCategory.BLOCKS,
            1.0F,
            1.0F
        );

        // Cancel the original useOnBlock behavior to prevent double application
        cir.setReturnValue(ActionResult.SUCCESS);
    }

    @Unique
    private void spawnBoneMealParticles(ServerWorld world, BlockPos pos) {
        for (int i = 0; i < 10; i++) {
            double offsetX = world.random.nextDouble();
            double offsetY = world.random.nextDouble() * 0.5 + 0.5;
            double offsetZ = world.random.nextDouble();
            double deltaY = world.random.nextDouble() * 0.1;
            double speed = world.random.nextDouble() * 0.05;

            Vec3d particlePos = new Vec3d(pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ);

            world.spawnParticles(ParticleTypes.HAPPY_VILLAGER, particlePos.x, particlePos.y, particlePos.z, 1, 0, deltaY, 0, speed);
        }
    }
}