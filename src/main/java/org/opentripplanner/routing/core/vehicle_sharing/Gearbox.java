package org.opentripplanner.routing.core.vehicle_sharing;

import java.util.Locale;

public enum Gearbox {

    MANUAL,
    AUTOMATIC;

    public static Gearbox fromString(String gearbox) {
        if (gearbox == null) {
            return null;
        }
        try {
            return Gearbox.valueOf(gearbox.toUpperCase(Locale.US));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
