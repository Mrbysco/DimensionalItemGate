package com.mrbysco.dimensiongate.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import top.theillusivec4.curios.api.CuriosCapability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CuriosCompat {
	/**
	 * Gets the Curios stacks from the given LivingEntity.
	 * @param livingEntity The LivingEntity to get the Curios stacks from.
	 * @return An immutable list of ItemStacks representing the Curios stacks of the given LivingEntity.
	 */
	public static List<ItemStack> getCuriosStacks(LivingEntity livingEntity) {
		List<ItemStack> stackList = new ArrayList<>();

		ResourceHandler<ItemResource> handler = livingEntity.getCapability(CuriosCapability.ITEM_HANDLER);
		if (handler != null) {
			for (int i = 0; i < handler.size(); i++) {
				stackList.add(handler.getResource(i).toStack(handler.getAmountAsInt(i)));
			}
		}
		return Collections.unmodifiableList(stackList);
	}
}
