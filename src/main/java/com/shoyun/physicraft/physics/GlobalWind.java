package com.shoyun.physicraft.physics;

import com.jme3.math.Vector3f;
import com.shoyun.physicraft.Config;

/** Constant wind from the common config. */
public final class GlobalWind implements WindSampler {
    public static final GlobalWind INSTANCE = new GlobalWind();

    private GlobalWind() {
    }

    @Override
    public void sample(float x, float y, float z, Vector3f store) {
        store.set(
                Config.WIND_X.get().floatValue(),
                Config.WIND_Y.get().floatValue(),
                Config.WIND_Z.get().floatValue());
    }
}
