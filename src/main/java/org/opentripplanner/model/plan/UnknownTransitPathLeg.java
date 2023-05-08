package org.opentripplanner.model.plan;

import static org.opentripplanner.model.plan.Itinerary.UNKNOWN;

import java.time.ZonedDateTime;
import java.util.List;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.framework.time.DurationUtils;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.model.fare.FareProductUse;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;

/**
 * A transit search may return an unknown transit path. A path consisting of a mix of
 * access, transit , transfers and egress - but where the specific legs are unknown.
 * This leg represent such path.
 */
public class UnknownTransitPathLeg implements Leg {

  private final Place from;
  private final Place to;
  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;

  private final int nTransfers;

  public UnknownTransitPathLeg(
    Place from,
    Place to,
    ZonedDateTime startTime,
    ZonedDateTime endTime,
    int nTransfers
  ) {
    this.from = from;
    this.to = to;
    this.startTime = startTime;
    this.endTime = endTime;
    this.nTransfers = nTransfers;
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
  public ZonedDateTime getStartTime() {
    return startTime;
  }

  @Override
  public ZonedDateTime getEndTime() {
    return endTime;
  }

  @Override
  public boolean isTransitLeg() {
    return false;
  }

  @Override
  public boolean hasSameMode(Leg other) {
    return false;
  }

  @Override
  public double getDistanceMeters() {
    return UNKNOWN;
  }

  @Override
  public LineString getLegGeometry() {
    return null;
  }

  @Override
  public int getGeneralizedCost() {
    return RaptorCostCalculator.ZERO_COST;
  }

  @Override
  public void setFareProducts(List<FareProductUse> products) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<FareProductUse> fareProducts() {
    return List.of();
  }

  public int getNumberOfTransfers() {
    return nTransfers;
  }

  public String description() {
    return ("Unknown transit " + nTransfers + "tx " + DurationUtils.durationToStr(getDuration()));
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(UnknownTransitPathLeg.class)
      .addObj("from", from)
      .addObj("to", to)
      .addTime("startTime", startTime)
      .addTime("endTime", endTime)
      .addNum("numOfTransfers", nTransfers)
      .toString();
  }
}
