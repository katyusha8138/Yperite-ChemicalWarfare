package io.github.katyusha8138.yperite.gas.sim;

/**
 * Per-step inputs that do not depend on the kind of gas.
 *
 * @param dtSeconds        game time covered by this step, in seconds
 * @param minConcentration cells below this are removed
 * @param maxCells         soft cap on the number of gas cells in one layer
 * @param windX            wind vector (x); its length is the wind strength, normally within 0..1
 * @param windZ            wind vector (z)
 * @param windGain         how strongly the wind skews the spread
 */
public record GasStepParams(
        float dtSeconds,
        float minConcentration,
        int maxCells,
        float windX,
        float windZ,
        float windGain) {

    public GasStepParams {
        if (dtSeconds <= 0f) {
            throw new IllegalArgumentException("dtSeconds must be positive: " + dtSeconds);
        }
        if (minConcentration < 0f) {
            throw new IllegalArgumentException("minConcentration must not be negative: " + minConcentration);
        }
        if (maxCells < 1) {
            throw new IllegalArgumentException("maxCells must be at least 1: " + maxCells);
        }
    }

    public boolean hasWind() {
        return windGain > 0f && (windX != 0f || windZ != 0f);
    }
}
