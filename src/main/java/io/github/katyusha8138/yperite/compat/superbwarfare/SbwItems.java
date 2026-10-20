package io.github.katyusha8138.yperite.compat.superbwarfare;

import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Items that exist only with Superb Warfare, other than the chemical rounds ({@link ChemicalRound#ITEMS}), for the
 * creative tab. Nothing here touches Superb Warfare, so the class can be loaded without it (the list stays empty).
 */
public final class SbwItems {
    /** The stick grenade, once registered. */
    public static final List<Supplier<Item>> ITEMS = new ArrayList<>();

    private SbwItems() {
    }
}
