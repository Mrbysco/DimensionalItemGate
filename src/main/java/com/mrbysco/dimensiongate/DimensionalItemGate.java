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
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
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

		NeoForge.EVENT_BUS.addListener(this::travelToDimension);
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

		List<RecipeHolder<GatedItemRecipe>> recipes = new ArrayList<>(level.getRecipeManager().getAllRecipesFor(GatedRecipes.GATED_ITEM_TYPE.get()));
		recipes.removeIf(recipe -> !recipe.value().getDimension().location().equals(event.getDimension().location()));

		for (RecipeHolder<GatedItemRecipe> recipe : recipes) {
			GatedItemRecipe gatedRecipe = recipe.value();
			if (gatedRecipe.isRequired()) {
				List<ItemStack> missingStacks = gatedRecipe.getMissingStacks(stackList, gatedRecipe);
				if (!missingStacks.isEmpty()) {
					if (event.getEntity() instanceof Player player) {
						ItemStack randomStack = missingStacks.get(player.getRandom().nextInt(missingStacks.size()));
						player.displayClientMessage(Component.translatable("dimensional_itemgate.gated.message2", randomStack.getDisplayName()).withStyle(ChatFormatting.RED), true);
					}
					event.setCanceled(true);
					break;
				}
			} else {
				List<ItemStack> matchingStacks = gatedRecipe.getMatchingStacks(stackList, gatedRecipe);
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
