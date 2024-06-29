package com.mrbysco.dimensiongate.compat;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.ArrayList;
import java.util.List;

public class CuriosCompat {
	public static List<ItemStack> getCuriosStacks(LivingEntity livingEntity) {
		List<ItemStack> stackList = new ArrayList<>();

		CuriosApi.getCuriosInventory(livingEntity).ifPresent(handler -> {
			for (int i = 0; i < handler.getEquippedCurios().getSlots(); i++) {
				stackList.add(handler.getEquippedCurios().getStackInSlot(i));
			}
		});

		return stackList;
	}
}
