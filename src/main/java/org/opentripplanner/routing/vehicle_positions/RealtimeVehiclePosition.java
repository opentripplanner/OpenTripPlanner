package org.opentripplanner.routing.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition.CongestionLevel;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StopLocation;

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

    /** Unix timestamp of when the realtime position was recorded */
    public long seconds;

    public String patternId;

    public VehicleStopStatus stopStatus;
    public StopLocation nextStop;
    public int nextStopSequenceId;

    public CongestionLevel congestionLevel;
}
