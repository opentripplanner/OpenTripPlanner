package org.opentripplanner.model.plan;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.model.StreetNote;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.util.lang.DoubleUtils;
import org.opentripplanner.util.lang.ToStringBuilder;

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
  private final Double elevationLost;
  private final Double elevationGained;

  private final LineString legGeometry;
  private final List<WalkStep> walkSteps;
  private final Set<StreetNote> streetNotes = new HashSet<>();
  private final List<P2<Double>> legElevation;

  private FeedScopedId pathwayId;
  private Boolean walkingBike;
  private Boolean rentedVehicle;
  private String vehicleRentalNetwork;

  public StreetLeg(
    TraverseMode mode,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    Place from,
    Place to,
    double distanceMeters,
    int generalizedCost,
    LineString geometry,
    List<P2<Double>> elevation,
    List<WalkStep> walkSteps
  ) {
    if (mode.isTransit()) {
      throw new IllegalArgumentException(
        "To create a transit leg use the other classes implementing Leg."
      );
    }
    this.mode = mode;
    this.startTime = startTime;
    this.endTime = endTime;
    this.distanceMeters = DoubleUtils.roundTo2Decimals(distanceMeters);
    this.from = from;
    this.to = to;
    this.generalizedCost = generalizedCost;
    this.legElevation = normalizeElevation(elevation);
    this.legGeometry = geometry;
    this.walkSteps = walkSteps;

    this.elevationGained = calculateElevationGained(legElevation);
    this.elevationLost = calculateElevationLost(legElevation);
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

  @Override
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
  public List<P2<Double>> getLegElevation() {
    return legElevation;
  }

  @Override
  public Double getElevationGained() {
    return elevationGained;
  }

  @Override
  public Double getElevationLost() {
    return elevationLost;
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

  public void setVehicleRentalNetwork(String network) {
    vehicleRentalNetwork = network;
  }

  @Override
  public int getGeneralizedCost() {
    return generalizedCost;
  }

  public void addStretNote(StreetNote streetNote) {
    streetNotes.add(streetNote);
  }

  @Override
  public Leg withTimeShift(Duration duration) {
    StreetLeg copy = new StreetLeg(
      mode,
      startTime.plus(duration),
      endTime.plus(duration),
      from,
      to,
      distanceMeters,
      generalizedCost,
      legGeometry,
      legElevation,
      walkSteps
    );

    copy.setPathwayId(pathwayId);
    copy.setWalkingBike(walkingBike);
    copy.setRentedVehicle(rentedVehicle);
    copy.setVehicleRentalNetwork(vehicleRentalNetwork);

    return copy;
  }

  /**
   * Should be used for debug logging only
   */
  @Override
  public String toString() {
    return ToStringBuilder
      .of(StreetLeg.class)
      .addObj("from", from)
      .addObj("to", to)
      .addTimeCal("startTime", startTime)
      .addTimeCal("endTime", endTime)
      .addEnum("mode", mode)
      .addNum("distance", distanceMeters, "m")
      .addNum("cost", generalizedCost)
      .addObj("gtfsPathwayId", pathwayId)
      .addObj("legGeometry", legGeometry)
      .addStr("legElevation", legElevation != null ? legElevation.toString() : null)
      .addNum("elevationGained", elevationGained, "m")
      .addNum("elevationLost", elevationLost, "m")
      .addCol("walkSteps", walkSteps)
      .addCol("streetNotes", streetNotes)
      .addBool("walkingBike", walkingBike)
      .addBool("rentedVehicle", rentedVehicle)
      .addStr("bikeRentalNetwork", vehicleRentalNetwork)
      .toString();
  }

  private static Double calculateElevationGained(List<P2<Double>> legElevation) {
    return calculateElevationChange(legElevation, v -> v > 0.0);
  }

  private static Double calculateElevationLost(List<P2<Double>> legElevation) {
    return calculateElevationChange(legElevation, v -> v < 0.0);
  }

  private static Double calculateElevationChange(
    List<P2<Double>> legElevation,
    Predicate<Double> elevationFilter
  ) {
    if (legElevation == null) {
      return null;
    }
    double sum = 0.0;
    Double lastElevation = null;

    for (final P2<Double> p2 : legElevation) {
      double elevation = p2.second;
      if (lastElevation != null) {
        double change = elevation - lastElevation;
        if (elevationFilter.test(change)) {
          sum += Math.abs(change);
        }
      }
      lastElevation = elevation;
    }

    return DoubleUtils.roundTo2Decimals(sum);
  }

  static List<P2<Double>> normalizeElevation(List<P2<Double>> elevation) {
    return elevation == null
      ? null
      : elevation
        .stream()
        .map(it ->
          new P2<>(DoubleUtils.roundTo2Decimals(it.first), DoubleUtils.roundTo2Decimals(it.second))
        )
        .toList();
  }
}
