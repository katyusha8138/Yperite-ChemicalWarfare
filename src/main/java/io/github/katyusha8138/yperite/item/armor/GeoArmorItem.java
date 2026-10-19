package io.github.katyusha8138.yperite.item.armor;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import io.github.katyusha8138.yperite.compat.superbwarfare.SbwAttributes;
import io.github.katyusha8138.yperite.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * A piece of armour drawn with a GeckoLib model ({@code geo/armor/<model>.geo.json} and
 * {@code textures/armor/<model>.png}). It can carry Superb Warfare's bullet resistance (only applied when that mod is
 * installed, and scaled down as the piece wears out, as Superb Warfare does) and a movement penalty.
 */
public class GeoArmorItem extends ArmorItem implements WornGeoItem {
    /** Per-slot ids so that pieces in different slots add up instead of replacing each other. */
    private static final UUID[] MODIFIER_IDS = {
            UUID.fromString("8f1b8a2e-55a4-4b52-9f0e-3c0a6f4b1d01"), UUID.fromString("8f1b8a2e-55a4-4b52-9f0e-3c0a6f4b1d02"),
            UUID.fromString("8f1b8a2e-55a4-4b52-9f0e-3c0a6f4b1d03"), UUID.fromString("8f1b8a2e-55a4-4b52-9f0e-3c0a6f4b1d04")};
    private static final UUID[] SPEED_IDS = {
            UUID.fromString("2c6d0b7e-1f3a-4e8b-a6b1-7d2e9c5a3f01"), UUID.fromString("2c6d0b7e-1f3a-4e8b-a6b1-7d2e9c5a3f02"),
            UUID.fromString("2c6d0b7e-1f3a-4e8b-a6b1-7d2e9c5a3f03"), UUID.fromString("2c6d0b7e-1f3a-4e8b-a6b1-7d2e9c5a3f04")};

    private final String model;
    private final double bulletResistance;
    private final double speedPenalty;
    private float standOffFront;
    private float standOffBack;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /**
     * @param model            name of the model and texture files
     * @param bulletResistance Superb Warfare bullet resistance when new (0 for none)
     * @param speedPenalty     share of walking speed lost while worn (0.1 is 10 %)
     */
    public GeoArmorItem(ArmorMaterial material, Type type, Properties properties, String model, double bulletResistance, double speedPenalty) {
        super(material, type, properties);
        this.model = model;
        this.bulletResistance = bulletResistance;
        this.speedPenalty = speedPenalty;
    }

    @Override
    public String model() {
        return model;
    }

    /** Sets how far this piece stands off the chest, front and back (see {@link WornGeoItem#standOffFront()}). */
    public GeoArmorItem standOff(float front, float back) {
        this.standOffFront = front;
        this.standOffBack = back;
        return this;
    }

    @Override
    public float standOffFront() {
        return standOffFront;
    }

    @Override
    public float standOffBack() {
        return standOffBack;
    }

    public double speedPenalty() {
        return speedPenalty;
    }

    public double bulletResistance() {
        return bulletResistance;
    }

    /** What is left of the bullet resistance as the piece wears out. */
    public double bulletResistance(ItemStack stack) {
        if (bulletResistance <= 0 || stack.getMaxDamage() <= 0) {
            return bulletResistance;
        }
        return bulletResistance * Math.max(0.0, 1.0 - (double) stack.getDamageValue() / stack.getMaxDamage());
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(EquipmentSlot slot, ItemStack stack) {
        Multimap<Attribute, AttributeModifier> base = super.getAttributeModifiers(slot, stack);
        if (slot != getEquipmentSlot()) {
            return base;
        }
        Attribute resistance = SbwAttributes.bulletResistance();
        boolean addResistance = resistance != null && bulletResistance > 0;
        if (!addResistance && speedPenalty <= 0) {
            return base;
        }
        Multimap<Attribute, AttributeModifier> map = HashMultimap.create(base);
        int index = getType().ordinal();
        if (addResistance) {
            map.put(resistance, new AttributeModifier(MODIFIER_IDS[index], "Yperite bullet resistance",
                    bulletResistance(stack), AttributeModifier.Operation.ADDITION));
        }
        if (speedPenalty > 0) {
            map.put(Attributes.MOVEMENT_SPEED, new AttributeModifier(SPEED_IDS[index], "Yperite armour weight",
                    -speedPenalty, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        return map;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        if (bulletResistance > 0) {
            String key = SbwAttributes.bulletResistance() != null ? "tooltip.yperite.bullet_resistance" : "tooltip.yperite.bullet_resistance.inactive";
            tooltip.add(Component.translatable(key, Math.round(bulletResistance(stack) * 100)).withStyle(ChatFormatting.BLUE));
        }
        if (ModItems.BODY_ARMOUR.stream().anyMatch(armour -> armour.get() == this)) {
            boolean curios = net.minecraftforge.fml.ModList.get().isLoaded("curios");
            tooltip.add(Component.translatable(curios ? "tooltip.yperite.body_armour.curios" : "tooltip.yperite.body_armour")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    // ------------------------------------------------------------------ GeckoLib

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HumanoidModel<?> renderer;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(LivingEntity entity, ItemStack stack, EquipmentSlot slot, HumanoidModel<?> original) {
                if (renderer == null) {
                    renderer = io.github.katyusha8138.yperite.client.armor.WornArmorRenderer.create();
                }
                io.github.katyusha8138.yperite.client.armor.WornArmorRenderer.prepare(renderer, entity, stack, slot, original);
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // static models: nothing to animate
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
