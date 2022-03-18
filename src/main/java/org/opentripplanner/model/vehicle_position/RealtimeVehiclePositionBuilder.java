package org.opentripplanner.model.vehicle_position;

import java.time.Instant;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.WgsCoordinate;
import org.opentripplanner.model.vehicle_position.RealtimeVehiclePosition.StopStatus;

public class RealtimeVehiclePositionBuilder {

    private String vehicleId;
    private String label;
    private WgsCoordinate coordinates;
    private Double speed = null;
    private Double heading = null;
    private Instant time;
    private StopStatus stopStatus;
    private StopLocation nextStop;
    private Trip trip;

    public RealtimeVehiclePositionBuilder setVehicleId(String vehicleId) {
        this.vehicleId = vehicleId;
        return this;
    }

    public RealtimeVehiclePositionBuilder setLabel(String label) {
        this.label = label;
        return this;
    }

    public RealtimeVehiclePositionBuilder setCoordinates(WgsCoordinate c) {
        this.coordinates = c;
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

    public RealtimeVehiclePositionBuilder setStopStatus(StopStatus stopStatus) {
        this.stopStatus = stopStatus;
        return this;
    }

    public RealtimeVehiclePositionBuilder setNextStop(StopLocation nextStop) {
        this.nextStop = nextStop;
        return this;
    }

    public RealtimeVehiclePositionBuilder setTrip(Trip trip) {
        this.trip = trip;
        return this;
    }

    public RealtimeVehiclePosition build() {
        return new RealtimeVehiclePosition(
                vehicleId, label, coordinates, speed, heading, time, stopStatus, nextStop, trip);
    }
}