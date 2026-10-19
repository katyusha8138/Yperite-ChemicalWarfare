package io.github.katyusha8138.yperite.protection.compat;

import io.github.katyusha8138.yperite.item.GasMaskItem;
import io.github.katyusha8138.yperite.item.ModItems;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.ModList;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotResult;

/**
 * The only place, with {@link BodyArmourCurio}, that touches the Curios API. Never call anything here but
 * {@link #isLoaded()} unless it returned true, and keep Curios types out of this class's method bodies where the
 * verifier would have to load them: then the mod also works without Curios.
 *
 * <p>Two slots are used: "face" for a gas mask under a helmet, and "body" for body armour over a tunic.
 */
public final class CuriosCompat {
    private CuriosCompat() {
    }

    public static boolean isLoaded() {
        return ModList.get().isLoaded("curios");
    }

    /** Body armour behaves in the Curios body slot as it does in the chest slot, and wears out by the hits it takes. */
    public static void init() {
        BodyArmourCurio.registerAll();
        MinecraftForge.EVENT_BUS.addListener(CuriosCompat::onHurt);
    }

    /** The gas mask worn in a Curios slot, or an empty stack. */
    public static ItemStack findMask(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(handler -> handler.findFirstCurio(stack -> stack.getItem() instanceof GasMaskItem))
                .map(SlotResult::stack)
                .orElse(ItemStack.EMPTY);
    }

    /** The body armour worn in the Curios body slot, or an empty stack. */
    public static ItemStack findBodyArmour(LivingEntity entity) {
        return bodyArmourSlot(entity).map(SlotResult::stack).orElse(ItemStack.EMPTY);
    }

    private static java.util.Optional<SlotResult> bodyArmourSlot(LivingEntity entity) {
        return CuriosApi.getCuriosInventory(entity).resolve()
                .flatMap(handler -> handler.findFirstCurio(stack -> ModItems.BODY_ARMOUR.stream().anyMatch(a -> a.get() == stack.getItem())));
    }

    /** Armour in the body slot takes wear from blows as it would in the chest slot: a quarter of the damage. */
    private static void onHurt(LivingHurtEvent event) {
        if (event.getSource().is(DamageTypeTags.BYPASSES_ARMOR) || event.getAmount() <= 0f) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide) {
            return;
        }
        bodyArmourSlot(entity).ifPresent(slot -> {
            int wear = Math.max(1, (int) (event.getAmount() / 4f));
            slot.stack().hurtAndBreak(wear, entity, broken -> CuriosApi.broadcastCurioBreakEvent(slot.slotContext()));
        });
    }
}
