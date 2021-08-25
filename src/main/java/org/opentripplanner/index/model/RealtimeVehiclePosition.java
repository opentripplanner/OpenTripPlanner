package org.opentripplanner.index.model;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition.CongestionLevel;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;

/**
 * A simplified vehicle position class that supports being output to HTTP
 */
public class RealtimeVehiclePosition {
    public String id;
    public String label;
    public double lat;
    public double lon;
    public double speed;
    public double heading;

    public VehicleStopStatus stopStatus;
    public CongestionLevel congestionLevel;
    // TODO: stop id?
    // TODO: timestamp?
}
