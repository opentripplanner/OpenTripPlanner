package org.opentripplanner.routing.core.vehicle_sharing;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VehicleValidatorTest {

    private static final VehicleDescription VEHICLE = new CarDescription("id", 1.0, 2.0, null, null, null);

    @Test
    public void testEmptyValidatorPasses() {
        // given
        VehicleValidator validator = new VehicleValidator();

        // then
        assertTrue(validator.isValid(VEHICLE));
    }

    @Test
    public void testValidatorFailsWhenFilterFiltersOut() {
        // given
        VehicleValidator validator = new VehicleValidator();
        VehicleFilter filter = mock(VehicleFilter.class);
        when(filter.isValid(VEHICLE)).thenReturn(false);
        validator.addFilter(filter);

        // then
        assertFalse(validator.isValid(VEHICLE));
    }
}
