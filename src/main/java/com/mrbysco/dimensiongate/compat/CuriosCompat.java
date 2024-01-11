package com.mrbysco.dimensiongate.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CuriosCompat {
	public static List<ItemStack> getCuriosStacks(LivingEntity livingEntity) {
		List<ItemStack> stackList = new ArrayList<>();

//		CuriosApi.getCuriosHelper().getEquippedCurios(livingEntity).ifPresent(handler -> { TODO: Wait for Curios to work with the networking update
//			for (int i = 0; i < handler.getSlots(); i++) {
//				stackList.add(handler.getStackInSlot(i));
//			}
//		});

		return stackList;
	}
}
