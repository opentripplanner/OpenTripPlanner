package org.opentripplanner.routing.vehicle_positions;

import com.google.transit.realtime.GtfsRealtime.VehiclePosition.CongestionLevel;
import com.google.transit.realtime.GtfsRealtime.VehiclePosition.VehicleStopStatus;
import java.time.Instant;
import org.opentripplanner.model.StopLocation;

public class RealtimeVehiclePositionBuilder {

    private String vehicleId;
    private String label;
    private double lat;
    private double lon;
    private double speed;
    private double heading;
    private Instant time;
    private VehicleStopStatus stopStatus;
    private StopLocation nextStop;
    private int nextStopSequenceId;
    private CongestionLevel congestionLevel;

    public RealtimeVehiclePositionBuilder setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
        return this;
    }

    public RealtimeVehiclePositionBuilder setLabel(String label) {
        this.label = label;
        return this;
    }

    public RealtimeVehiclePositionBuilder setLat(double lat) {
        this.lat = lat;
        return this;
    }

    public RealtimeVehiclePositionBuilder setLon(double lon) {
        this.lon = lon;
        return this;
    }

    public RealtimeVehiclePositionBuilder setSpeed(double speed) {
        this.speed = speed;
        return this;
    }

    public RealtimeVehiclePositionBuilder setHeading(double heading) {
        this.heading = heading;
        return this;
    }

    public RealtimeVehiclePositionBuilder setTime(Instant time) {
        this.time = time;
        return this;
    }

    public RealtimeVehiclePositionBuilder setStopStatus(VehicleStopStatus stopStatus) {
        this.stopStatus = stopStatus;
        return this;
    }

    public RealtimeVehiclePositionBuilder setNextStop(StopLocation nextStop) {
        this.nextStop = nextStop;
        return this;
    }

    public RealtimeVehiclePositionBuilder setNextStopSequenceId(int nextStopSequenceId) {
        this.nextStopSequenceId = nextStopSequenceId;
        return this;
    }

    public RealtimeVehiclePositionBuilder setCongestionLevel(CongestionLevel congestionLevel) {
        this.congestionLevel = congestionLevel;
        return this;
    }

    public RealtimeVehiclePosition build() {
        return new RealtimeVehiclePosition(
                vehicleId, label, lat, lon, speed, heading, time, stopStatus, nextStop,
                nextStopSequenceId, congestionLevel
        );
    }
}