package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.block.ModBlocks;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.protection.FilterKind;
import io.github.katyusha8138.yperite.protection.SuitProtection;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ModItems {
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, YperiteMod.MODID);

    // ---- delivery
    /** One hand grenade per built-in gas, by item name. */
    public static final Map<String, RegistryObject<Item>> GRENADES = new LinkedHashMap<>();

    static {
        registerGrenade("tear_gas_grenade", GasTypes.TEAR_GAS);
        registerGrenade("chlorine_grenade", GasTypes.CHLORINE);
        registerGrenade("blood_gas_grenade", GasTypes.BLOOD_GAS);
        registerGrenade("yperite_grenade", GasTypes.YPERITE);
        registerGrenade("nerve_gas_grenade", GasTypes.NERVE_GAS);
        registerGrenade("blue_cross_grenade", GasTypes.BLUE_CROSS);
    }

    public static final RegistryObject<Item> GAS_CYLINDER = ITEMS.register("gas_cylinder",
            () -> new GasCylinderItem(ModBlocks.GAS_CYLINDER.get(), new Item.Properties().stacksTo(1)));

    // ---- protection
    public static final RegistryObject<Item> GAS_MASK = ITEMS.register("gas_mask", () -> new GasMaskItem(new Item.Properties()));
    public static final RegistryObject<Item> GAS_FILTER_GENERAL = ITEMS.register("gas_filter_general",
            () -> new GasFilterItem(FilterKind.GENERAL, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> GAS_FILTER_SPECIAL = ITEMS.register("gas_filter_special",
            () -> new GasFilterItem(FilterKind.SPECIAL, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SEALED_FABRIC = ITEMS.register("sealed_fabric", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PROTECTIVE_HOOD = ITEMS.register("protective_hood",
            () -> new ProtectiveArmorItem(ArmorItem.Type.HELMET, SuitProtection.Part.HOOD, new Item.Properties()));
    public static final RegistryObject<Item> PROTECTIVE_JACKET = ITEMS.register("protective_jacket",
            () -> new ProtectiveArmorItem(ArmorItem.Type.CHESTPLATE, SuitProtection.Part.JACKET, new Item.Properties()));
    public static final RegistryObject<Item> PROTECTIVE_TROUSERS = ITEMS.register("protective_trousers",
            () -> new ProtectiveArmorItem(ArmorItem.Type.LEGGINGS, SuitProtection.Part.TROUSERS, new Item.Properties()));
    public static final RegistryObject<Item> PROTECTIVE_BOOTS = ITEMS.register("protective_boots",
            () -> new ProtectiveArmorItem(ArmorItem.Type.BOOTS, SuitProtection.Part.BOOTS, new Item.Properties()));

    // ---- detection
    public static final RegistryObject<Item> GAS_DETECTOR = ITEMS.register("gas_detector",
            () -> new GasDetectorItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> WIND_VANE = ITEMS.register("wind_vane",
            () -> new WindVaneItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GAS_ALARM = ITEMS.register("gas_alarm",
            () -> new BlockItem(ModBlocks.GAS_ALARM.get(), new Item.Properties()));

    // ---- decontamination and treatment
    public static final RegistryObject<Item> DECON_SOLUTION = ITEMS.register("decon_solution",
            () -> new DeconSolutionItem(new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> DECON_POWDER = ITEMS.register("decon_powder",
            () -> new DeconPowderItem(new Item.Properties()));
    public static final RegistryObject<Item> MEDICINE_GENERAL = ITEMS.register("medicine_general",
            () -> new MedicineItem(MedicineItem.Kind.GENERAL, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> ANTIDOTE_NERVE = ITEMS.register("antidote_nerve",
            () -> new MedicineItem(MedicineItem.Kind.NERVE_ANTIDOTE, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> OINTMENT_BLISTER = ITEMS.register("ointment_blister",
            () -> new MedicineItem(MedicineItem.Kind.BLISTER_OINTMENT, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));

    private ModItems() {
    }

    private static void registerGrenade(String name, RegistryObject<GasType> gas) {
        GRENADES.put(name, ITEMS.register(name, () -> new GasGrenadeItem(gas, new Item.Properties().stacksTo(16))));
    }

    /** The grenade that stands in for the entity's item before it is given a real one. */
    public static Item firstGrenade() {
        return GRENADES.values().iterator().next().get();
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
    }
}
