package com.mrbysco.dimensiongate.recipe;

import com.mrbysco.dimensiongate.DimensionalItemGate;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class GatedRecipes {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, DimensionalItemGate.MOD_ID);
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(ForgeRegistries.RECIPE_TYPES, DimensionalItemGate.MOD_ID);


	public static final RegistryObject<RecipeType<GatedItemRecipe>> GATED_ITEM_TYPE = RECIPE_TYPES.register("recipe", () -> new RecipeType<>() {
	});
	public static final RegistryObject<GatedItemRecipe.GatedItemRecipeSerializer> GATED_ITEM_SERIALIZER = RECIPE_SERIALIZERS.register("recipe", GatedItemRecipe.GatedItemRecipeSerializer::new);
}
