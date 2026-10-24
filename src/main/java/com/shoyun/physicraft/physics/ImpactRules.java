package com.shoyun.physicraft.physics;

/**
 * Decides which physics block breaks when two dynamic bodies hit.
 * Bullet multiplies contact impulses into {@code appliedImpulse}; resting contacts are rejected by relative speed.
 */
public final class ImpactRules {
    private ImpactRules() {
    }

    /**
     * Effective impulse a body can survive. Harder blocks scale the configured base threshold up.
     */
    public static float requiredImpulse(float baseImpulse, float hardness) {
        return baseImpulse * (0.35F + Math.max(0.0F, hardness));
    }

    /**
     * @return {@code 0} to break A, {@code 1} to break B, or {@code -1} to break neither
     */
    public static int selectVictim(float hardnessA, float hardnessB, float speedA, float speedB,
            float impulse, float relativeSpeed, float baseImpulse, float minImpactSpeed) {
        if (relativeSpeed < minImpactSpeed || baseImpulse <= 0.0F || impulse <= 0.0F) {
            return -1;
        }
        boolean aBreaks = impulse >= requiredImpulse(baseImpulse, hardnessA);
        boolean bBreaks = impulse >= requiredImpulse(baseImpulse, hardnessB);
        if (aBreaks && !bBreaks) {
            return 0;
        }
        if (bBreaks && !aBreaks) {
            return 1;
        }
        if (!aBreaks) {
            return -1;
        }
        if (hardnessA < hardnessB) {
            return 0;
        }
        if (hardnessB < hardnessA) {
            return 1;
        }
        return speedA >= speedB ? 0 : 1;
    }
}
