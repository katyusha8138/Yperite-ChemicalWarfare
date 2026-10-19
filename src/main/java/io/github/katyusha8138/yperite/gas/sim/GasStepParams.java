package io.github.katyusha8138.yperite.gas.sim;

/**
 * Per-step inputs that do not depend on the kind of gas.
 *
 * <p>How open a cell is to the air comes from its sky light: {@code openness = (skyLight / 15) ^ shelterExponent}.
 * Wind and the extra thinning outdoors both scale with it, so they fade out gradually towards the inside of
 * buildings and caves.
 *
 * @param dtSeconds        game time covered by this step, in seconds
 * @param minConcentration cells below this are removed
 * @param maxCells         soft cap on the number of gas cells in one layer
 * @param windX            wind speed along x in blocks per second (east is positive)
 * @param windZ            wind speed along z in blocks per second (south is positive)
 * @param shelterExponent  how quickly openness falls off with falling sky light; 1 is linear, larger is steeper
 * @param openAirDilution  factor on spreading and decay in fully open air (1 = same as indoors)
 */
public record GasStepParams(
        float dtSeconds,
        float minConcentration,
        int maxCells,
        float windX,
        float windZ,
        float shelterExponent,
        float openAirDilution) {

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
        if (shelterExponent <= 0f) {
            throw new IllegalArgumentException("shelterExponent must be positive: " + shelterExponent);
        }
        if (openAirDilution < 1f) {
            throw new IllegalArgumentException("openAirDilution must be at least 1: " + openAirDilution);
        }
    }

    public boolean hasWind() {
        return windX != 0f || windZ != 0f;
    }
}
