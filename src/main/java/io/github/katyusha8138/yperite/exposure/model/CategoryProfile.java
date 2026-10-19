package io.github.katyusha8138.yperite.exposure.model;

/**
 * How one category of gas takes effect on a body. All three are time constants in seconds (the time in which
 * the process has covered about 63% of its way).
 *
 * @param onsetSeconds    time for absorbed dose to turn into symptoms; long means delayed onset
 * @param recoverySeconds time for symptoms to fade once no more dose arrives
 * @param absorbSeconds   time for dose on the skin to be taken into the body
 */
public record CategoryProfile(float onsetSeconds, float recoverySeconds, float absorbSeconds) {
    public CategoryProfile {
        if (onsetSeconds <= 0f || recoverySeconds <= 0f || absorbSeconds <= 0f) {
            throw new IllegalArgumentException("time constants must be positive");
        }
    }
}
