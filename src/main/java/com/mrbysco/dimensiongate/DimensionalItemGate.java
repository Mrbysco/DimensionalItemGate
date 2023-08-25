package com.mrbysco.dimensiongate;

import com.mojang.logging.LogUtils;
import com.mrbysco.dimensiongate.compat.CuriosCompat;
import com.mrbysco.dimensiongate.recipe.GatedItemRecipe;
import com.mrbysco.dimensiongate.recipe.GatedRecipes;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

@Mod(DimensionalItemGate.MOD_ID)
public class DimensionalItemGate {
	public static final String MOD_ID = "dimensional_itemgate";
	public static final Logger LOGGER = LogUtils.getLogger();


	public DimensionalItemGate() {
		IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

		GatedRecipes.RECIPE_TYPES.register(eventBus);
		GatedRecipes.RECIPE_SERIALIZERS.register(eventBus);

		MinecraftForge.EVENT_BUS.addListener(this::travelToDimension);
	}

	private void travelToDimension(EntityTravelToDimensionEvent event) {
		Level level = event.getEntity().level();
		List<ItemStack> stackList = new ArrayList<>();
		if (event.getEntity() instanceof ItemEntity itemEntity) {
			stackList.add(itemEntity.getItem());
		} else if (event.getEntity() instanceof Player player) {
			player.getInventory().items.forEach(stack -> {
				if (!stack.isEmpty()) stackList.add(stack);
			});
			player.getInventory().armor.forEach(stack -> {
				if (!stack.isEmpty()) stackList.add(stack);
			});
			player.getInventory().offhand.forEach(stack -> {
				if (!stack.isEmpty()) stackList.add(stack);
			});
			if (ModList.get().isLoaded("curios")) {
				CuriosCompat.getCuriosStacks(player).forEach(stack -> {
					if (!stack.isEmpty()) stackList.add(stack);
				});
			}
		} else if (event.getEntity() instanceof LivingEntity livingEntity) {
			livingEntity.getAllSlots().forEach(stack -> {
				if (!stack.isEmpty()) stackList.add(stack);
			});
			if (ModList.get().isLoaded("curios")) {
				CuriosCompat.getCuriosStacks(livingEntity).forEach(stack -> {
					if (!stack.isEmpty()) stackList.add(stack);
				});
			}
		} else if (event.getEntity() instanceof ContainerEntity containerEntity) {
			stackList.addAll(containerEntity.getItemStacks());
		}

		List<GatedItemRecipe> recipes = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(GatedRecipes.GATED_ITEM_TYPE.get()));
		recipes.removeIf(recipe -> !recipe.getDimension().location().equals(event.getDimension().location()));

		for (GatedItemRecipe recipe : recipes) {
			if (recipe.isRequired()) {
				List<ItemStack> missingStacks = recipe.getMissingStacks(stackList, recipe);
				if (!missingStacks.isEmpty()) {
					if (event.getEntity() instanceof Player player) {
						ItemStack randomStack = missingStacks.get(player.getRandom().nextInt(missingStacks.size()));
						player.displayClientMessage(Component.translatable("dimensional_itemgate.gated.message2", randomStack.getDisplayName()).withStyle(ChatFormatting.RED), true);
					}
					event.setCanceled(true);
					break;
				}
			} else {
				List<ItemStack> matchingStacks = recipe.getMatchingStacks(stackList, recipe);
				if (!matchingStacks.isEmpty()) {
					if (event.getEntity() instanceof Player player) {
						ItemStack randomStack = matchingStacks.get(player.getRandom().nextInt(matchingStacks.size()));
						player.displayClientMessage(Component.translatable("dimensional_itemgate.gated.message", randomStack.getDisplayName()).withStyle(ChatFormatting.RED), true);
					}
					event.setCanceled(true);
					break;
				}
			}
		}
	}

}
