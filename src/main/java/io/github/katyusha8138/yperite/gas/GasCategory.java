package io.github.katyusha8138.yperite.gas;

import net.minecraft.network.chat.Component;

/**
 * Game-side grouping of gases by how they behave, not a chemical classification.
 * Effects, protection and detection are tied to the category.
 */
public enum GasCategory {
    /** Light and short-lived; blurs vision. */
    LACRIMATORY("lacrimatory"),
    /** Heavy and short-lived; sustained damage. */
    ASPHYXIANT("asphyxiant"),
    /** Heavy and very persistent; reaches the skin, so a mask alone is not enough. */
    VESICANT("vesicant"),
    /** Light; hampers movement and attacks with little direct damage. */
    INCAPACITANT("incapacitant"),
    /** The most dangerous; tiny doses are serious and only dedicated filters help. */
    HIGH_POTENCY("high_potency");

    private final String id;

    GasCategory(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return Component.translatable("gas_category.yperite." + id);
    }
}
