package org.opentripplanner.updater.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition;

import java.util.List;

public interface VehiclePositionSource {
    /**
     * Parses GTFS-RT for vehicle positions
     */
    public List<VehiclePosition> getPositions();

    public String getFeedId();
}
