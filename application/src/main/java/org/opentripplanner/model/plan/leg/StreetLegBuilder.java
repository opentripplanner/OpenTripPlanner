package org.opentripplanner.model.plan.leg;

import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.search.TraverseMode;

public class StreetLegBuilder {

  private TraverseMode mode;
  private ZonedDateTime startTime;
  private ZonedDateTime endTime;
  private Place from;
  private Place to;
  private double distanceMeters;
  private int generalizedCost;
  private LineString geometry;
  private ElevationProfile elevationProfile;
  private List<WalkStep> walkSteps = List.of();
  private Boolean walkingBike;
  private Boolean rentedVehicle;
  private String vehicleRentalNetwork;
  private Set<StreetNote> streetNotes = new HashSet<>();
  private Float accessibilityScore;

  protected StreetLegBuilder() {}

  protected StreetLegBuilder(StreetLeg leg) {
    this.mode = leg.getMode();
    this.startTime = leg.startTime();
    this.endTime = leg.endTime();
    this.from = leg.from();
    this.to = leg.to();
    this.distanceMeters = leg.distanceMeters();
    this.generalizedCost = leg.generalizedCost();
    this.geometry = leg.legGeometry();
    this.elevationProfile = leg.elevationProfile();
    this.walkSteps = Objects.requireNonNull(leg.listWalkSteps());
    this.walkingBike = leg.walkingBike();
    this.rentedVehicle = leg.rentedVehicle();
    this.vehicleRentalNetwork = leg.vehicleRentalNetwork();
    this.streetNotes = leg.listStreetNotes();
    this.accessibilityScore = leg.accessibilityScore();
  }

  public StreetLeg build() {
    return new StreetLeg(this);
  }

  public TraverseMode getMode() {
    return mode;
  }

  public ZonedDateTime getStartTime() {
    return startTime;
  }

  public ZonedDateTime getEndTime() {
    return endTime;
  }

  public Place getFrom() {
    return from;
  }

  public Place getTo() {
    return to;
  }

  public double getDistanceMeters() {
    return distanceMeters;
  }

  public int getGeneralizedCost() {
    return generalizedCost;
  }

  public LineString getGeometry() {
    return geometry;
  }

  public ElevationProfile getElevationProfile() {
    return elevationProfile;
  }

  public List<WalkStep> getWalkSteps() {
    return walkSteps;
  }

  public Boolean getWalkingBike() {
    return walkingBike;
  }

  public Boolean getRentedVehicle() {
    return rentedVehicle;
  }

  public String getVehicleRentalNetwork() {
    return vehicleRentalNetwork;
  }

  public Float getAccessibilityScore() {
    return accessibilityScore;
  }

  public Set<StreetNote> getStreetNotes() {
    return streetNotes;
  }

  public StreetLegBuilder withMode(TraverseMode mode) {
    this.mode = mode;
    return this;
  }

  public StreetLegBuilder withStartTime(ZonedDateTime startTime) {
    this.startTime = startTime;
    return this;
  }

  public StreetLegBuilder withEndTime(ZonedDateTime endTime) {
    this.endTime = endTime;
    return this;
  }

  public StreetLegBuilder withFrom(Place from) {
    this.from = from;
    return this;
  }

  public StreetLegBuilder withTo(Place to) {
    this.to = to;
    return this;
  }

  public StreetLegBuilder withDistanceMeters(double distanceMeters) {
    this.distanceMeters = distanceMeters;
    return this;
  }

  public StreetLegBuilder withGeneralizedCost(int generalizedCost) {
    this.generalizedCost = generalizedCost;
    return this;
  }

  public StreetLegBuilder withGeometry(LineString geometry) {
    this.geometry = geometry;
    return this;
  }

  public StreetLegBuilder withElevationProfile(ElevationProfile elevationProfile) {
    this.elevationProfile = elevationProfile;
    return this;
  }

  public StreetLegBuilder withWalkSteps(List<WalkStep> walkSteps) {
    this.walkSteps = Objects.requireNonNull(walkSteps);
    return this;
  }

  public StreetLegBuilder withWalkingBike(Boolean walkingBike) {
    this.walkingBike = walkingBike;
    return this;
  }

  public StreetLegBuilder withRentedVehicle(Boolean rentedVehicle) {
    this.rentedVehicle = rentedVehicle;
    return this;
  }

  public StreetLegBuilder withVehicleRentalNetwork(String vehicleRentalNetwork) {
    this.vehicleRentalNetwork = vehicleRentalNetwork;
    return this;
  }

  public StreetLegBuilder withStreetNotes(Set<StreetNote> notes) {
    streetNotes = notes;
    return this;
  }

  public StreetLegBuilder withAccessibilityScore(Float accessibilityScore) {
    this.accessibilityScore = accessibilityScore;
    return this;
  }
}
