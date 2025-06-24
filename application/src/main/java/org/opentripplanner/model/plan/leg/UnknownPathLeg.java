package org.opentripplanner.model.plan.leg;

import static org.opentripplanner.model.plan.Itinerary.UNKNOWN;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.model.fare.FareOffer;
import org.opentripplanner.model.plan.Emission;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.utils.time.DurationUtils;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * A transit search may return an unknown transit path. A path consisting of a mix of
 * access, transit , transfers and egress - but where the specific legs are unknown.
 * This leg represent such path.
 */
public class UnknownPathLeg implements Leg {

  private final Place from;
  private final Place to;
  private final ZonedDateTime startTime;
  private final ZonedDateTime endTime;

  private final int nTransfers;

  public UnknownPathLeg(
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
  public Place from() {
    return from;
  }

  @Override
  public Place to() {
    return to;
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
  public boolean isTransitLeg() {
    return false;
  }

  @Override
  public boolean hasSameMode(Leg other) {
    return false;
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
  public double distanceMeters() {
    return UNKNOWN;
  }

  @Override
  @Nullable
  public LineString legGeometry() {
    return null;
  }

  @Override
  public Set<TransitAlert> listTransitAlerts() {
    return Set.of();
  }

  @Nullable
  @Override
  public Emission emissionPerPerson() {
    return null;
  }

  @Nullable
  @Override
  public Leg withEmissionPerPerson(Emission emissionPerPerson) {
    return null;
  }

  @Override
  public int generalizedCost() {
    return RaptorCostCalculator.ZERO_COST;
  }

  @Override
  public List<FareOffer> fareOffers() {
    return List.of();
  }

  public int getNumberOfTransfers() {
    return nTransfers;
  }

  public String description() {
    return ("Unknown transit " + nTransfers + "tx " + DurationUtils.durationToStr(duration()));
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(UnknownPathLeg.class)
      .addObj("from", from)
      .addObj("to", to)
      .addTime("startTime", startTime)
      .addTime("endTime", endTime)
      .addNum("numOfTransfers", nTransfers)
      .toString();
  }
}
