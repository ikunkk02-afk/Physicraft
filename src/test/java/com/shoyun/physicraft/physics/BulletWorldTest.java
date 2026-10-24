package com.shoyun.physicraft.physics;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.ManifoldPoints;
import com.jme3.bullet.collision.PersistentManifolds;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Vector3f;

class BulletWorldTest {
    @Test
    void windDriftsAFallingBox() {
        PhysicsSpace space = newSpace();
        PhysicsRigidBody ground = staticGround(space);
        PhysicsRigidBody box = dynamicBox(space, 1.0F, 0.0F, 6.0F, 0.0F);
        Vector3f wind = new Vector3f(6.0F, 0.0F, 0.0F);
        Vector3f location = new Vector3f();
        for (int step = 0; step < 80; step++) {
            wind.set(6.0F, 0.0F, 0.0F).multLocal(box.getMass());
            box.activate(true);
            box.applyCentralForce(wind);
            space.update(0.05F, 0);
        }
        box.getPhysicsLocation(location);
        space.removeCollisionObject(box);
        space.removeCollisionObject(ground);
        space.destroy();
        assertTrue(location.y < 5.0F, "box should fall, y=" + location.y);
        assertTrue(location.x > 0.25F, "wind should push +X, x=" + location.x);
    }

    @Test
    void hardLandingProducesBreakableImpulse() {
        PhysicsSpace space = newSpace();
        PhysicsRigidBody ground = staticGround(space);
        float mass = 0.5F + 0.5F * 0.45F;
        PhysicsRigidBody lower = dynamicBox(space, mass, 0.0F, 0.5F, 0.0F);
        PhysicsRigidBody upper = dynamicBox(space, mass, 0.0F, 8.5F, 0.0F);
        Vector3f previousLower = new Vector3f();
        Vector3f previousUpper = new Vector3f();
        float strongest = 0.0F;
        for (int step = 0; step < 160; step++) {
            lower.getLinearVelocity(previousLower);
            upper.getLinearVelocity(previousUpper);
            space.update(0.05F, 0);
            strongest = Math.max(strongest, dynamicImpulse(space, lower, upper, previousLower, previousUpper));
        }
        space.removeCollisionObject(upper);
        space.removeCollisionObject(lower);
        space.removeCollisionObject(ground);
        space.destroy();

        float required = ImpactRules.requiredImpulse(4.0F, 0.5F);
        assertTrue(strongest >= required,
                "dirt-like fall from 8 blocks should exceed break impulse " + required + " but was " + strongest);
    }

    private static float dynamicImpulse(PhysicsSpace space, PhysicsRigidBody lower, PhysicsRigidBody upper,
            Vector3f previousLower, Vector3f previousUpper) {
        float strongest = 0.0F;
        for (long manifold : space.listManifoldIds()) {
            PhysicsCollisionObject objectA = PhysicsCollisionObject.findInstance(PersistentManifolds.getBodyAId(manifold));
            PhysicsCollisionObject objectB = PhysicsCollisionObject.findInstance(PersistentManifolds.getBodyBId(manifold));
            boolean pair = (objectA == lower && objectB == upper) || (objectA == upper && objectB == lower);
            if (!pair) {
                continue;
            }
            float impulse = 0.0F;
            for (long point : PersistentManifolds.listPointIds(manifold)) {
                if (ManifoldPoints.getDistance1(point) <= 0.02F) {
                    impulse += Math.abs(ManifoldPoints.getAppliedImpulse(point));
                }
            }
            float relX = previousLower.x - previousUpper.x;
            float relY = previousLower.y - previousUpper.y;
            float relZ = previousLower.z - previousUpper.z;
            float relativeSpeed = (float) Math.sqrt(relX * relX + relY * relY + relZ * relZ);
            if (relativeSpeed >= 3.5F) {
                strongest = Math.max(strongest, impulse);
            }
        }
        return strongest;
    }

    private static PhysicsSpace newSpace() {
        assertTrue(BulletNative.ensureLoaded(), BulletNative.failureReason());
        PhysicsSpace space = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
        space.setGravity(new Vector3f(0.0F, -20.0F, 0.0F));
        return space;
    }

    private static PhysicsRigidBody staticGround(PhysicsSpace space) {
        BoxCollisionShape shape = new BoxCollisionShape(40.0F, PhysicsShapes.HALF_EXTENT, 40.0F);
        shape.setMargin(PhysicsShapes.MARGIN);
        PhysicsRigidBody ground = new PhysicsRigidBody(shape, 0.0F);
        ground.setPhysicsLocation(new Vector3f(0.0F, -0.5F, 0.0F));
        ground.setFriction(0.9F);
        ground.setRestitution(0.0F);
        space.addCollisionObject(ground);
        return ground;
    }

    private static PhysicsRigidBody dynamicBox(PhysicsSpace space, float mass, float x, float y, float z) {
        PhysicsRigidBody body = new PhysicsRigidBody(PhysicsShapes.unitBlock(), mass);
        body.setPhysicsLocation(new Vector3f(x, y, z));
        body.setFriction(0.55F);
        body.setRestitution(0.02F);
        body.setDamping(0.12F, 0.35F);
        body.setCcdMotionThreshold(0.25F);
        body.setCcdSweptSphereRadius(0.42F);
        space.addCollisionObject(body);
        return body;
    }
}
