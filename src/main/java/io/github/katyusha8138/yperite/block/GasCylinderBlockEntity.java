package io.github.katyusha8138.yperite.block;

import io.github.katyusha8138.yperite.config.YperiteConfig;
import io.github.katyusha8138.yperite.gas.GasManager;
import io.github.katyusha8138.yperite.gas.GasType;
import io.github.katyusha8138.yperite.gas.GasTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/** What a gas cylinder holds, and the slow release while its valve is open. */
public class GasCylinderBlockEntity extends BlockEntity {
    public static final String TAG_GAS = "Gas";
    public static final String TAG_AMOUNT = "Amount";
    /** Ticks between two releases. */
    public static final int INTERVAL = 10;
    /** Gas goes into the outlet cell alone: a larger sphere would reach past a wall that the outlet touches. */
    private static final float OUTLET_RADIUS = 0f;
    private static final Direction[] OUTLETS = {Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    @Nullable
    private ResourceLocation gasId;
    private float amount;

    public GasCylinderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.GAS_CYLINDER.get(), pos, state);
    }

    @Nullable
    public GasType gas() {
        return gasId == null ? null : GasTypes.REGISTRY.get().getValue(gasId);
    }

    public float amount() {
        return amount;
    }

    public void setContents(@Nullable GasType type, float amount) {
        ResourceLocation before = gasId;
        this.gasId = type == null ? null : type.id();
        this.amount = type == null ? 0f : Math.max(0f, amount);
        setChanged();
        // clients draw the cylinder's band in the colour of its gas: tell them when the gas changes
        if (level != null && !level.isClientSide && !Objects.equals(before, gasId)) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** The fill level, 0..1, relative to the configured capacity. */
    public float fraction() {
        double capacity = YperiteConfig.CYLINDER_CAPACITY.get();
        return (float) Math.min(1.0, amount / capacity);
    }

    /** The redstone comparator reading: 0 only when empty, otherwise 1..15. */
    public int comparatorSignal() {
        return gas() == null || amount <= 0f ? 0 : Math.max(1, (int) Math.ceil(fraction() * 15.0));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, GasCylinderBlockEntity cylinder) {
        if (!(level instanceof ServerLevel serverLevel) || !state.getValue(GasCylinderBlock.OPEN)) {
            return;
        }
        if (serverLevel.getGameTime() % INTERVAL != 0) {
            return;
        }
        cylinder.release(serverLevel, pos);
    }

    private void release(ServerLevel level, BlockPos pos) {
        GasType type = gas();
        if (type == null || amount <= 0f) {
            return;
        }
        float wanted = Math.min(amount, (float) (YperiteConfig.CYLINDER_RATE.get() * INTERVAL / 20.0));
        float placed = 0f;
        BlockPos outlet = pos;
        for (Direction side : OUTLETS) {
            outlet = pos.relative(side);
            placed = GasManager.release(level, type, outlet, OUTLET_RADIUS, wanted);
            if (placed > 0f) {
                break;
            }
        }
        if (placed <= 0f) {
            return; // every side is walled in: the valve is open but nothing can come out
        }
        amount = Math.max(0f, amount - placed);
        setChanged();
        level.sendParticles(ParticleTypes.CLOUD, outlet.getX() + 0.5, outlet.getY() + 0.5, outlet.getZ() + 0.5, 3, 0.15, 0.15, 0.15, 0.02);
        level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.3f, 1.8f);
    }

    // ------------------------------------------------------------------ what clients know: only the gas, for the band

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        if (gasId != null) {
            tag.putString(TAG_GAS, gasId.toString());
        }
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        gasId = tag.contains(TAG_GAS) ? ResourceLocation.tryParse(tag.getString(TAG_GAS)) : null;
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet) {
        CompoundTag tag = packet.getTag();
        handleUpdateTag(tag == null ? new CompoundTag() : tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if (gasId != null) {
            tag.putString(TAG_GAS, gasId.toString());
        }
        tag.putFloat(TAG_AMOUNT, amount);
    }

    @Override
    @SuppressWarnings("removal")
    public void load(CompoundTag tag) {
        super.load(tag);
        gasId = tag.contains(TAG_GAS) ? ResourceLocation.tryParse(tag.getString(TAG_GAS)) : null;
        amount = tag.getFloat(TAG_AMOUNT);
    }
}
