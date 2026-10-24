package com.shoyun.physicraft.physics;

import com.jme3.bullet.collision.shapes.BoxCollisionShape;

/**
 * Full-cube collision used for both dynamic blocks and nearby terrain.
 * Bullet's margin sits outside the half extents, so the total size stays one block.
 */
public final class PhysicsShapes {
    public static final float MARGIN = 0.04F;
    public static final float HALF_EXTENT = 0.5F - MARGIN;

    private PhysicsShapes() {
    }

    public static BoxCollisionShape unitBlock() {
        BoxCollisionShape shape = new BoxCollisionShape(HALF_EXTENT);
        shape.setMargin(MARGIN);
        return shape;
    }
}
