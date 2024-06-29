package com.mrbysco.dimensiongate.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrbysco.dimensiongate.DimensionalItemGate;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;

public class GatedItemRecipe implements Recipe<SingleRecipeInput> {
	protected final NonNullList<Ingredient> ingredients;
	protected final ResourceKey<Level> dimension;
	protected final ItemStack result = ItemStack.EMPTY;
	protected final boolean required;

	public GatedItemRecipe(NonNullList<Ingredient> ingredientNonNullList, ResourceKey<Level> dimension, boolean required) {
		this.ingredients = ingredientNonNullList;
		this.dimension = dimension;
		this.required = required;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return GatedRecipes.GATED_ITEM_SERIALIZER.get();
	}

	@Override
	public RecipeType<?> getType() {
		return GatedRecipes.GATED_ITEM_TYPE.get();
	}

	@Override
	public boolean matches(SingleRecipeInput input, Level level) {
		//Unused please use getMatchingStacks instead as it will give you the stacks that match the recipe

		return false;
	}

	public List<ItemStack> getMatchingStacks(List<ItemStack> stacks, GatedItemRecipe recipe) {
		List<ItemStack> matchingStacks = new ArrayList<>();
		if (getIngredients().stream().anyMatch(Ingredient::isEmpty)) {
			DimensionalItemGate.LOGGER.error("Gated ItemRecipe has empty ingredient");
			return matchingStacks;
		}

		for (ItemStack stack : stacks) {
			if (recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(stack))) {
				matchingStacks.add(stack);
			}
			IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
			if (handler != null) {
				for (int i = 0; i < handler.getSlots(); i++) {
					ItemStack slotStack = handler.getStackInSlot(i);
					if (recipe.getIngredients().stream().anyMatch(ingredient -> ingredient.test(slotStack))) {
						matchingStacks.add(slotStack);
					}
				}
			}
		}
		return matchingStacks;
	}

	public List<ItemStack> getMissingStacks(List<ItemStack> stacks, GatedItemRecipe recipe) {
		List<ItemStack> missingStacks = new ArrayList<>();
		if (getIngredients().stream().anyMatch(Ingredient::isEmpty)) {
			DimensionalItemGate.LOGGER.error("Gated ItemRecipe has empty ingredient");
			return List.of(Items.BARRIER.getDefaultInstance());
		}

		List<Ingredient> missingIngredients = new ArrayList<>(getIngredients());
		if (!stacks.isEmpty()) {
			missingIngredients.removeIf(ingredient -> {
				if (stacks.stream().anyMatch(ingredient)) return true;
				for (ItemStack stack : stacks) {
					IItemHandler handler = stack.getCapability(Capabilities.ItemHandler.ITEM);
					if (handler != null) {
						for (int i = 0; i < handler.getSlots(); i++) {
							ItemStack slotStack = handler.getStackInSlot(i);
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
			missingIngredients.forEach(ingredient -> missingStacks.add(ingredient.getItems()[0]));
			return missingStacks;
		}

		return missingStacks;
	}

	@Override
	public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider provider) {
		return getResultItem(provider).copy();
	}

	@Override
	public ItemStack getResultItem(HolderLookup.Provider provider) {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return ingredients;
	}

	public ResourceKey<Level> getDimension() {
		return this.dimension;
	}

	public boolean isRequired() {
		return required;
	}

	@Override
	public boolean canCraftInDimensions(int x, int y) {
		return false;
	}

	@Override
	public boolean isSpecial() {
		return true;
	}

	public static class Serializer implements RecipeSerializer<GatedItemRecipe> {

		private static final MapCodec<GatedItemRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap((array) -> {
			Ingredient[] aingredient = array.toArray(Ingredient[]::new);
			if (aingredient.length == 0) {
				return DataResult.error(() -> "No items in Gated Item Recipe");
			} else {
				return DataResult.success(NonNullList.of(Ingredient.EMPTY, aingredient));
			}
		}, DataResult::success).forGetter(recipe -> recipe.ingredients), Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(recipe -> recipe.dimension), Codec.BOOL.optionalFieldOf("required", false).forGetter(recipe -> recipe.required)).apply(instance, GatedItemRecipe::new));
		public static final StreamCodec<RegistryFriendlyByteBuf, GatedItemRecipe> STREAM_CODEC = StreamCodec.of(GatedItemRecipe.Serializer::toNetwork, GatedItemRecipe.Serializer::fromNetwork);

		@Override
		public MapCodec<GatedItemRecipe> codec() {
			return CODEC;
		}

		@Override
		public StreamCodec<RegistryFriendlyByteBuf, GatedItemRecipe> streamCodec() {
			return STREAM_CODEC;
		}

		private static GatedItemRecipe fromNetwork(RegistryFriendlyByteBuf buf) {
			int i = buf.readVarInt();
			NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);
			nonnulllist.replaceAll(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
			ResourceKey<Level> dimension = buf.readResourceKey(Registries.DIMENSION);
			boolean required = buf.readBoolean();
			return new GatedItemRecipe(nonnulllist, dimension, required);
		}

		private static void toNetwork(RegistryFriendlyByteBuf buf, GatedItemRecipe recipe) {
			buf.writeVarInt(recipe.ingredients.size());

			for (Ingredient ingredient : recipe.ingredients) {
				Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
			}

			buf.writeResourceKey(recipe.dimension);
			buf.writeBoolean(recipe.required);
		}
	}
}
