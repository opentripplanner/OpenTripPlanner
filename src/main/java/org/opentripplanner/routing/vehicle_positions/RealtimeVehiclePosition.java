package org.opentripplanner.routing.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition.CongestionLevel;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;
import java.time.Instant;
import org.opentripplanner.model.StopLocation;

/**
 * A simplified vehicle position class that supports being output to HTTP.
 * Compatible with https://github.com/opentripplanner/otp-ui/blob/master/packages/core-utils/src/types.js#L86
 */
public class RealtimeVehiclePosition {
    public final String vehicleId;
    public final String label;
    public final double lat;
    public final double lon;
    public final double speed;
    public final double heading;

    /** Unix timestamp of when the realtime position was recorded */
    public final Instant time;

    public final VehicleStopStatus stopStatus;
    public final StopLocation nextStop;
    public final int nextStopSequenceId;

    public final CongestionLevel congestionLevel;

    public RealtimeVehiclePosition(
            String vehicleId,
            String label,
            double lat,
            double lon,
            double speed,
            double heading,
            Instant time,
            VehicleStopStatus stopStatus,
            StopLocation nextStop,
            int nextStopSequenceId,
            CongestionLevel congestionLevel
    ) {
        this.vehicleId = vehicleId;
        this.label = label;
        this.lat = lat;
        this.lon = lon;
        this.speed = speed;
        this.heading = heading;
        this.time = time;
        this.stopStatus = stopStatus;
        this.nextStop = nextStop;
        this.nextStopSequenceId = nextStopSequenceId;
        this.congestionLevel = congestionLevel;
    }

    public static RealtimeVehiclePositionBuilder builder() {
        return new RealtimeVehiclePositionBuilder();
    }
}
