package com.mrbysco.dimensiongate.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mrbysco.dimensiongate.DimensionalItemGate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.Registry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class GatedItemRecipe implements Recipe<Container> {
	protected final ResourceLocation id;
	protected final NonNullList<Ingredient> ingredients;
	protected final ResourceKey<Level> dimension;
	protected final ItemStack result = ItemStack.EMPTY;
	protected final boolean required;

	public GatedItemRecipe(ResourceLocation id, NonNullList<Ingredient> ingredientNonNullList, ResourceKey<Level> dimension, boolean required) {
		this.id = id;
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
	public boolean matches(Container container, Level level) {
		//Unused please use getMatchingStacks instead as it will give you the stacks that match the recipe

		return true;
	}

	@SuppressWarnings("DataFlowIssue")
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
			if (stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
				IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
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

		for (Ingredient ingredient : getIngredients()) {
			if (stacks.stream().noneMatch(stack -> {
				List<ItemStack> stackList = new ArrayList<>();
				stackList.add(stack);
				IItemHandler handler = stack.getCapability(ForgeCapabilities.ITEM_HANDLER).orElse(null);
				if (stack.getCapability(ForgeCapabilities.ITEM_HANDLER).isPresent()) {
					for (int i = 0; i < handler.getSlots(); i++) {
						ItemStack slotStack = handler.getStackInSlot(i);
						if (!slotStack.isEmpty()) {
							stackList.add(slotStack);
						}
					}
				}

				return stackList.stream().noneMatch(ingredient);
			})) {
				missingStacks.add(ingredient.getItems()[0]);
			}
		}

		return missingStacks;
	}

	@Override
	public ItemStack assemble(Container inventory) {
		return getResultItem().copy();
	}

	@Override
	public ItemStack getResultItem() {
		return result;
	}

	@Override
	public NonNullList<Ingredient> getIngredients() {
		return ingredients;
	}

	@Override
	public ResourceLocation getId() {
		return id;
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

	public static class GatedItemRecipeSerializer implements RecipeSerializer<GatedItemRecipe> {
		@Override
		public GatedItemRecipe fromJson(ResourceLocation recipeId, JsonObject jsonObject) {
			NonNullList<Ingredient> nonnulllist = itemsFromJson(GsonHelper.getAsJsonArray(jsonObject, "ingredients"));
			if (nonnulllist.isEmpty()) {
				throw new JsonParseException("No ingredients for shapeless recipe");
			} else {
				String dimension = GsonHelper.getAsString(jsonObject, "dimension");
				ResourceLocation dimensionLocation = ResourceLocation.tryParse(dimension);
				if (dimensionLocation == null) {
					throw new JsonParseException("Dimension" + dimension + " defined in Item Gate Recipe is not valid!");
				}
				ResourceKey<Level> dimensionKey = ResourceKey.create(Registry.DIMENSION_REGISTRY, dimensionLocation);
				boolean required = GsonHelper.getAsBoolean(jsonObject, "required", false);
				return new GatedItemRecipe(recipeId, nonnulllist, dimensionKey, required);
			}
		}

		private static NonNullList<Ingredient> itemsFromJson(JsonArray jsonArray) {
			NonNullList<Ingredient> nonnulllist = NonNullList.create();

			for (int i = 0; i < jsonArray.size(); ++i) {
				Ingredient ingredient = Ingredient.fromJson(jsonArray.get(i));
				nonnulllist.add(ingredient);
			}

			return nonnulllist;
		}

		@Nullable
		@Override
		public GatedItemRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
			int i = buffer.readVarInt();
			NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);
			for (int j = 0; j < nonnulllist.size(); ++j) {
				nonnulllist.set(j, Ingredient.fromNetwork(buffer));
			}

			ResourceKey<Level> dimension = buffer.readResourceKey(Registry.DIMENSION_REGISTRY);
			boolean required = buffer.readBoolean();
			return new GatedItemRecipe(recipeId, nonnulllist, dimension, required);
		}

		@Override
		public void toNetwork(FriendlyByteBuf buffer, GatedItemRecipe recipe) {
			buffer.writeVarInt(recipe.ingredients.size());
			for (Ingredient ingredient : recipe.ingredients) {
				ingredient.toNetwork(buffer);
			}

			buffer.writeResourceKey(recipe.dimension);
			buffer.writeBoolean(recipe.required);
		}
	}
}
