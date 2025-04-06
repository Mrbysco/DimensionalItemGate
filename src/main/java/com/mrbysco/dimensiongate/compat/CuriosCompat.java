package com.mrbysco.dimensiongate.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CuriosCompat {
	public static List<ItemStack> getCuriosStacks(LivingEntity livingEntity) {
		List<ItemStack> stackList = new ArrayList<>();

//		CuriosApi.getCuriosInventory(livingEntity).ifPresent(handler -> { TODO: Re-enable when Curios updates
//			for (int i = 0; i < handler.getEquippedCurios().getSlots(); i++) {
//				stackList.add(handler.getEquippedCurios().getStackInSlot(i));
//			}
//		});

		return stackList;
	}
}
