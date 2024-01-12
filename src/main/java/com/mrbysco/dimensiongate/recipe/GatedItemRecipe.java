package com.mrbysco.dimensiongate.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mrbysco.dimensiongate.DimensionalItemGate;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GatedItemRecipe implements Recipe<Container> {
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
	public boolean matches(Container container, Level level) {
		//Unused please use getMatchingStacks instead as it will give you the stacks that match the recipe

		return false;
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
		missingIngredients.removeIf(ingredient -> stacks.stream().anyMatch(ingredient));
		if (!missingIngredients.isEmpty()) {
			missingIngredients.forEach(ingredient -> missingStacks.add(ingredient.getItems()[0]));
			return missingStacks;
		}

		return missingStacks;
	}

	@Override
	public ItemStack assemble(Container inventory, RegistryAccess registryAccess) {
		return getResultItem(registryAccess).copy();
	}

	@Override
	public ItemStack getResultItem(RegistryAccess registryAccess) {
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

		private static final Codec<GatedItemRecipe> CODEC = RecordCodecBuilder.create(
				instance -> instance.group(
								Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients").flatXmap((array) -> {
									Ingredient[] aingredient = array.toArray(Ingredient[]::new);
									if (aingredient.length == 0) {
										return DataResult.error(() -> "No items in Gated Item Recipe");
									} else {
										return DataResult.success(NonNullList.of(Ingredient.EMPTY, aingredient));
									}
								}, DataResult::success).forGetter(recipe -> recipe.ingredients),
								Level.RESOURCE_KEY_CODEC.fieldOf("dimension").forGetter(recipe -> recipe.dimension),
								ExtraCodecs.strictOptionalField(Codec.BOOL, "required", false).forGetter(recipe -> recipe.required)
						)
						.apply(instance, GatedItemRecipe::new)
		);

		@Override
		public Codec<GatedItemRecipe> codec() {
			return CODEC;
		}

		@Nullable
		@Override
		public GatedItemRecipe fromNetwork(FriendlyByteBuf buffer) {
			int i = buffer.readVarInt();
			NonNullList<Ingredient> nonnulllist = NonNullList.withSize(i, Ingredient.EMPTY);
			for (int j = 0; j < nonnulllist.size(); ++j) {
				nonnulllist.set(j, Ingredient.fromNetwork(buffer));
			}

			ResourceKey<Level> dimension = buffer.readResourceKey(Registries.DIMENSION);
			boolean required = buffer.readBoolean();
			return new GatedItemRecipe(nonnulllist, dimension, required);
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
