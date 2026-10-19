package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS = DeferredRegister.create(Registries.RECIPE_SERIALIZER, YperiteMod.MODID);

    public static final RegistryObject<RecipeSerializer<MaskFilterRecipe>> MASK_FILTER =
            SERIALIZERS.register("crafting_gas_mask_filter", () -> new SimpleCraftingRecipeSerializer<>(MaskFilterRecipe::new));

    private ModRecipes() {
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }
}
