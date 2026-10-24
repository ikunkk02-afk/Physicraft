package com.shoyun.physicraft.entity;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.jme3.math.Quaternion;
import com.jme3.math.Vector3f;
import com.shoyun.physicraft.Config;
import com.shoyun.physicraft.Physicraft;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.Vec3;

/**
 * A converted block simulated by Bullet on the server and rendered from the synced block state plus quaternion.
 */
public class PhysicsBlockEntity extends Entity {
    public static final int IMPACT_GRACE_TICKS = 5;

    private static final EntityDataAccessor<BlockState> DATA_BLOCK = SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.BLOCK_STATE);
    private static final EntityDataAccessor<Float> ROT_X = SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROT_Y = SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROT_Z = SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROT_W = SynchedEntityData.defineId(PhysicsBlockEntity.class, EntityDataSerializers.FLOAT);

    private float hardness = 1.0F;
    private float vx;
    private float vy;
    private float vz;
    private float avx;
    private float avy;
    private float avz;
    private float rotX;
    private float rotY;
    private float rotZ;
    private float rotW = 1.0F;
    private float prevRotX;
    private float prevRotY;
    private float prevRotZ;
    private float prevRotW = 1.0F;
    private int impactGrace;
    private boolean shattered;

    public PhysicsBlockEntity(EntityType<? extends PhysicsBlockEntity> type, Level level) {
        super(type, level);
        this.noPhysics = true;
        this.setNoGravity(true);
        this.blocksBuilding = true;
    }

    public static PhysicsBlockEntity converted(ServerLevel level, BlockPos pos, BlockState state) {
        PhysicsBlockEntity entity = new PhysicsBlockEntity(Physicraft.PHYSICS_BLOCK.get(), level);
        entity.setBlockState(state);
        entity.hardness = Math.max(0.0F, state.getDestroySpeed(level, pos));
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        entity.grantImpactGrace(IMPACT_GRACE_TICKS);
        return entity;
    }

    public static boolean canConvert(Level level, BlockPos pos, BlockState state) {
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        if (level.getBlockEntity(pos) != null) {
            return false;
        }
        return Block.isShapeFullBlock(state.getCollisionShape(level, pos));
    }

    public BlockState getBlockState() {
        return this.entityData.get(DATA_BLOCK);
    }

    public float getHardness() {
        return this.hardness;
    }

    public float getMass() {
        return Mth.clamp(0.5F + this.hardness * 0.45F, 0.5F, 40.0F);
    }

    public float centerX() {
        return (float) this.getX();
    }

    public float centerY() {
        return (float) (this.getY() + this.getBbHeight() * 0.5D);
    }

    public float centerZ() {
        return (float) this.getZ();
    }

    public float linearX() {
        return this.vx;
    }

    public float linearY() {
        return this.vy;
    }

    public float linearZ() {
        return this.vz;
    }

    public float angularX() {
        return this.avx;
    }

    public float angularY() {
        return this.avy;
    }

    public float angularZ() {
        return this.avz;
    }

    public Quaternion rotation(Quaternion store) {
        store.set(this.rotX, this.rotY, this.rotZ, this.rotW);
        return store;
    }

    public float renderRotX() {
        return this.rotX;
    }

    public float renderRotY() {
        return this.rotY;
    }

    public float renderRotZ() {
        return this.rotZ;
    }

    public float renderRotW() {
        return this.rotW;
    }

    public float prevRenderRotX() {
        return this.prevRotX;
    }

    public float prevRenderRotY() {
        return this.prevRotY;
    }

    public float prevRenderRotZ() {
        return this.prevRotZ;
    }

    public float prevRenderRotW() {
        return this.prevRotW;
    }

    public void grantImpactGrace(int ticks) {
        this.impactGrace = ticks;
    }

    public boolean hasImpactGrace() {
        return this.impactGrace > 0;
    }

    public void tickImpactGrace() {
        if (this.impactGrace > 0) {
            this.impactGrace--;
        }
    }

    public void applySimulationState(Vector3f center, Quaternion rotation, Vector3f linearVelocity, Vector3f angularVelocity) {
        this.setPos(center.x, center.y - this.getBbHeight() * 0.5D, center.z);
        this.setDeltaMovement(linearVelocity.x / 20.0D, linearVelocity.y / 20.0D, linearVelocity.z / 20.0D);
        this.vx = linearVelocity.x;
        this.vy = linearVelocity.y;
        this.vz = linearVelocity.z;
        this.avx = angularVelocity.x;
        this.avy = angularVelocity.y;
        this.avz = angularVelocity.z;
        this.rotX = rotation.getX();
        this.rotY = rotation.getY();
        this.rotZ = rotation.getZ();
        this.rotW = rotation.getW();
        this.syncRotation(this.rotX, this.rotY, this.rotZ, this.rotW);
    }

    public void shatter() {
        if (this.shattered || this.level().isClientSide()) {
            return;
        }
        this.shattered = true;
        ServerLevel server = (ServerLevel) this.level();
        BlockPos pos = this.centerBlockPos();
        BlockState state = this.getBlockState();
        server.levelEvent(2001, pos, Block.getId(state));
        SoundType sound = state.getSoundType(server, pos, null);
        server.playSound(null, pos, sound.getBreakSound(), SoundSource.BLOCKS, 1.0F, 1.0F);
        server.gameEvent(GameEvent.BLOCK_DESTROY, Vec3.atCenterOf(pos), GameEvent.Context.of(null, state));
        if (Config.BREAK_DROPS.get() && server.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOBLOCKDROPS)) {
            List<ItemStack> drops = Block.getDrops(state, server, pos, null, null, ItemStack.EMPTY);
            for (ItemStack drop : drops) {
                Block.popResource(server, pos, drop);
            }
        }
        this.discard();
    }

    @Override
    public void tick() {
        this.prevRotX = this.rotX;
        this.prevRotY = this.rotY;
        this.prevRotZ = this.rotZ;
        this.prevRotW = this.rotW;
        super.tick();
        if (this.level().isClientSide()) {
            this.rotX = this.entityData.get(ROT_X);
            this.rotY = this.entityData.get(ROT_Y);
            this.rotZ = this.entityData.get(ROT_Z);
            this.rotW = this.entityData.get(ROT_W);
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!stack.is(Physicraft.PHYSICS_CONVERTER.get())) {
            return InteractionResult.PASS;
        }
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return this.restore(player) ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_BLOCK, Blocks.STONE.defaultBlockState());
        builder.define(ROT_X, 0.0F);
        builder.define(ROT_Y, 0.0F);
        builder.define(ROT_Z, 0.0F);
        builder.define(ROT_W, 1.0F);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.put("BlockState", NbtUtils.writeBlockState(this.getBlockState()));
        tag.putFloat("Hardness", this.hardness);
        tag.putFloat("vx", this.vx);
        tag.putFloat("vy", this.vy);
        tag.putFloat("vz", this.vz);
        tag.putFloat("avx", this.avx);
        tag.putFloat("avy", this.avy);
        tag.putFloat("avz", this.avz);
        tag.putFloat("qx", this.rotX);
        tag.putFloat("qy", this.rotY);
        tag.putFloat("qz", this.rotZ);
        tag.putFloat("qw", this.rotW);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("BlockState", CompoundTag.TAG_COMPOUND)) {
            BlockState state = NbtUtils.readBlockState(this.level().holderLookup(Registries.BLOCK), tag.getCompound("BlockState"));
            this.setBlockState(state);
        }
        this.hardness = tag.contains("Hardness") ? tag.getFloat("Hardness") : 1.0F;
        this.vx = tag.getFloat("vx");
        this.vy = tag.getFloat("vy");
        this.vz = tag.getFloat("vz");
        this.avx = tag.getFloat("avx");
        this.avy = tag.getFloat("avy");
        this.avz = tag.getFloat("avz");
        this.rotX = tag.getFloat("qx");
        this.rotY = tag.getFloat("qy");
        this.rotZ = tag.getFloat("qz");
        this.rotW = tag.contains("qw") ? tag.getFloat("qw") : 1.0F;
        if (this.rotX == 0.0F && this.rotY == 0.0F && this.rotZ == 0.0F && this.rotW == 0.0F) {
            this.rotW = 1.0F;
        }
        this.prevRotX = this.rotX;
        this.prevRotY = this.rotY;
        this.prevRotZ = this.rotZ;
        this.prevRotW = this.rotW;
        this.syncRotation(this.rotX, this.rotY, this.rotZ, this.rotW);
    }

    private boolean restore(Player player) {
        BlockPos pos = this.centerBlockPos();
        BlockState occupying = this.level().getBlockState(pos);
        BlockState state = this.getBlockState();
        if (state.isAir() || !occupying.canBeReplaced() || !this.level().setBlock(pos, state, Block.UPDATE_ALL)) {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("physicraft.converter.restore_blocked"), true);
            return false;
        }
        SoundType sound = state.getSoundType(this.level(), pos, player);
        this.level().playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS, 1.0F, 0.8F);
        this.level().gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        this.discard();
        return true;
    }

    private BlockPos centerBlockPos() {
        return BlockPos.containing(this.getX(), this.getY() + this.getBbHeight() * 0.5D, this.getZ());
    }

    private void setBlockState(BlockState state) {
        this.entityData.set(DATA_BLOCK, state);
    }

    private void syncRotation(float x, float y, float z, float w) {
        this.setIfChanged(ROT_X, x);
        this.setIfChanged(ROT_Y, y);
        this.setIfChanged(ROT_Z, z);
        this.setIfChanged(ROT_W, w);
    }

    private void setIfChanged(EntityDataAccessor<Float> accessor, float value) {
        Float current = this.entityData.get(accessor);
        if (current == null || Math.abs(current - value) > 0.0008F) {
            this.entityData.set(accessor, value);
        }
    }

    @Nullable
    @Override
    public ItemStack getPickResult() {
        return new ItemStack(this.getBlockState().getBlock());
    }
}
