package org.opentripplanner.model.plan.leg;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.street.model.note.StreetNote;
import org.opentripplanner.street.search.TraverseMode;
import org.opentripplanner.utils.lang.DoubleUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

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
  private final Emission emissionPerPerson;

  protected StreetLeg(StreetLegBuilder builder) {
    this.mode = Objects.requireNonNull(builder.getMode());
    this.startTime = builder.getStartTime();
    this.endTime = builder.getEndTime();
    this.distanceMeters = DoubleUtils.roundTo2Decimals(builder.getDistanceMeters());
    this.from = builder.getFrom();
    this.to = builder.getTo();
    this.generalizedCost = builder.getGeneralizedCost();
    this.elevationProfile = builder.getElevationProfile();
    this.legGeometry = builder.getGeometry();
    this.walkSteps = Objects.requireNonNull(builder.getWalkSteps());
    this.streetNotes = Set.copyOf(builder.getStreetNotes());
    this.walkingBike = builder.getWalkingBike();
    this.rentedVehicle = builder.getRentedVehicle();
    this.vehicleRentalNetwork = builder.getVehicleRentalNetwork();
    this.accessibilityScore = builder.getAccessibilityScore();
    this.emissionPerPerson = builder.emissionPerPerson();
  }

  public static StreetLegBuilder of() {
    return new StreetLegBuilder();
  }

  public StreetLegBuilder copyOf() {
    return new StreetLegBuilder(this);
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
  public ZonedDateTime startTime() {
    return startTime;
  }

  @Override
  public ZonedDateTime endTime() {
    return endTime;
  }

  @Override
  public double distanceMeters() {
    return distanceMeters;
  }

  @Override
  public Place from() {
    return from;
  }

  @Override
  public Place to() {
    return to;
  }

  @Override
  public LineString legGeometry() {
    return legGeometry;
  }

  /**
   * Get elevation profile, with values rounded to two decimals.
   */
  @Override
  public ElevationProfile elevationProfile() {
    return elevationProfile;
  }

  @Override
  public List<WalkStep> listWalkSteps() {
    return walkSteps;
  }

  @Override
  public Set<StreetNote> listStreetNotes() {
    return streetNotes;
  }

  @Override
  public Set<TransitAlert> listTransitAlerts() {
    return Set.of();
  }

  @Override
  public Boolean walkingBike() {
    return walkingBike;
  }

  @Override
  @Nullable
  public Float accessibilityScore() {
    return accessibilityScore;
  }

  @Override
  public Boolean rentedVehicle() {
    return rentedVehicle;
  }

  @Override
  public String vehicleRentalNetwork() {
    return vehicleRentalNetwork;
  }

  @Override
  public int generalizedCost() {
    return generalizedCost;
  }

  @Override
  public boolean hasSameMode(Leg other) {
    return other instanceof StreetLeg oSL && mode.equals(oSL.mode);
  }

  @Override
  public LegCallTime start() {
    return LegCallTime.ofStatic(startTime);
  }

  @Override
  public LegCallTime end() {
    return LegCallTime.ofStatic(endTime);
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    return copyOf()
      .withStartTime(startTime.plus(duration))
      .withEndTime(endTime.plus(duration))
      .build();
  }

  @Override
  public List<FareProductUse> fareProducts() {
    return List.of();
  }

  public StreetLeg withAccessibilityScore(float accessibilityScore) {
    return copyOf().withAccessibilityScore(accessibilityScore).build();
  }

  @Nullable
  @Override
  public Emission emissionPerPerson() {
    return emissionPerPerson;
  }

  @Nullable
  @Override
  public Leg withEmissionPerPerson(Emission emissionPerPerson) {
    return copyOf().withEmissionPerPerson(emissionPerPerson).build();
  }

  /**
   * Should be used for debug logging only.
   * <p>
   * The {@code legGeometry}, {@code elevationProfile}, and {@code walkSteps} are skipped to avoid
   * spamming logs. Explicit access should be used if needed.
   */
  @Override
  public String toString() {
    return ToStringBuilder.of(StreetLeg.class)
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
