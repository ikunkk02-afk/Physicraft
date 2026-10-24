package com.shoyun.physicraft;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config for the embedded rigid-body simulation.
 * Values are read live so a config reload applies on the next tick.
 */
public class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    static {
        BUILDER.comment(
                "Physicraft 刚体参数。未被转换的原版方块不会进入物理世界。",
                "Rigid-body settings. Vanilla blocks stay static until converted."
        ).push("physics");
    }

    public static final ModConfigSpec.DoubleValue GRAVITY = BUILDER
            .comment(
                    "向下加速度，单位：方块/秒²。原版掉落大约是 16。",
                    "Downward acceleration in blocks per second squared. Vanilla falling is about 16."
            )
            .defineInRange("gravity", 20.0D, 0.0D, 80.0D);

    public static final ModConfigSpec.DoubleValue WIND_X = BUILDER
            .comment(
                    "全局风的 X 加速度（方块/秒²）。正方向为东。设为 0 可关闭。",
                    "Global wind acceleration on X, in blocks/second^2. Positive is east. Use 0 to disable."
            )
            .defineInRange("windX", 6.0D, -80.0D, 80.0D);

    public static final ModConfigSpec.DoubleValue WIND_Y = BUILDER
            .comment(
                    "全局风的 Y 加速度（方块/秒²）。",
                    "Global wind acceleration on Y, in blocks/second^2."
            )
            .defineInRange("windY", 0.0D, -80.0D, 80.0D);

    public static final ModConfigSpec.DoubleValue WIND_Z = BUILDER
            .comment(
                    "全局风的 Z 加速度（方块/秒²）。正方向为南。",
                    "Global wind acceleration on Z, in blocks/second^2. Positive is south."
            )
            .defineInRange("windZ", 0.0D, -80.0D, 80.0D);

    public static final ModConfigSpec.DoubleValue FRICTION = BUILDER
            .comment(
                    "物理方块表面摩擦。Bullet 会把双方的摩擦相乘。",
                    "Surface friction of a physics block. Bullet multiplies the two bodies' friction values."
            )
            .defineInRange("friction", 0.55D, 0.0D, 2.0D);

    public static final ModConfigSpec.DoubleValue TERRAIN_FRICTION = BUILDER
            .comment(
                    "地形碰撞体的摩擦。与物理方块摩擦相乘后生效。",
                    "Friction of the generated terrain colliders. Combined with block friction by multiplication."
            )
            .defineInRange("terrainFriction", 0.9D, 0.0D, 2.0D);

    public static final ModConfigSpec.DoubleValue RESTITUTION = BUILDER
            .comment(
                    "物理方块弹性。0 几乎不反弹。",
                    "Bounciness of a physics block. 0 is nearly inelastic."
            )
            .defineInRange("restitution", 0.02D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue LINEAR_DAMPING = BUILDER
            .comment(
                    "线速度阻尼，用来给刮风中的方块一个大致的终端速度。",
                    "Linear damping. Gives wind-blown blocks a rough terminal speed."
            )
            .defineInRange("linearDamping", 0.12D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue ANGULAR_DAMPING = BUILDER
            .comment(
                    "角速度阻尼。",
                    "Angular damping."
            )
            .defineInRange("angularDamping", 0.35D, 0.0D, 1.0D);

    public static final ModConfigSpec.DoubleValue BREAK_IMPULSE = BUILDER
            .comment(
                    "撞击破坏的基础冲量阈值。实际阈值 = breakImpulse * (0.35 + 硬度)。越大越难打碎。",
                    "Base impulse required to break a physics block. Effective threshold is breakImpulse * (0.35 + hardness)."
            )
            .defineInRange("breakImpulse", 4.0D, 0.0D, 10000.0D);

    public static final ModConfigSpec.DoubleValue MIN_IMPACT_SPEED = BUILDER
            .comment(
                    "触发破坏所需的最低相对速度（方块/秒），避免静止堆叠被接触冲量打碎。",
                    "Minimum relative speed, in blocks/second, required before an impact can break a block."
            )
            .defineInRange("minImpactSpeed", 3.5D, 0.0D, 100.0D);

    public static final ModConfigSpec.BooleanValue BREAK_DROPS = BUILDER
            .comment(
                    "撞击破坏时是否按原版规则掉落物品。",
                    "Whether a block broken by impact drops items using vanilla loot rules."
            )
            .define("breakDropsItem", true);

    public static final ModConfigSpec.IntValue TERRAIN_RADIUS = BUILDER
            .comment(
                    "每个物理方块周围生成地形碰撞盒的半径（方块）。",
                    "Radius, in blocks, of terrain colliders built around each physics body."
            )
            .defineInRange("terrainRadius", 3, 1, 8);

    public static final ModConfigSpec.IntValue MAX_BODIES = BUILDER
            .comment(
                    "单个维度允许存在的物理方块上限。",
                    "Maximum number of physics blocks in a single dimension."
            )
            .defineInRange("maxBodies", 256, 1, 5000);

    static {
        BUILDER.pop();
    }

    static final ModConfigSpec SPEC = BUILDER.build();

    private Config() {
    }
}
