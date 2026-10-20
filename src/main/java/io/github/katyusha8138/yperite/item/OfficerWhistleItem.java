package io.github.katyusha8138.yperite.item;

import io.github.katyusha8138.yperite.sound.ModSounds;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * An officer's trench whistle. Use it to blow one long blast: it is put to the mouth like a goat horn, and the blast
 * carries far ({@link ModSounds#WHISTLE_RANGE} blocks), at its natural loudness nearby and fainter with distance.
 * It follows the one who blows it, and sculk sensors hear it.
 */
public class OfficerWhistleItem extends Item {
    /** How long it is held to the mouth, in ticks (about as long as the blast). */
    public static final int BLAST_TICKS = 25;
    /** Ticks before it can be blown again. */
    public static final int COOLDOWN_TICKS = 30;

    public OfficerWhistleItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        // played on both sides, as the goat horn does: the blower hears it at once, everyone else from the server
        level.playSound(player, player, ModSounds.OFFICER_WHISTLE.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        level.gameEvent(GameEvent.INSTRUMENT_PLAY, player.position(), GameEvent.Context.of(player));
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return BLAST_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }
}
