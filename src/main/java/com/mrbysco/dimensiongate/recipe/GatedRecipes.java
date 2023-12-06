package com.mrbysco.dimensiongate.recipe;

import com.mrbysco.dimensiongate.DimensionalItemGate;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class GatedRecipes {
	public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, DimensionalItemGate.MOD_ID);
	public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, DimensionalItemGate.MOD_ID);


	public static final Supplier<RecipeType<GatedItemRecipe>> GATED_ITEM_TYPE = RECIPE_TYPES.register("recipe", () -> new RecipeType<>() {
	});
	public static final Supplier<GatedItemRecipe.Serializer> GATED_ITEM_SERIALIZER = RECIPE_SERIALIZERS.register("recipe", GatedItemRecipe.Serializer::new);
}
