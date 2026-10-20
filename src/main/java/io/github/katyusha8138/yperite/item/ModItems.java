package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.block.ModBlocks;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.GasTypes;
import io.github.katyusha8138.yperite.item.armor.GeoArmorItem;
import io.github.katyusha8138.yperite.item.armor.WarArmorMaterials;
import io.github.katyusha8138.yperite.protection.FilterKind;
import io.github.katyusha8138.yperite.protection.MaskType;
import io.github.katyusha8138.yperite.protection.SuitProtection;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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

    // ---- First World War equipment
    public static final RegistryObject<Item> STAHLHELM_M1916 = helmet("stahlhelm_m1916", 0.08);
    public static final RegistryObject<Item> BRODIE_MK1 = helmet("brodie_mk1", 0.06);
    public static final RegistryObject<Item> ADRIAN_M15 = helmet("adrian_m15", 0.05);
    public static final RegistryObject<Item> BERNDORFER = helmet("berndorfer", 0.07);
    public static final RegistryObject<Item> ADRIAN_M16_RUSSIAN = helmet("adrian_m16_russian", 0.05);

    /**
     * Field uniforms of the five armies: tunic, trousers, and what is worn on the feet, in that order per army.
     * Most wore puttees over ankle boots; the Russians tucked breeches into jackboots.
     */
    public static final List<RegistryObject<Item>> UNIFORMS = new ArrayList<>();

    static {
        Map<String, String> feet = new LinkedHashMap<>();
        feet.put("british", "puttees");
        feet.put("french", "puttees");
        feet.put("german", "puttees");
        feet.put("austrian", "puttees");
        feet.put("russian", "boots");
        feet.forEach((army, onFeet) -> {
            UNIFORMS.add(uniform(army + "_tunic", ArmorItem.Type.CHESTPLATE));
            UNIFORMS.add(uniform(army + "_trousers", ArmorItem.Type.LEGGINGS));
            UNIFORMS.add(uniform(army + "_" + onFeet, ArmorItem.Type.BOOTS));
        });
    }

    /** German Sappenpanzer: heavy plates, good against bullets, slow to walk in. */
    public static final RegistryObject<Item> SAPPENPANZER = ITEMS.register("sappenpanzer",
            () -> new GeoArmorItem(WarArmorMaterials.TRENCH_ARMOR, ArmorItem.Type.CHESTPLATE, new Item.Properties(),
                    "sappenpanzer", 0.30, 0.10).standOff(1.35f, 0.5f));
    /** British Dayfield body shield: lighter plates sewn into cloth. */
    public static final RegistryObject<Item> DAYFIELD = ITEMS.register("dayfield",
            () -> new GeoArmorItem(WarArmorMaterials.BODY_SHIELD, ArmorItem.Type.CHESTPLATE, new Item.Properties(),
                    "dayfield", 0.18, 0.04).standOff(0.75f, 0.75f));
    /** Body armour: worn in the chest slot, or over a tunic in the Curios body slot. */
    public static final List<RegistryObject<Item>> BODY_ARMOUR = List.of(SAPPENPANZER, DAYFIELD);

    // ---- protection
    public static final RegistryObject<Item> GAS_MASK = mask(MaskType.MODERN);
    public static final RegistryObject<Item> SMALL_BOX_RESPIRATOR = mask(MaskType.SMALL_BOX_RESPIRATOR);
    public static final RegistryObject<Item> M2_MASK = mask(MaskType.M2);
    public static final RegistryObject<Item> LEDERSCHUTZMASKE = mask(MaskType.LEDERSCHUTZMASKE);
    public static final RegistryObject<Item> AUSTRIAN_GAS_MASK = mask(MaskType.AUSTRIAN);
    public static final RegistryObject<Item> ZELINSKY_KUMMANT_MASK = mask(MaskType.ZELINSKY);
    public static final RegistryObject<Item> GAS_FILTER_GENERAL = ITEMS.register("gas_filter_general",
            () -> new GasFilterItem(FilterKind.GENERAL, new Item.Properties().stacksTo(16)));
    public static final RegistryObject<Item> GAS_FILTER_SPECIAL = ITEMS.register("gas_filter_special",
            () -> new GasFilterItem(FilterKind.SPECIAL, new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)));
    public static final RegistryObject<Item> SEALED_FABRIC = ITEMS.register("sealed_fabric", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PROTECTIVE_HOOD = ITEMS.register("protective_hood",
            () -> new ProtectiveArmorItem(ArmorItem.Type.HELMET, SuitProtection.Part.HOOD, "protective_hood", new Item.Properties()));
    /** The jacket stands off the chest like a tunic (model pixels; the models are built with the same numbers). */
    public static final RegistryObject<Item> PROTECTIVE_JACKET = ITEMS.register("protective_jacket",
            () -> new ProtectiveArmorItem(ArmorItem.Type.CHESTPLATE, SuitProtection.Part.JACKET, "protective_jacket", new Item.Properties())
                    .standOff(0.9f, 0.6f));
    public static final RegistryObject<Item> PROTECTIVE_TROUSERS = ITEMS.register("protective_trousers",
            () -> new ProtectiveArmorItem(ArmorItem.Type.LEGGINGS, SuitProtection.Part.TROUSERS, "protective_trousers", new Item.Properties()));
    public static final RegistryObject<Item> PROTECTIVE_BOOTS = ITEMS.register("protective_boots",
            () -> new ProtectiveArmorItem(ArmorItem.Type.BOOTS, SuitProtection.Part.BOOTS, "protective_boots", new Item.Properties()));

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

    /** Every pattern of gas mask, in the order of {@link MaskType}. */
    public static final List<RegistryObject<Item>> MASKS = List.of(GAS_MASK, SMALL_BOX_RESPIRATOR, M2_MASK, LEDERSCHUTZMASKE, AUSTRIAN_GAS_MASK,
            ZELINSKY_KUMMANT_MASK);

    /** A gas mask of the given pattern; the pattern also names the item. */
    private static RegistryObject<Item> mask(MaskType type) {
        return ITEMS.register(type.model(), () -> new GasMaskItem(type, new Item.Properties()));
    }

    /** A piece of uniform. Tunics stand off the chest, so that body armour and haversacks go over them. */
    private static RegistryObject<Item> uniform(String name, ArmorItem.Type type) {
        return ITEMS.register(name, () -> {
            GeoArmorItem item = new GeoArmorItem(WarArmorMaterials.WOOL_UNIFORM, type, new Item.Properties(), name, 0.0, 0.0);
            return type == ArmorItem.Type.CHESTPLATE ? item.standOff(TUNIC_FRONT, TUNIC_BACK) : item;
        });
    }

    /** How far a tunic stands off the chest (model pixels; the models are built with the same numbers). */
    private static final float TUNIC_FRONT = 1.0f;
    private static final float TUNIC_BACK = 0.8f;

    /** A First World War steel helmet; bullet resistance is Superb Warfare's (0..1) when new. */
    private static RegistryObject<Item> helmet(String name, double bulletResistance) {
        return ITEMS.register(name, () -> new GeoArmorItem(WarArmorMaterials.STEEL_HELMET, ArmorItem.Type.HELMET,
                new Item.Properties(), name, bulletResistance, 0.0));
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
