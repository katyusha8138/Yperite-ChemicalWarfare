package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * The gas registry and the built-in gases.
 *
 * <p>The numbers are tuning values for gameplay only. "Rates per second" are the share lost per second;
 * toxicity is in exposure units per second at concentration 1.
 */
@SuppressWarnings("removal") // ResourceLocation constructors are used on purpose: they exist on every Forge 47.x
public final class GasTypes {
    public static final ResourceKey<Registry<GasType>> REGISTRY_KEY =
            ResourceKey.createRegistryKey(new ResourceLocation(YperiteMod.MODID, "gas_type"));

    private static final DeferredRegister<GasType> GAS_TYPES = DeferredRegister.create(REGISTRY_KEY, YperiteMod.MODID);

    public static final Supplier<IForgeRegistry<GasType>> REGISTRY = GAS_TYPES.makeRegistry(() -> new RegistryBuilder<GasType>());

    /** Riot control: light, short-lived, blurs vision. */
    public static final RegistryObject<GasType> TEAR_GAS = GAS_TYPES.register("tear_gas", () ->
            GasType.builder(GasCategory.RIOT_CONTROL)
                    .color(0xE9E7C8).opacity(0.6f)
                    .buoyancy(0.35f).diffusion(0.10f)
                    .decayPerSecond(0.06f)
                    .toxicity(3.0f, 0f, 0f)
                    .rainWashoutPerSecond(0.15f).waterAbsorbPerSecond(0.30f)
                    .build());

    /** Choking: heavy, short-lived, sinks into low ground. */
    public static final RegistryObject<GasType> CHLORINE = GAS_TYPES.register("chlorine", () ->
            GasType.builder(GasCategory.CHOKING)
                    .color(0xB9D94A).opacity(0.7f)
                    .buoyancy(-0.7f).diffusion(0.09f)
                    .decayPerSecond(0.04f)
                    .toxicity(0.7f, 0f, 0f)
                    .rainWashoutPerSecond(0.10f).waterAbsorbPerSecond(0.40f)
                    .build());

    /** Blister: heavy, hardly spreads, and leaves residue on the ground for a very long time. */
    public static final RegistryObject<GasType> YPERITE = GAS_TYPES.register("yperite", () ->
            GasType.builder(GasCategory.BLISTER)
                    .color(0xC9A24B).opacity(0.35f)
                    .buoyancy(-0.5f).diffusion(0.05f)
                    .decayPerSecond(0.005f)
                    .toxicity(0.15f, 0.5f, 0.12f)
                    .settlePerSecond(0.08f).residueDecayPerSecond(0.001f).evaporationPerSecond(0.004f)
                    .rainWashoutPerSecond(0.02f).waterAbsorbPerSecond(0.05f)
                    .build());

    /** Blood: thins out fast in the open, so it is mainly a danger in closed rooms and tunnels. */
    public static final RegistryObject<GasType> BLOOD_GAS = GAS_TYPES.register("blood_gas", () ->
            GasType.builder(GasCategory.BLOOD)
                    .color(0xD9C7C7).opacity(0.2f)
                    .buoyancy(0.20f).diffusion(0.11f)
                    .decayPerSecond(0.07f)
                    .toxicity(0.6f, 0f, 0f)
                    .rainWashoutPerSecond(0.05f).waterAbsorbPerSecond(0.15f)
                    .build());

    /** Incapacitant: light, medium duration, hampers movement. */
    public static final RegistryObject<GasType> BLUE_CROSS = GAS_TYPES.register("blue_cross", () ->
            GasType.builder(GasCategory.INCAPACITANT)
                    .color(0x6FA8DC).opacity(0.5f)
                    .buoyancy(0.10f).diffusion(0.08f)
                    .decayPerSecond(0.025f)
                    .toxicity(1.2f, 0f, 0f)
                    .rainWashoutPerSecond(0.08f).waterAbsorbPerSecond(0.20f)
                    .build());

    /** Nerve: nearly invisible, so it is only noticed with a detector. */
    public static final RegistryObject<GasType> NERVE_GAS = GAS_TYPES.register("nerve_gas", () ->
            GasType.builder(GasCategory.NERVE)
                    .color(0xE0E0F8).opacity(0.12f)
                    .buoyancy(0.0f).diffusion(0.07f)
                    .decayPerSecond(0.015f)
                    .toxicity(2.5f, 0.8f, 0.3f)
                    .settlePerSecond(0.02f).residueDecayPerSecond(0.002f).evaporationPerSecond(0.003f)
                    .rainWashoutPerSecond(0.05f).waterAbsorbPerSecond(0.10f)
                    .build());

    /** Every gas that this mod adds, in a fixed order. Items made per gas (grenades, chemical rounds) follow it. */
    public static final java.util.List<RegistryObject<GasType>> BUILT_IN =
            java.util.List.of(TEAR_GAS, CHLORINE, BLOOD_GAS, YPERITE, NERVE_GAS, BLUE_CROSS);

    private GasTypes() {
    }

    public static void register(IEventBus modBus) {
        GAS_TYPES.register(modBus);
    }
}
