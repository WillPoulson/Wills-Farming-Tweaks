package uk.co.willpoulson.willsfarmingtweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.willpoulson.willsfarmingtweaks.config.ConfigManager;

@Mixin(BoneMealItem.class)
public class BoneMealItemMixin {

	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void applyBoneMealToNearbyCrops(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		Level level = context.getLevel();
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

		BlockPos pos = context.getClickedPos();
		BlockState state = level.getBlockState(pos);
		Block block = state.getBlock();

		if (!(block instanceof CropBlock cropBlock) || cropBlock.isMaxAge(state)) return;

		int radius = ConfigManager.get().bonemeal.radius;
		double baseChance = ConfigManager.get().bonemeal.baseChance;

		// Always apply to the clicked crop first (vanilla feel)
		boolean appliedToOrigin = BoneMealItem.growCrop(context.getItemInHand(), level, pos);
		if (appliedToOrigin) {
			serverLevel.getChunkSource().blockChanged(pos);
			spawnBoneMealParticles(serverLevel, pos);
		}

		if (radius > 0 && baseChance > 0.0) {
			double maxDistance = Math.sqrt(2 * (radius * radius));

			for (int dx = -radius; dx <= radius; dx++) {
				for (int dz = -radius; dz <= radius; dz++) {
					if (dx == 0 && dz == 0) continue;

					BlockPos nearbyPos = pos.offset(dx, 0, dz);
					BlockState nearbyState = level.getBlockState(nearbyPos);

					if (!(nearbyState.getBlock() instanceof CropBlock nearbyCrop) || nearbyCrop.isMaxAge(nearbyState)) {
						continue;
					}

					double distance = Math.sqrt(dx * dx + dz * dz);
					double distanceChance = 1.0 - (distance / maxDistance);
					double chance = baseChance * distanceChance;

					if (level.getRandom().nextDouble() > chance) continue;

					boolean applied = BoneMealItem.growCrop(context.getItemInHand(), level, nearbyPos);
					if (applied) {
						serverLevel.getChunkSource().blockChanged(nearbyPos);
						spawnBoneMealParticles(serverLevel, nearbyPos);
					}
				}
			}
		}

		level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 1.0F);
		cir.setReturnValue(InteractionResult.SUCCESS);
	}

	@Unique
	private void spawnBoneMealParticles(ServerLevel level, BlockPos pos) {
		for (int i = 0; i < 10; i++) {
			double offsetX = level.getRandom().nextDouble();
			double offsetY = level.getRandom().nextDouble() * 0.5 + 0.5;
			double offsetZ = level.getRandom().nextDouble();
			double deltaY = level.getRandom().nextDouble() * 0.1;
			double speed = level.getRandom().nextDouble() * 0.05;

			Vec3 particlePos = new Vec3(pos.getX() + offsetX, pos.getY() + offsetY, pos.getZ() + offsetZ);
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, particlePos.x, particlePos.y, particlePos.z, 1, 0, deltaY, 0, speed);
		}
	}
}
