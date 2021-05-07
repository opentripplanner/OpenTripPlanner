package org.opentripplanner.graph_builder.module.osm;

import org.junit.jupiter.api.Test;
import org.opentripplanner.openstreetmap.model.OSMWithTags;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class WayPropertySetSourceTest {

    @Test
    public void isMotorThroughTrafficExplicitlyDisallowed() {
        OSMWithTags o = new OSMWithTags();
        WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();

        assertFalse(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

        o.addTag("access", "something");
        assertFalse(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

        o.addTag("access", "destination");
        assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

        o.addTag("access", "private");
        assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(o));

        assertTrue(wayPropertySetSource.isMotorVehicleThroughTrafficExplicitlyDisallowed(way("motor_vehicle", "destination")));
    }

    @Test
    public void isBicycleNoThroughTrafficExplicitlyDisallowed() {
        OSMWithTags o = new OSMWithTags();
        WayPropertySetSource wayPropertySetSource = new DefaultWayPropertySetSource();
        assertTrue(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(way("bicycle", "destination")));
        assertTrue(wayPropertySetSource.isBicycleNoThroughTrafficExplicitlyDisallowed(way("access", "destination")));
    }

    public  OSMWithTags way(String key, String value) {
        var way = new OSMWithTags();
        way.addTag(key, value);
        return way;
    }
}
