package org.opentripplanner.model.plan;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.FeedScopedId;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.routing.core.TraverseMode;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place using
 * mainly a single model on the street network.
 */
public class StreetLeg implements Leg {

    private final TraverseMode mode;

    private final Calendar startTime;

    private final Calendar endTime;

    private final Double distanceMeters;

    private final Place from;

    private final Place to;

    private final LineString legGeometry;

    private final List<WalkStep> walkSteps;

    private final Set<StreetNote> streetNotes = new HashSet<>();

    private final int generalizedCost;

    private FeedScopedId pathwayId;

    private Boolean walkingBike;

    private Boolean rentedVehicle;

    private String vehicleRentalNetwork;

    public StreetLeg(
            TraverseMode mode,
            Calendar startTime,
            Calendar endTime,
            Place from,
            Place to,
            Double distanceMeters,
            int generalizedCost,
            LineString geometry,
            List<WalkStep> walkSteps
    ) {
        if (mode.isTransit()) {
            throw new IllegalArgumentException(
                    "To create a transit leg use the other classes implementing Leg.");
        }
        this.mode = mode;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distanceMeters = distanceMeters;
        this.from = from;
        this.to = to;
        this.generalizedCost = generalizedCost;
        this.legGeometry = geometry;
        this.walkSteps = walkSteps;

    }

    @Override
    public boolean isTransitLeg() {
        return false;
    }

    @Override
    public boolean isWalkingLeg() {
        return mode.isWalking();
    }

    @Override
    public boolean isOnStreetNonTransit() {
        return true;
    }

    public void addStretNote(StreetNote streetNote) {
        streetNotes.add(streetNote);
    }

    public void setVehicleRentalNetwork(String network) {
        vehicleRentalNetwork = network;
    }

    @Override
    public TraverseMode getMode() {
        return mode;
    }

    @Override
    public Calendar getStartTime() {
        return startTime;
    }

    @Override
    public Calendar getEndTime() {
        return endTime;
    }

    @Override
    public Double getDistanceMeters() {
        return distanceMeters;
    }

    @Override
    public FeedScopedId getPathwayId() {
        return pathwayId;
    }

    public void setPathwayId(FeedScopedId pathwayId) {
        this.pathwayId = pathwayId;
    }

    @Override
    public Place getFrom() {
        return from;
    }

    @Override
    public Place getTo() {
        return to;
    }

    @Override
    public LineString getLegGeometry() {
        return legGeometry;
    }

    @Override
    public List<WalkStep> getWalkSteps() {
        return walkSteps;
    }

    @Override
    public Set<StreetNote> getStreetNotes() {
        return streetNotes;
    }

    @Override
    public Boolean getWalkingBike() {
        return walkingBike;
    }

    public void setWalkingBike(Boolean walkingBike) {
        this.walkingBike = walkingBike;
    }

    @Override
    public Boolean getRentedVehicle() {
        return rentedVehicle;
    }

    public void setRentedVehicle(Boolean rentedVehicle) {
        this.rentedVehicle = rentedVehicle;
    }

    @Override
    public String getVehicleRentalNetwork() {
        return vehicleRentalNetwork;
    }

    @Override
    public int getGeneralizedCost() {
        return generalizedCost;
    }

    /**
     * Should be used for debug logging only
     */
    @Override
    public String toString() {
        return ToStringBuilder.of(StreetLeg.class)
                .addObj("from", from)
                .addObj("to", to)
                .addTimeCal("startTime", startTime)
                .addTimeCal("endTime", endTime)
                .addEnum("mode", mode)
                .addNum("distance", distanceMeters, "m")
                .addNum("cost", generalizedCost)
                .addObj("gtfsPathwayId", pathwayId)
                .addObj("legGeometry", legGeometry)
                .addCol("walkSteps", walkSteps)
                .addCol("streetNotes", streetNotes)
                .addBool("walkingBike", walkingBike)
                .addBool("rentedVehicle", rentedVehicle)
                .addStr("bikeRentalNetwork", vehicleRentalNetwork)
                .toString();
    }
}