package org.opentripplanner.index.model;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition.CongestionLevel;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;

/**
 * A simplified vehicle position class that supports being output to HTTP.
 * Compatible with https://github.com/opentripplanner/otp-ui/blob/master/packages/core-utils/src/types.js#L86
 */
public class RealtimeVehiclePosition {
    public String vehicleId;
    public String label;
    public double lat;
    public double lon;
    public double speed;
    public double heading;

    public String patternId;

    public VehicleStopStatus stopStatus;
    public CongestionLevel congestionLevel;
    // TODO: stop id?
    // TODO: timestamp?
}
