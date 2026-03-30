package uk.co.willpoulson.willsfarmingtweaks.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import uk.co.willpoulson.willsfarmingtweaks.config.ConfigManager;

@Mixin(HoeItem.class)
public class HoeItemMixin {

	@Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
	private void harvestCropsWithHoe(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
		Level level = context.getLevel();
		if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) return;

		Player player = context.getPlayer();
		ItemStack stack = context.getItemInHand();

		int cooldownTicks = ConfigManager.get().harvest.cooldownTicks;
		if (player != null && player.getCooldowns().isOnCooldown(stack)) return;

		int radius = getRadiusFor(stack);
		if (radius < 0) return;

		BlockPos originPos = context.getClickedPos();
		BlockState originState = level.getBlockState(originPos);

		if (!(originState.getBlock() instanceof CropBlock crop) || !crop.isMaxAge(originState)) return;

		boolean harvested = false;
		EquipmentSlot slot = context.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;

		for (int dx = -radius; dx <= radius; dx++) {
			for (int dz = -radius; dz <= radius; dz++) {
				BlockPos pos = originPos.offset(dx, 0, dz);
				BlockState state = level.getBlockState(pos);

				if (!(state.getBlock() instanceof CropBlock nearbyCrop) || !nearbyCrop.isMaxAge(state)) continue;

				serverLevel.setBlock(pos, nearbyCrop.defaultBlockState(), Block.UPDATE_ALL);

				if (player != null) {
					Block.dropResources(state, serverLevel, pos, null, player, stack);
					stack.hurtAndBreak(1, player, slot);
				} else {
					Block.dropResources(state, serverLevel, pos);
				}

				serverLevel.levelEvent(null, LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(state));

				serverLevel.playSound(
						null,
						pos,
						state.getSoundType().getBreakSound(),
						SoundSource.BLOCKS,
						(state.getSoundType().getVolume() + 1.0F) / 2.0F,
						state.getSoundType().getPitch() * 0.8F
				);

				harvested = true;
			}
		}

		if (!harvested) return;

		if (player != null) {
			player.swing(context.getHand(), true);
			if (cooldownTicks > 0) player.getCooldowns().addCooldown(stack, cooldownTicks);
		}

		cir.setReturnValue(InteractionResult.SUCCESS);
	}

	@Unique
	private int getRadiusFor(ItemStack stack) {
		String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();

		Integer configured = ConfigManager.get().harvest.radiusByItemId.get(id);
		if (configured != null) return configured;

		return ConfigManager.get().harvest.defaultRadius;
	}
}
