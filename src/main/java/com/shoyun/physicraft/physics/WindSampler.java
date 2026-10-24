package com.shoyun.physicraft.physics;

import com.jme3.math.Vector3f;

/**
 * Acceleration contributed by one wind source, in blocks per second squared.
 * A future wind-source block can register another sampler on {@link WindField}.
 */
@FunctionalInterface
public interface WindSampler {
    void sample(float x, float y, float z, Vector3f store);
}
