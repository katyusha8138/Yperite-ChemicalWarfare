package io.github.katyusha8138.yperite.compat.superbwarfare;

import com.atsuishio.superbwarfare.api.event.LoadingJsonEvent;
import com.atsuishio.superbwarfare.api.event.ProjectileHitEvent;
import com.atsuishio.superbwarfare.entity.projectile.MortarShellEntity;
import com.atsuishio.superbwarfare.entity.vehicle.MortarEntity;
import io.github.katyusha8138.yperite.YperiteMod;
import io.github.katyusha8138.yperite.gas.GasTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * The three places where this mod meets Superb Warfare, all through its public events:
 * its data loading (to add chemical ammunition to weapons), a mortar shell leaving the mortar (to mark it), and
 * any projectile hitting something (to release the gas of marked ones).
 */
public final class SbwEvents {
    /** Called on the Forge event bus when Superb Warfare loads a weapon or vehicle definition. */
    @SubscribeEvent
    public void onLoadingJson(LoadingJsonEvent event) {
        String patched = SbwDataPatcher.patch(event.getId(), event.getJsonStr(), gasIds(),
                spec(ChemicalRound.GRENADE_40MM), spec(ChemicalRound.LARGE_SHELL));
        if (patched != null) {
            event.setJsonStr(patched);
            YperiteMod.LOGGER.debug("Added chemical ammunition to {}", event.getId());
        }
    }

    @SubscribeEvent
    public void onHitBlock(ProjectileHitEvent.HitBlock event) {
        releaseAt(event.getProjectile(), event.getPos().relative(event.getFace()));
    }

    @SubscribeEvent
    public void onHitEntity(ProjectileHitEvent.HitEntity event) {
        releaseAt(event.getProjectile(), BlockPos.containing(event.getHitVec()));
    }

    /**
     * The mortar builds its shell and adds it to the world in one go, and only afterwards empties itself, so at the
     * moment the shell joins the world the mortar next to it still holds the chemical shell it is firing.
     */
    @SubscribeEvent
    public void onShellJoined(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getEntity() instanceof MortarShellEntity shell)
                || shell.getPersistentData().contains(ChemicalRound.TAG_GAS)) {
            return;
        }
        for (MortarEntity mortar : level.getEntitiesOfClass(MortarEntity.class, shell.getBoundingBox().inflate(2.5))) {
            ItemStack loaded = mortar.getItems().get(0);
            if (loaded.getItem() instanceof ChemicalMortarShellItem chemical) {
                ChemicalAmmo.mark(shell, ChemicalRound.MORTAR_SHELL, chemical.gas().id());
                shell.setDamage(ChemicalRound.MORTAR_SHELL.impactDamage());
                shell.setExplosionDamage(ChemicalRound.MORTAR_SHELL.explosionDamage());
                shell.setExplosionRadius(ChemicalRound.MORTAR_SHELL.explosionRadius());
                shell.setExplosionDestroy(false);
                return;
            }
        }
    }

    private static void releaseAt(Entity projectile, BlockPos cell) {
        if (projectile.level() instanceof ServerLevel level) {
            ChemicalAmmo.release(level, projectile, cell);
        }
    }

    private static java.util.List<String> gasIds() {
        return GasTypes.BUILT_IN.stream().map(gas -> gas.getId().toString()).toList();
    }

    private static SbwDataPatcher.Spec spec(ChemicalRound round) {
        return new SbwDataPatcher.Spec(round.itemPrefix(), round.dataName(), round.impactDamage(), round.explosionDamage(), round.explosionRadius());
    }
}
