package org.opentripplanner.model.plan;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class StreetLegBuilder {

  private TraverseMode mode;
  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private Place from;
  private Place to;
  private double distanceMeters;
  private int generalizedCost;
  private LineString geometry;
  private List<P2<Double>> elevation;
  private List<WalkStep> walkSteps;
  private FeedScopedId pathwayId;
  private Boolean walkingBike;
  private Boolean rentedVehicle;
  private String vehicleRentalNetwork;
  private Float accessibilityScore;

  private Set<StreetNote> streetNotes = new HashSet<>();

  public static StreetLegBuilder of(StreetLeg leg) {
    return new StreetLegBuilder()
      .setMode(leg.getMode())
      .setStartTime(leg.getStartTime())
      .setEndTime(leg.getEndTime())
      .setFrom(leg.getFrom())
      .setTo(leg.getTo())
      .setDistanceMeters(leg.getDistanceMeters())
      .setGeneralizedCost(leg.getGeneralizedCost())
      .setGeometry(leg.getLegGeometry())
      .setElevation(leg.getLegElevation())
      .setWalkSteps(leg.getWalkSteps())
      .setPathwayId(leg.getPathwayId())
      .setWalkingBike(leg.getWalkingBike())
      .setRentedVehicle(leg.getRentedVehicle())
      .setVehicleRentalNetwork(leg.getVehicleRentalNetwork())
      .setAccessibilityScore(leg.accessibilityScore())
      .setStreetNotes(leg.getStreetNotes());
  }

  public StreetLeg build() {
    return new StreetLeg(
      mode,
      startTime,
      endTime,
      from,
      to,
      distanceMeters,
      generalizedCost,
      geometry,
      elevation,
      walkSteps,
      streetNotes,
      pathwayId,
      walkingBike,
      rentedVehicle,
      vehicleRentalNetwork,
      accessibilityScore
    );
  }

  public StreetLegBuilder setMode(TraverseMode mode) {
    this.mode = mode;
    return this;
  }

  public StreetLegBuilder setStartTime(ZonedDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  public StreetLegBuilder setEndTime(ZonedDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  public StreetLegBuilder setFrom(Place from) {
    this.from = from;
    return this;
  }

  public StreetLegBuilder setTo(Place to) {
    this.to = to;
    return this;
  }

  public StreetLegBuilder setDistanceMeters(double distanceMeters) {
    this.distanceMeters = distanceMeters;
    return this;
  }

  public StreetLegBuilder setGeneralizedCost(int generalizedCost) {
    this.generalizedCost = generalizedCost;
    return this;
  }

  public StreetLegBuilder setGeometry(LineString geometry) {
    this.geometry = geometry;
    return this;
  }

  public StreetLegBuilder setElevation(List<P2<Double>> elevation) {
    this.elevation = elevation;
    return this;
  }

  public StreetLegBuilder setWalkSteps(List<WalkStep> walkSteps) {
    this.walkSteps = walkSteps;
    return this;
  }

  public StreetLegBuilder setPathwayId(FeedScopedId pathwayId) {
    this.pathwayId = pathwayId;
    return this;
  }

  public StreetLegBuilder setWalkingBike(Boolean walkingBike) {
    this.walkingBike = walkingBike;
    return this;
  }

  public StreetLegBuilder setRentedVehicle(Boolean rentedVehicle) {
    this.rentedVehicle = rentedVehicle;
    return this;
  }

  public StreetLegBuilder setVehicleRentalNetwork(String vehicleRentalNetwork) {
    this.vehicleRentalNetwork = vehicleRentalNetwork;
    return this;
  }

  public StreetLegBuilder setAccessibilityScore(Float accessibilityScore) {
    this.accessibilityScore = accessibilityScore;
    return this;
  }

  public StreetLegBuilder addStreetNote(StreetNote note) {
    this.streetNotes.add(note);
    return this;
  }

  public StreetLegBuilder setStreetNotes(Set<StreetNote> notes) {
    streetNotes = new HashSet<>();
    streetNotes.addAll(notes);
    return this;
  }
}
