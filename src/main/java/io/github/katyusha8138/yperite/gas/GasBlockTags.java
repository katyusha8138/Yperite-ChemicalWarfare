package io.github.katyusha8138.yperite.gas;

import io.github.katyusha8138.yperite.YperiteMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** Tags that override how a block treats gas. Data packs can add to them. */
public final class GasBlockTags {
    /** Gas passes through even though the block has a full-cube collision shape (for example leaves). */
    public static final TagKey<Block> GAS_PERMEABLE = create("gas_permeable");
    /** Gas never passes, even if the collision shape has gaps. */
    public static final TagKey<Block> GAS_BLOCKING = create("gas_blocking");

    private GasBlockTags() {
    }

    @SuppressWarnings("removal")
    private static TagKey<Block> create(String name) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(YperiteMod.MODID, name));
    }
}
