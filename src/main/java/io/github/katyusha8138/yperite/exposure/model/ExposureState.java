package io.github.katyusha8138.yperite.exposure.model;

import java.util.Arrays;

/**
 * What one living body has taken in, per gas category. Nothing here depends on Minecraft.
 *
 * <ul>
 *   <li>{@code skin}: contamination still on the skin; it keeps seeping into the body until it is washed off</li>
 *   <li>{@code latent}: dose that is in the body but has not taken effect yet</li>
 *   <li>{@code active}: dose that is taking effect now; this decides the symptoms</li>
 *   <li>{@code aftermath}: 0..1 lasting damage; it only ever grows (until the body dies and a new one starts)</li>
 * </ul>
 */
public final class ExposureState {
    public final float[] skin;
    public final float[] latent;
    public final float[] active;
    public final float[] aftermath;

    public ExposureState(int categories) {
        this.skin = new float[categories];
        this.latent = new float[categories];
        this.active = new float[categories];
        this.aftermath = new float[categories];
    }

    public int categories() {
        return active.length;
    }

    /** True when nothing is left: no dose anywhere and no lasting damage. */
    public boolean isClean() {
        for (int i = 0; i < active.length; i++) {
            if (skin[i] != 0f || latent[i] != 0f || active[i] != 0f || aftermath[i] != 0f) {
                return false;
            }
        }
        return true;
    }

    /** Removes everything, lasting damage included. */
    public void clear() {
        Arrays.fill(skin, 0f);
        Arrays.fill(latent, 0f);
        Arrays.fill(active, 0f);
        Arrays.fill(aftermath, 0f);
    }

    public void copyFrom(ExposureState other) {
        if (other.categories() != categories()) {
            throw new IllegalArgumentException("category counts differ: " + other.categories() + " vs " + categories());
        }
        System.arraycopy(other.skin, 0, skin, 0, skin.length);
        System.arraycopy(other.latent, 0, latent, 0, latent.length);
        System.arraycopy(other.active, 0, active, 0, active.length);
        System.arraycopy(other.aftermath, 0, aftermath, 0, aftermath.length);
    }
}
