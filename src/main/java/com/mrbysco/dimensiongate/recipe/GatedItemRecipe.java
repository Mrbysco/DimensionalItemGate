package com.mrbysco.dimensiongate.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrbysco.dimensiongate.DimensionalItemGate;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GatedItemRecipe implements Recipe<CraftingInput> {
	private static final MapCodec<GatedItemRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
					Codec.lazyInitialized(() -> Ingredient.CODEC.listOf(1, 9)).fieldOf("ingredients").forGetter(recipe -> recipe.ingredients),
					Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(recipe -> recipe.dimension),
					Codec.BOOL.optionalFieldOf("required", false).forGetter(recipe -> recipe.required))
			.apply(instance, GatedItemRecipe::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, GatedItemRecipe> STREAM_CODEC = StreamCodec.composite(
			Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()),
			recipe -> recipe.ingredients,
			ResourceKey.streamCodec(Registries.DIMENSION),
			recipe -> recipe.dimension,
			ByteBufCodecs.BOOL,
			recipe -> recipe.required,
			GatedItemRecipe::new
	);
	public static final RecipeSerializer<GatedItemRecipe> SERIALIZER = new RecipeSerializer<>(CODEC, STREAM_CODEC);

	protected final List<Ingredient> ingredients;
	protected final ResourceKey<Level> dimension;
	protected final boolean required;
	@Nullable
	private PlacementInfo placementInfo;

	public GatedItemRecipe(List<Ingredient> ingredientNonNullList, ResourceKey<Level> dimension, boolean required) {
		this.ingredients = ingredientNonNullList;
		this.dimension = dimension;
		this.required = required;
	}

	@Override
	public RecipeSerializer<GatedItemRecipe> getSerializer() {
		return GatedRecipes.GATED_ITEM_SERIALIZER.get();
	}

	@Override
	public RecipeType<GatedItemRecipe> getType() {
		return GatedRecipes.GATED_ITEM_TYPE.get();
	}

	@Override
	public PlacementInfo placementInfo() {
		if (this.placementInfo == null) {
			this.placementInfo = PlacementInfo.create(this.ingredients);
		}

		return this.placementInfo;
	}

	@Override
	public RecipeBookCategory recipeBookCategory() {
		return null;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	@Override
	public boolean showNotification() {
		return false;
	}

	@Override
	public boolean matches(CraftingInput input, Level level) {
		// input is always empty when checked!
		//Unused please use getMatchingStacks instead as it will give you the stacks that match the recipe
		return true;
	}

	public List<ItemStack> getMatchingStacks(List<ItemStack> stacks, GatedItemRecipe recipe) {
		List<ItemStack> matchingStacks = new ArrayList<>();
		if (ingredients.stream().anyMatch(Ingredient::isEmpty)) {
			DimensionalItemGate.LOGGER.error("Gated ItemRecipe has empty ingredient");
			return matchingStacks;
		}

		for (ItemStack stack : stacks) {
			if (ingredients.stream().anyMatch(ingredient -> ingredient.test(stack))) {
				matchingStacks.add(stack);
			}
			ResourceHandler<ItemResource> handler = stack.getCapability(Capabilities.Item.ITEM, null);
			if (handler != null) {
				for (int i = 0; i < handler.size(); i++) {
					ItemStack slotStack = handler.getResource(i).toStack();
					if (ingredients.stream().anyMatch(ingredient -> ingredient.test(slotStack))) {
						matchingStacks.add(slotStack);
					}
				}
			}
		}
		return matchingStacks;
	}

	public List<ItemStack> getMissingStacks(List<ItemStack> stacks, GatedItemRecipe recipe) {
		List<ItemStack> missingStacks = new ArrayList<>();
		if (ingredients.stream().anyMatch(Ingredient::isEmpty)) {
			DimensionalItemGate.LOGGER.error("Gated ItemRecipe has empty ingredient");
			return List.of(Items.BARRIER.getDefaultInstance());
		}

		List<Ingredient> missingIngredients = new ArrayList<>(ingredients);
		if (!stacks.isEmpty()) {
			missingIngredients.removeIf(ingredient -> {
				if (stacks.stream().anyMatch(ingredient)) return true;
				for (ItemStack stack : stacks) {
					ResourceHandler<ItemResource> handler = stack.getCapability(Capabilities.Item.ITEM, null);
					if (handler != null) {
						for (int i = 0; i < handler.size(); i++) {
							ItemStack slotStack = handler.getResource(i).toStack();
							if (ingredient.test(slotStack)) {
								return true;
							}
						}
					}
				}

				return false;
			});
		}

		if (!missingIngredients.isEmpty()) {
			missingIngredients.forEach(ingredient -> {
				if (ingredient.getValues().size() > 0) {
					missingStacks.add(new ItemStack(ingredient.getValues().get(0)));
				}
			});
			return missingStacks;
		}

		return missingStacks;
	}

	@Override
	public ItemStack assemble(CraftingInput input) {
		return ItemStack.EMPTY;
	}

	@Override
	public String group() {
		return "";
	}

	public ResourceKey<Level> getDimension() {
		return this.dimension;
	}

	public boolean isRequired() {
		return required;
	}
}
