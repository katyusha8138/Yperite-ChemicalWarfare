package io.github.katyusha8138.yperite.block;

import io.github.katyusha8138.yperite.protection.DetectorLevel;
import io.github.katyusha8138.yperite.protection.GasScanner;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;

/**
 * Senses gas within a few blocks. While it senses any, it sounds an alarm and gives a redstone signal that grows
 * with the level (3, 7, 11 or 15). It works with scheduled block ticks, so it needs no block entity.
 */
public class GasAlarmBlock extends Block {
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    /** Cells in every direction that the alarm senses. */
    public static final int RADIUS = 3;
    /** Ticks between two checks. */
    public static final int INTERVAL = 10;

    public GasAlarmBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL)
                .strength(2.0f, 6.0f)
                .sound(SoundType.METAL)
                .lightLevel(state -> state.getValue(POWER) > 0 ? 7 : 0));
        registerDefaultState(stateDefinition.any().setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWER);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        if (!level.isClientSide && !oldState.is(this)) {
            level.scheduleTick(pos, this, INTERVAL);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int detected = GasScanner.scan(level, pos, RADIUS, null).maxLevel();
        int power = DetectorLevel.redstone(detected);
        if (state.getValue(POWER) != power) {
            level.setBlock(pos, state.setValue(POWER, power), Block.UPDATE_ALL);
        }
        if (detected > 0) {
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.5f, 0.6f + 0.2f * detected);
        }
        level.scheduleTick(pos, this, INTERVAL);
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }
}
