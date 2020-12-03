package org.opentripplanner.model.modes;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TransitModeTest {

    @Test
    void fromMainModeEnum() {
        // Verify all modes are mapped
        for (TransitMainMode it : TransitMainMode.values()) {
            assertNotNull(TransitMode.fromMainModeEnum(it));
        }
    }
}