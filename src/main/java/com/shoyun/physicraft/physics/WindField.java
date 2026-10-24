package com.shoyun.physicraft.physics;

import java.util.ArrayList;
import java.util.List;

import com.jme3.math.Vector3f;

/**
 * Sums wind accelerations at a point. The global config wind is always present.
 */
public final class WindField {
    private final List<WindSampler> samplers = new ArrayList<>();
    private final Vector3f sample = new Vector3f();

    public WindField() {
        this.samplers.add(GlobalWind.INSTANCE);
    }

    public void addSampler(WindSampler sampler) {
        this.samplers.add(sampler);
    }

    public void accelerationAt(float x, float y, float z, Vector3f store) {
        store.set(0.0F, 0.0F, 0.0F);
        for (int i = 0; i < this.samplers.size(); i++) {
            this.samplers.get(i).sample(x, y, z, this.sample);
            store.addLocal(this.sample);
        }
    }
}
