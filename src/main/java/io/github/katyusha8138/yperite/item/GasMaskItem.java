package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.item.armor.WornGeoItem;
import io.github.katyusha8138.yperite.protection.FilterKind;
import io.github.katyusha8138.yperite.protection.MaskType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.List;
import java.util.function.Consumer;

/**
 * A gas mask. It sits in the helmet slot, or in the Curios "face" slot when Curios is installed. Its pattern
 * ({@link MaskType}) decides how well it seals and whether a filter can be loaded; a filter is loaded by crafting,
 * and the filter's kind and what is left of its charge live in the item's data. Masks with a built-in pad (the
 * French M2) start with it full and cannot be reloaded. The bar shows the charge.
 */
public class GasMaskItem extends ArmorItem implements WornGeoItem {
    private static final String TAG_FILTER = "Filter";
    private static final String TAG_KIND = "Kind";
    private static final String TAG_CHARGE = "Charge";

    private final MaskType type;
    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public GasMaskItem(MaskType type, Properties properties) {
        super(ModArmorMaterials.GAS_MASK, Type.HELMET, properties);
        this.type = type;
    }

    public MaskType type() {
        return type;
    }

    public static MaskType typeOf(ItemStack mask) {
        return mask.getItem() instanceof GasMaskItem item ? item.type : MaskType.MODERN;
    }

    // ------------------------------------------------------------------ the loaded filter

    @Nullable
    public static FilterKind filterKind(ItemStack mask) {
        CompoundTag filter = mask.getTagElement(TAG_FILTER);
        if (filter == null) {
            return typeOf(mask).builtIn();
        }
        return FilterKind.byId(filter.getString(TAG_KIND));
    }

    /** What is left of the filter's charge; 0 without a filter. A built-in pad that was never used is full. */
    public static float charge(ItemStack mask) {
        CompoundTag filter = mask.getTagElement(TAG_FILTER);
        if (filter == null) {
            FilterKind builtIn = typeOf(mask).builtIn();
            return builtIn == null ? 0f : builtIn.capacity();
        }
        return filter.getFloat(TAG_CHARGE);
    }

    /** Loads a fresh filter, replacing whatever was in the mask. Masks that take no filter are left as they are. */
    public static void load(ItemStack mask, FilterKind kind) {
        if (!(mask.getItem() instanceof GasMaskItem item) || !item.type.acceptsFilters()) {
            return;
        }
        CompoundTag filter = mask.getOrCreateTagElement(TAG_FILTER);
        filter.putString(TAG_KIND, kind.id());
        filter.putFloat(TAG_CHARGE, kind.capacity());
    }

    public static void setCharge(ItemStack mask, float charge) {
        CompoundTag filter = mask.getTagElement(TAG_FILTER);
        if (filter == null) {
            FilterKind builtIn = typeOf(mask).builtIn();
            if (builtIn == null) {
                return;
            }
            filter = mask.getOrCreateTagElement(TAG_FILTER);
            filter.putString(TAG_KIND, builtIn.id());
        }
        filter.putFloat(TAG_CHARGE, Math.max(0f, charge));
    }

    /** A mask with a fresh filter loaded, for the creative tab. */
    public static ItemStack withFilter(ItemStack mask, FilterKind kind) {
        ItemStack copy = mask.copy();
        load(copy, kind);
        return copy;
    }

    // ------------------------------------------------------------------ filter bar and tooltip

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return filterKind(stack) != null || super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        FilterKind kind = filterKind(stack);
        if (kind == null) {
            return super.getBarWidth(stack);
        }
        return Math.round(13f * Math.min(1f, charge(stack) / kind.capacity()));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        FilterKind kind = filterKind(stack);
        if (kind == null) {
            return super.getBarColor(stack);
        }
        return charge(stack) / kind.capacity() > 0.2f ? 0x3FA7D6 : 0xD64040;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        FilterKind kind = filterKind(stack);
        if (kind == null) {
            tooltip.add(Component.translatable("tooltip.yperite.gas_mask.no_filter").withStyle(ChatFormatting.RED));
        } else {
            int percent = Math.round(100f * Math.min(1f, charge(stack) / kind.capacity()));
            Component name = type.acceptsFilters() ? Component.translatable("item.yperite.gas_filter_" + kind.id())
                    : Component.translatable("tooltip.yperite.gas_mask.pad");
            tooltip.add(Component.translatable("tooltip.yperite.gas_mask.filter", name, percent).withStyle(ChatFormatting.GRAY));
            if (charge(stack) <= 0f) {
                tooltip.add(Component.translatable("tooltip.yperite.gas_mask.exhausted").withStyle(ChatFormatting.RED));
            }
        }
        if (type.weakAgainstArsenicalSmoke()) {
            tooltip.add(Component.translatable("tooltip.yperite.gas_mask.arsenical").withStyle(ChatFormatting.GOLD));
        }
        tooltip.add(Component.translatable(type.acceptsFilters() ? "tooltip.yperite.gas_mask.hint" : "tooltip.yperite.gas_mask.no_reload")
                .withStyle(ChatFormatting.DARK_GRAY));
    }

    // ------------------------------------------------------------------ GeckoLib

    /**
     * The texture the vanilla armour layer asks for. GeckoLib draws with its own; answering with the same file keeps
     * the game from looking for a flat armour texture that does not exist.
     */
    @Override
    public String getArmorTexture(ItemStack stack, net.minecraft.world.entity.Entity entity, EquipmentSlot slot, String type) {
        return io.github.katyusha8138.yperite.YperiteMod.MODID + ":textures/armor/" + model() + ".png";
    }

    @Override
    public String model() {
        return type.model();
    }

    @Override
    public boolean showsFilter(ItemStack stack) {
        return filterKind(stack) != null && charge(stack) > 0f;
    }

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
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
