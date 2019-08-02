package org.opentripplanner.model;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class StopTest {
    private Stop subject = new Stop();

    @Test public void isPlatform() {
        subject.setLocationType(Stop.PLATFORM_LOCATION_TYPE);
        assertTrue(subject.isPlatform());
        subject.setLocationType(Stop.PARENT_STATION_LOCATION_TYPE);
        assertFalse(subject.isPlatform());
    }

    @Test public void isStation() {
        subject.setLocationType(Stop.PARENT_STATION_LOCATION_TYPE);
        assertTrue(subject.isStation());
        subject.setLocationType(Stop.PLATFORM_LOCATION_TYPE);
        assertFalse(subject.isStation());
    }

    @Test public void convertStationToStop() {
        // Given a Station
        subject.setLocationType(Stop.PARENT_STATION_LOCATION_TYPE);

        // When changed it to a stop
        subject.convertStationToStop();

        // Then
        assertTrue(subject.isPlatform());
    }
}