package com.shoyun.physicraft.physics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ImpactRulesTest {
    @Test
    void restingContactDoesNotBreak() {
        assertEquals(-1, ImpactRules.selectVictim(0.5F, 0.5F, 0.1F, 0.0F, 20.0F, 0.2F, 4.0F, 3.5F));
    }

    @Test
    void softBodyBreaksBeforeHardBody() {
        float impulse = 5.0F;
        assertTrue(impulse >= ImpactRules.requiredImpulse(4.0F, 0.5F));
        assertTrue(impulse < ImpactRules.requiredImpulse(4.0F, 1.5F));
        assertEquals(0, ImpactRules.selectVictim(0.5F, 1.5F, 4.0F, 1.0F, impulse, 6.0F, 4.0F, 3.5F));
        assertEquals(1, ImpactRules.selectVictim(1.5F, 0.5F, 1.0F, 4.0F, impulse, 6.0F, 4.0F, 3.5F));
    }

    @Test
    void equalHardnessBreaksTheFasterBody() {
        float impulse = 30.0F;
        assertEquals(1, ImpactRules.selectVictim(1.5F, 1.5F, 2.0F, 12.0F, impulse, 10.0F, 4.0F, 3.5F));
        assertEquals(0, ImpactRules.selectVictim(1.5F, 1.5F, 12.0F, 2.0F, impulse, 10.0F, 4.0F, 3.5F));
    }
}
