package vazkii.quark.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import vazkii.quark.content.tools.item.PickarangItem;

@Mixin(Enchantment.class)
public class EnchantmentMixin {

	@Inject(method = "canEnchant", at = @At("RETURN"), cancellable = true)
	private void canApply(ItemStack stack, CallbackInfoReturnable<Boolean> callbackInfoReturnable) {
		if (!callbackInfoReturnable.getReturnValue()) {
			Enchantment enchantment = (Enchantment) (Object) this;
			callbackInfoReturnable.setReturnValue(canPiercingApply(enchantment, stack));
		}
	}

	private static boolean canPiercingApply(Enchantment enchantment, ItemStack stack) {
		return enchantment == Enchantments.PIERCING && stack.getItem() instanceof PickarangItem;
	}

}
