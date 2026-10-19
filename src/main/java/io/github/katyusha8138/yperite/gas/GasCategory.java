package io.github.katyusha8138.yperite.gas;

import net.minecraft.network.chat.Component;

/**
 * Rough, game-side grouping of gases by the kind of trouble they cause. It is deliberately abstract:
 * effects, protection and detection are tied to the category, and nothing here models real toxicology.
 */
public enum GasCategory {
    /** Riot-control agents: short-lived; blurs vision and slows the player a little. */
    RIOT_CONTROL("riot_control"),
    /** Choking agents: heavy and short-lived; laboured breathing and steady damage. */
    CHOKING("choking"),
    /** Blood agents: dissipate quickly in the open but are deadly in enclosed spaces; wear the body down. */
    BLOOD("blood"),
    /** Blister agents: heavy and very persistent; act through the skin, so a mask alone is not enough. */
    BLISTER("blister"),
    /** Nerve agents: the most dangerous; movement and control break down, and serious cases come on fast. */
    NERVE("nerve"),
    /** Incapacitating agents: hamper movement and attacks with little direct damage. */
    INCAPACITANT("incapacitant");

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
