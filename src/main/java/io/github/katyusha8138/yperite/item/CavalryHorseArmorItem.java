package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.item.HorseArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Cavalry horse armour: a quilted caparison with a steel chanfron, a little stronger than iron horse armour, and
 * light enough that a horse wearing it runs {@link #SPEED_BONUS} faster than its own speed (vanilla horses are slow).
 * The speed is a modifier on the horse's movement speed, put on when the armour is put on and taken off with it.
 */
@Mod.EventBusSubscriber(modid = YperiteMod.MODID)
public class CavalryHorseArmorItem extends HorseArmorItem {
    /** Protection, as iron horse armour's 5 and golden's 7. */
    public static final int PROTECTION = 7;
    /** Share added to the horse's own movement speed. */
    public static final double SPEED_BONUS = 0.35;
    public static final UUID SPEED_ID = UUID.fromString("7e3c1d2a-4b5f-4c8e-9a61-2f0d3b7c5e19");

    @SuppressWarnings("removal")
    public CavalryHorseArmorItem(Properties properties) {
        super(PROTECTION, new ResourceLocation(YperiteMod.MODID, "textures/entity/horse/armor/cavalry_horse_armor.png"), properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.yperite.cavalry_horse_armor", Math.round(SPEED_BONUS * 100)).withStyle(ChatFormatting.BLUE));
    }

    /** Puts the horse's speed bonus on or takes it off as its armour changes (also when a horse is loaded). */
    @SubscribeEvent
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getSlot() != EquipmentSlot.CHEST || !(event.getEntity() instanceof AbstractHorse horse) || horse.level().isClientSide) {
            return;
        }
        update(horse);
    }

    /** Makes the horse's movement speed carry the bonus exactly when it wears this armour. */
    public static void update(AbstractHorse horse) {
        AttributeInstance speed = horse.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        boolean wearing = horse.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof CavalryHorseArmorItem;
        boolean has = speed.getModifier(SPEED_ID) != null;
        if (wearing && !has) {
            speed.addTransientModifier(new AttributeModifier(SPEED_ID, "Cavalry horse armour", SPEED_BONUS, AttributeModifier.Operation.MULTIPLY_BASE));
        } else if (!wearing && has) {
            speed.removeModifier(SPEED_ID);
        }
    }
}
