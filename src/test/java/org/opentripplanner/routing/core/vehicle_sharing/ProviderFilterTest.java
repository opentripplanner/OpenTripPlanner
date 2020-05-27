package org.opentripplanner.routing.core.vehicle_sharing;

import org.junit.Test;

import static com.google.common.collect.ImmutableSet.of;
import static org.junit.Assert.*;

public class ProviderFilterTest {

    private static final VehicleDescription VEHICLE = new CarDescription("id", 1.0, 2.0, null, null, new Provider(1, "Panek"));

    @Test
    public void testProvidersAllowedShouldPass() {
        // given
        ProviderFilter filter = ProviderFilter.providersAllowedFilter(of("Innogy", "Panek"));

        // then
        assertTrue(filter.isValid(VEHICLE));
    }

    @Test
    public void testProvidersDisallowedShouldFail() {
        // given
        ProviderFilter filter = ProviderFilter.providersDisallowedFilter(of("Innogy", "Panek"));

        // then
        assertFalse(filter.isValid(VEHICLE));
    }

    @Test
    public void testProvidersAllowedShouldFail() {
        // given
        ProviderFilter filter = ProviderFilter.providersAllowedFilter(of("Innogy"));

        // then
        assertFalse(filter.isValid(VEHICLE));
    }

    @Test
    public void testProvidersDisallowedShouldPass() {
        // given
        ProviderFilter filter = ProviderFilter.providersDisallowedFilter(of("Innogy"));

        // then
        assertTrue(filter.isValid(VEHICLE));
    }
}