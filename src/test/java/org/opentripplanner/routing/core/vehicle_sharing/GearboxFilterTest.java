package org.opentripplanner.routing.core.vehicle_sharing;

import org.junit.Test;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class GearboxFilterTest {
    
    private static final VehicleDescription CAR = new CarDescription("id", 1.0, 2.0, null, Gearbox.MANUAL, null);
    private static final VehicleDescription KICKSCOOTER = new KickScooterDescription("id", 1.0, 2.0, null, null, null);
    private static final VehicleDescription MOTORBIKE = new MotorbikeDescription("id", 1.0, 2.0, null, null, null);

    @Test
    public void testShouldPassCarWhenGearboxAllowed() {
        // given
        GearboxFilter filter = new GearboxFilter(of(Gearbox.MANUAL));

        // then
        assertTrue(filter.isValid(CAR));
    }

    @Test
    public void testShouldFilterOutCarWhenGearboxDisallowed() {
        // given
        GearboxFilter filter = new GearboxFilter(of(Gearbox.AUTOMATIC));

        // then
        assertFalse(filter.isValid(CAR));
    }

    @Test
    public void testShouldIgnoreNonCarTypes() {
        // given
        GearboxFilter filter = new GearboxFilter(of(Gearbox.AUTOMATIC));

        // then
        assertTrue(filter.isValid(MOTORBIKE));
        assertTrue(filter.isValid(KICKSCOOTER));
    }
}