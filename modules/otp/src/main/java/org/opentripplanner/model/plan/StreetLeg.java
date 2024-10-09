package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.lang.DoubleUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.search.TraverseMode;

/**
 * One leg of a trip -- that is, a temporally continuous piece of the journey that takes place using
 * mainly a single model on the street network.
 */
public class StreetLeg implements Leg {

  private final TraverseMode mode;
  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;
  private final double distanceMeters;
  private final Place from;
  private final Place to;
  private final int generalizedCost;
  private final LineString legGeometry;
  private final List<WalkStep> walkSteps;
  private final Set<StreetNote> streetNotes;
  private final ElevationProfile elevationProfile;

  private final Boolean walkingBike;
  private final Boolean rentedVehicle;
  private final String vehicleRentalNetwork;
  private final Float accessibilityScore;

  public StreetLeg(StreetLegBuilder builder) {
    this.mode = Objects.requireNonNull(builder.getMode());
    this.startTime = builder.getStartTime();
    this.endTime = builder.getEndTime();
    this.distanceMeters = DoubleUtils.roundTo2Decimals(builder.getDistanceMeters());
    this.from = builder.getFrom();
    this.to = builder.getTo();
    this.generalizedCost = builder.getGeneralizedCost();
    this.elevationProfile = builder.getElevationProfile();
    this.legGeometry = builder.getGeometry();
    this.walkSteps = builder.getWalkSteps();
    this.streetNotes = Set.copyOf(builder.getStreetNotes());
    this.walkingBike = builder.getWalkingBike();
    this.rentedVehicle = builder.getRentedVehicle();
    this.vehicleRentalNetwork = builder.getVehicleRentalNetwork();
    this.accessibilityScore = builder.getAccessibilityScore();
  }

  public static StreetLegBuilder create() {
    return new StreetLegBuilder();
  }

  @Override
  public boolean isTransitLeg() {
    return false;
  }

  @Override
  public boolean isWalkingLeg() {
    return mode == TraverseMode.WALK;
  }

  @Override
  public boolean isStreetLeg() {
    return true;
  }

  public TraverseMode getMode() {
    return mode;
  }

  @Override
  public ZonedDateTime getStartTime() {
    return startTime;
  }

  @Override
  public ZonedDateTime getEndTime() {
    return endTime;
  }

  @Override
  public double getDistanceMeters() {
    return distanceMeters;
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

  /**
   * Get elevation profile, with values rounded to two decimals.
   */
  @Override
  public ElevationProfile getElevationProfile() {
    return elevationProfile;
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

  @Override
  @Nullable
  public Float accessibilityScore() {
    return accessibilityScore;
  }

  @Override
  public Boolean getRentedVehicle() {
    return rentedVehicle;
  }

  @Override
  public String getVehicleRentalNetwork() {
    return vehicleRentalNetwork;
  }

  @Override
  public int getGeneralizedCost() {
    return generalizedCost;
  }

  @Override
  public boolean hasSameMode(Leg other) {
    return other instanceof StreetLeg oSL && mode.equals(oSL.mode);
  }

  @Override
  public LegTime start() {
    return LegTime.ofStatic(startTime);
  }

  @Override
  public LegTime end() {
    return LegTime.ofStatic(endTime);
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    return StreetLegBuilder
      .of(this)
      .withStartTime(startTime.plus(duration))
      .withEndTime(endTime.plus(duration))
      .build();
  }

  @Override
  public void setFareProducts(List<FareProductUse> products) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<FareProductUse> fareProducts() {
    return List.of();
  }

  public StreetLeg withAccessibilityScore(float accessibilityScore) {
    return StreetLegBuilder.of(this).withAccessibilityScore(accessibilityScore).build();
  }

  /**
   * Should be used for debug logging only.
   * <p>
   * The {@code legGeometry}, {@code elevationProfile}, and {@code walkSteps} are skipped to avoid
   * spamming logs. Explicit access should be used if needed.
   */
  @Override
  public String toString() {
    return ToStringBuilder
      .of(StreetLeg.class)
      .addObj("from", from)
      .addObj("to", to)
      .addTime("startTime", startTime)
      .addTime("endTime", endTime)
      .addEnum("mode", mode)
      .addNum("distance", distanceMeters, "m")
      .addNum("cost", generalizedCost)
      .addCol("streetNotes", streetNotes)
      .addBool("walkingBike", walkingBike)
      .addBool("rentedVehicle", rentedVehicle)
      .addStr("bikeRentalNetwork", vehicleRentalNetwork)
      .toString();
  }
}
