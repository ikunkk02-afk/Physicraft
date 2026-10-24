package com.shoyun.physicraft.physics;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jme3.bullet.PhysicsSpace;
import com.jme3.bullet.collision.ManifoldPoints;
import com.jme3.bullet.collision.PersistentManifolds;
import com.jme3.bullet.collision.PhysicsCollisionObject;
import com.jme3.bullet.objects.PhysicsRigidBody;
import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.shoyun.physicraft.Config;
import com.shoyun.physicraft.Physicraft;
import com.shoyun.physicraft.entity.PhysicsBlockEntity;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server-side Bullet world for one {@link ServerLevel}.
 * Terrain is approximated by static unit cubes around each dynamic body.
 */
public final class LevelPhysics {
    private static final Map<ServerLevel, LevelPhysics> WORLDS = new IdentityHashMap<>();
    private static final float TICK_SECONDS = 0.05F;

    private final ServerLevel level;
    private final WindField windField = new WindField();
    private final Map<UUID, SimBody> bodies = new java.util.HashMap<>();
    private final Long2ObjectOpenHashMap<PhysicsRigidBody> terrain = new Long2ObjectOpenHashMap<>();
    private final LongOpenHashSet neededTerrain = new LongOpenHashSet();
    private final List<PhysicsBlockEntity> breakQueue = new ArrayList<>();
    private final Vector3f gravity = new Vector3f(0.0F, -20.0F, 0.0F);
    private final Vector3f location = new Vector3f();
    private final Vector3f linear = new Vector3f();
    private final Vector3f angular = new Vector3f();
    private final Vector3f wind = new Vector3f();
    private final Vector3f otherLinear = new Vector3f();
    private final Quaternion rotation = new Quaternion();
    private PhysicsSpace space;
    private boolean closed;

    private LevelPhysics(ServerLevel level) {
        this.level = level;
    }

    public static LevelPhysics get(ServerLevel level) {
        return WORLDS.computeIfAbsent(level, LevelPhysics::new);
    }

    public static void discard(ServerLevel level) {
        LevelPhysics physics = WORLDS.remove(level);
        if (physics != null) {
            physics.close();
        }
    }

    public WindField windField() {
        return this.windField;
    }

    public boolean isReady() {
        return !this.closed && (this.space != null || BulletNative.ensureLoaded());
    }

    public int dynamicCount() {
        return this.bodies.size();
    }

    public void add(PhysicsBlockEntity entity) {
        if (this.closed || entity.level() != this.level || entity.isRemoved()) {
            return;
        }
        if (!this.ensureSpace() || this.bodies.containsKey(entity.getUUID())) {
            return;
        }
        PhysicsRigidBody body = new PhysicsRigidBody(PhysicsShapes.unitBlock(), entity.getMass());
        body.setUserObject(entity.getUUID());
        body.setSleepingThresholds(0.15F, 0.2F);
        body.setCcdMotionThreshold(0.25F);
        body.setCcdSweptSphereRadius(0.42F);
        this.applyMaterial(body);
        this.location.set(entity.centerX(), entity.centerY(), entity.centerZ());
        body.setPhysicsLocation(this.location);
        body.setPhysicsRotation(entity.rotation(this.rotation));
        body.setLinearVelocity(this.linear.set(entity.linearX(), entity.linearY(), entity.linearZ()));
        body.setAngularVelocity(this.angular.set(entity.angularX(), entity.angularY(), entity.angularZ()));
        this.space.addCollisionObject(body);
        this.bodies.put(entity.getUUID(), new SimBody(entity, body));
    }

    public void remove(PhysicsBlockEntity entity) {
        SimBody sim = this.bodies.remove(entity.getUUID());
        if (sim == null || this.space == null) {
            return;
        }
        this.snapshot(sim);
        this.space.removeCollisionObject(sim.body);
    }

    public void preTick() {
        if (this.closed || this.bodies.isEmpty()) {
            if (this.space != null && !this.terrain.isEmpty()) {
                this.clearTerrain();
            }
            return;
        }
        if (!this.ensureSpace()) {
            return;
        }
        this.gravity.set(0.0F, -Config.GRAVITY.get().floatValue(), 0.0F);
        this.space.setGravity(this.gravity);
        for (SimBody sim : this.bodies.values()) {
            sim.entity.tickImpactGrace();
            this.applyMaterial(sim.body);
            sim.body.setGravity(this.gravity);
        }
        this.refreshTerrain();
        this.applyWind();
        // Capture speed before the solver removes it. Resting contacts stay slow; impacts do not.
        for (SimBody sim : this.bodies.values()) {
            sim.body.getLinearVelocity(sim.previousVelocity);
        }
        // One 50 ms step matches a Minecraft tick. maxSteps 0 disables substeps.
        this.space.update(TICK_SECONDS, 0);
        this.collectImpacts();
        for (SimBody sim : this.bodies.values()) {
            this.snapshot(sim);
        }
        if (!this.breakQueue.isEmpty()) {
            List<PhysicsBlockEntity> pending = new ArrayList<>(this.breakQueue);
            this.breakQueue.clear();
            for (int i = 0; i < pending.size(); i++) {
                pending.get(i).shatter();
            }
        }
    }

    private void applyMaterial(PhysicsRigidBody body) {
        body.setFriction(Config.FRICTION.get().floatValue());
        body.setRestitution(Config.RESTITUTION.get().floatValue());
        body.setDamping(Config.LINEAR_DAMPING.get().floatValue(), Config.ANGULAR_DAMPING.get().floatValue());
        body.setRollingFriction(0.04F);
        body.setSpinningFriction(0.02F);
    }

    private void applyWind() {
        for (SimBody sim : this.bodies.values()) {
            sim.body.getPhysicsLocation(this.location);
            this.windField.accelerationAt(this.location.x, this.location.y, this.location.z, this.wind);
            if (this.wind.lengthSquared() <= 1.0E-6F) {
                continue;
            }
            if (!sim.body.isActive()) {
                sim.body.activate(true);
            }
            this.wind.multLocal(sim.body.getMass());
            sim.body.applyCentralForce(this.wind);
        }
    }

    private void collectImpacts() {
        float baseImpulse = Config.BREAK_IMPULSE.get().floatValue();
        float minSpeed = Config.MIN_IMPACT_SPEED.get().floatValue();
        long[] manifolds = this.space.listManifoldIds();
        for (long manifold : manifolds) {
            PhysicsCollisionObject objectA = PhysicsCollisionObject.findInstance(PersistentManifolds.getBodyAId(manifold));
            PhysicsCollisionObject objectB = PhysicsCollisionObject.findInstance(PersistentManifolds.getBodyBId(manifold));
            if (!(objectA instanceof PhysicsRigidBody bodyA) || !(objectB instanceof PhysicsRigidBody bodyB)) {
                continue;
            }
            if (bodyA.getMass() <= 0.0F || bodyB.getMass() <= 0.0F) {
                continue;
            }
            if (!(bodyA.getUserObject() instanceof UUID idA) || !(bodyB.getUserObject() instanceof UUID idB)) {
                continue;
            }
            SimBody simA = this.bodies.get(idA);
            SimBody simB = this.bodies.get(idB);
            if (simA == null || simB == null) {
                continue;
            }
            if (simA.entity.hasImpactGrace() || simB.entity.hasImpactGrace()) {
                continue;
            }
            float impulse = 0.0F;
            long[] points = PersistentManifolds.listPointIds(manifold);
            for (long point : points) {
                if (ManifoldPoints.getDistance1(point) > 0.02F) {
                    continue;
                }
                impulse += Math.abs(ManifoldPoints.getAppliedImpulse(point));
            }
            if (impulse <= 0.0F) {
                continue;
            }
            this.linear.set(simA.previousVelocity);
            this.otherLinear.set(simB.previousVelocity);
            float relX = this.linear.x - this.otherLinear.x;
            float relY = this.linear.y - this.otherLinear.y;
            float relZ = this.linear.z - this.otherLinear.z;
            float relativeSpeed = (float) Math.sqrt(relX * relX + relY * relY + relZ * relZ);
            int victim = ImpactRules.selectVictim(
                    simA.entity.getHardness(),
                    simB.entity.getHardness(),
                    this.linear.length(),
                    this.otherLinear.length(),
                    impulse,
                    relativeSpeed,
                    baseImpulse,
                    minSpeed);
            PhysicsBlockEntity broken = victim == 0 ? simA.entity : victim == 1 ? simB.entity : null;
            if (broken != null && !this.breakQueue.contains(broken)) {
                Physicraft.LOGGER.debug("Physics impact impulse {} speed {} breaks {}", impulse, relativeSpeed, broken.getBlockState());
                this.breakQueue.add(broken);
            }
        }
    }

    private void refreshTerrain() {
        this.neededTerrain.clear();
        int baseRadius = Config.TERRAIN_RADIUS.get();
        float terrainFriction = Config.TERRAIN_FRICTION.get().floatValue();
        int minY = this.level.getMinBuildHeight();
        int maxY = this.level.getMaxBuildHeight() - 1;
        for (SimBody sim : this.bodies.values()) {
            sim.body.getPhysicsLocation(this.location);
            sim.body.getLinearVelocity(this.linear);
            int radius = Math.min(8, baseRadius + (int) (this.linear.length() * 0.05F));
            int minX = (int) Math.floor(this.location.x) - radius;
            int maxX = (int) Math.floor(this.location.x) + radius;
            int minZ = (int) Math.floor(this.location.z) - radius;
            int maxZ = (int) Math.floor(this.location.z) + radius;
            int y0 = Math.max(minY, (int) Math.floor(this.location.y) - radius);
            int y1 = Math.min(maxY, (int) Math.floor(this.location.y) + radius);
            for (int x = minX; x <= maxX; x++) {
                for (int y = y0; y <= y1; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        long key = BlockPos.asLong(x, y, z);
                        if (this.neededTerrain.contains(key)) {
                            continue;
                        }
                        if (this.occupiesTerrain(x, y, z)) {
                            this.neededTerrain.add(key);
                        }
                    }
                }
            }
        }

        Iterator<Long2ObjectMap.Entry<PhysicsRigidBody>> iterator = this.terrain.long2ObjectEntrySet().iterator();
        while (iterator.hasNext()) {
            Long2ObjectMap.Entry<PhysicsRigidBody> entry = iterator.next();
            if (!this.neededTerrain.contains(entry.getLongKey())) {
                this.space.removeCollisionObject(entry.getValue());
                iterator.remove();
            }
        }

        LongIterator needed = this.neededTerrain.iterator();
        while (needed.hasNext()) {
            long key = needed.nextLong();
            PhysicsRigidBody existing = this.terrain.get(key);
            if (existing != null) {
                existing.setFriction(terrainFriction);
                continue;
            }
            int x = BlockPos.getX(key);
            int y = BlockPos.getY(key);
            int z = BlockPos.getZ(key);
            PhysicsRigidBody body = new PhysicsRigidBody(PhysicsShapes.unitBlock(), 0.0F);
            body.setFriction(terrainFriction);
            body.setRestitution(0.0F);
            body.setPhysicsLocation(this.location.set(x + 0.5F, y + 0.5F, z + 0.5F));
            this.space.addCollisionObject(body);
            this.terrain.put(key, body);
        }
    }

    private boolean occupiesTerrain(int x, int y, int z) {
        BlockPos pos = new BlockPos(x, y, z);
        if (!this.level.isLoaded(pos)) {
            return false;
        }
        BlockState state = this.level.getBlockState(pos);
        return !state.getCollisionShape(this.level, pos).isEmpty();
    }

    private void snapshot(SimBody sim) {
        sim.body.getPhysicsLocation(this.location);
        sim.body.getLinearVelocity(this.linear);
        sim.body.getAngularVelocity(this.angular);
        sim.body.getPhysicsRotation(this.rotation);
        sim.entity.applySimulationState(this.location, this.rotation, this.linear, this.angular);
    }

    private void clearTerrain() {
        for (PhysicsRigidBody body : this.terrain.values()) {
            this.space.removeCollisionObject(body);
        }
        this.terrain.clear();
    }

    private boolean ensureSpace() {
        if (this.space != null) {
            return true;
        }
        if (!BulletNative.ensureLoaded()) {
            Physicraft.LOGGER.error("Skipping physics world because Libbulletjme is unavailable: {}", BulletNative.failureReason());
            return false;
        }
        this.space = new PhysicsSpace(PhysicsSpace.BroadphaseType.DBVT);
        this.gravity.set(0.0F, -Config.GRAVITY.get().floatValue(), 0.0F);
        this.space.setGravity(this.gravity);
        return true;
    }

    private void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        if (this.space != null) {
            this.clearTerrain();
            for (SimBody sim : this.bodies.values()) {
                this.space.removeCollisionObject(sim.body);
            }
            this.bodies.clear();
            this.space.destroy();
            this.space = null;
        }
    }

    private static final class SimBody {
        private final PhysicsBlockEntity entity;
        private final PhysicsRigidBody body;
        private final Vector3f previousVelocity = new Vector3f();

        private SimBody(PhysicsBlockEntity entity, PhysicsRigidBody body) {
            this.entity = entity;
            this.body = body;
        }
    }
}
