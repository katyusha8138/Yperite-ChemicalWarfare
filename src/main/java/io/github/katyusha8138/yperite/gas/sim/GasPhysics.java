package io.github.katyusha8138.yperite.gas.sim;

/**
 * Game parameters that decide how one kind of gas behaves. These are tuning numbers for the game,
 * not a model of real chemistry.
 *
 * <p>All "per second" values are the fraction lost (or moved) per second of game time.
 *
 * @param buoyancy              -1 (sinks hard) .. +1 (rises hard)
 * @param diffusion             base share of a cell's gas that moves to each side neighbour per step
 * @param decayPerSecond        gas lost to the air
 * @param settlePerSecond       gas at floor level that is deposited on the ground as residue
 * @param residueDecayPerSecond residue that disappears
 * @param evaporationPerSecond  residue that returns to the air as gas
 * @param rainWashoutPerSecond  extra gas lost while it rains on the cell
 * @param waterAbsorbPerSecond  extra gas lost while the cell is under water
 */
public record GasPhysics(
        float buoyancy,
        float diffusion,
        float decayPerSecond,
        float settlePerSecond,
        float residueDecayPerSecond,
        float evaporationPerSecond,
        float rainWashoutPerSecond,
        float waterAbsorbPerSecond) {

    public static final float MAX_DIFFUSION = 0.2f;

    public GasPhysics {
        if (buoyancy < -1f || buoyancy > 1f) {
            throw new IllegalArgumentException("buoyancy must be within [-1, 1]: " + buoyancy);
        }
        if (diffusion < 0f || diffusion > MAX_DIFFUSION) {
            throw new IllegalArgumentException("diffusion must be within [0, " + MAX_DIFFUSION + "]: " + diffusion);
        }
        requireFraction("decayPerSecond", decayPerSecond);
        requireFraction("settlePerSecond", settlePerSecond);
        requireFraction("residueDecayPerSecond", residueDecayPerSecond);
        requireFraction("evaporationPerSecond", evaporationPerSecond);
        requireFraction("rainWashoutPerSecond", rainWashoutPerSecond);
        requireFraction("waterAbsorbPerSecond", waterAbsorbPerSecond);
    }

    private static void requireFraction(String name, float value) {
        if (value < 0f || value > 1f) {
            throw new IllegalArgumentException(name + " must be within [0, 1]: " + value);
        }
    }
}
