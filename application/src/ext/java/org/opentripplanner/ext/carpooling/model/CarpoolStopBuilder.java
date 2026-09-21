package org.opentripplanner.ext.carpooling.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractEntityBuilder;

/**
 * Builder for {@link CarpoolStop} instances.
 */
public class CarpoolStopBuilder extends AbstractEntityBuilder<CarpoolStop, CarpoolStopBuilder> {

  private WgsCoordinate coordinate;

  private ZonedDateTime expectedArrivalTime;
  private ZonedDateTime aimedArrivalTime;
  private ZonedDateTime latestExpectedArrivalTime;
  private ZonedDateTime expectedDepartureTime;
  private ZonedDateTime aimedDepartureTime;
  private int onboardCount = CarpoolStop.DEFAULT_ONBOARD_COUNT;

  private Duration deviationBudget = CarpoolStop.DEFAULT_DEVIATION_BUDGET;

  CarpoolStopBuilder(FeedScopedId id) {
    super(id);
  }

  CarpoolStopBuilder(CarpoolStop original) {
    super(original);
    this.coordinate = original.getCoordinate();
    this.expectedArrivalTime = original.getExpectedArrivalTime();
    this.aimedArrivalTime = original.getAimedArrivalTime();
    this.latestExpectedArrivalTime = original.getLatestExpectedArrivalTime();
    this.expectedDepartureTime = original.getExpectedDepartureTime();
    this.aimedDepartureTime = original.getAimedDepartureTime();
    this.onboardCount = original.getOnboardCount();
    this.deviationBudget = original.getDeviationBudget();
  }

  @Override
  protected CarpoolStop buildFromValues() {
    return new CarpoolStop(this);
  }

  public CarpoolStopBuilder withCoordinate(WgsCoordinate coordinate) {
    this.coordinate = coordinate;
    return this;
  }

  public CarpoolStopBuilder withExpectedArrivalTime(ZonedDateTime expectedArrivalTime) {
    this.expectedArrivalTime = expectedArrivalTime;
    return this;
  }

  public CarpoolStopBuilder withAimedArrivalTime(ZonedDateTime aimedArrivalTime) {
    this.aimedArrivalTime = aimedArrivalTime;
    return this;
  }

  public CarpoolStopBuilder withLatestExpectedArrivalTime(ZonedDateTime latestExpectedArrivalTime) {
    this.latestExpectedArrivalTime = latestExpectedArrivalTime;
    return this;
  }

  public CarpoolStopBuilder withExpectedDepartureTime(ZonedDateTime expectedDepartureTime) {
    this.expectedDepartureTime = expectedDepartureTime;
    return this;
  }

  public CarpoolStopBuilder withAimedDepartureTime(ZonedDateTime aimedDepartureTime) {
    this.aimedDepartureTime = aimedDepartureTime;
    return this;
  }

  public CarpoolStopBuilder withOnboardCount(int onboardCount) {
    this.onboardCount = onboardCount;
    return this;
  }

  /**
   * Sets the per-stop deviation budget. See {@link CarpoolStop#getDeviationBudget()} for the
   * semantics of the value.
   *
   * @param deviationBudget remaining slack at this stop; must be non-null. Use
   *                        {@link Duration#ZERO} for stops where no further deviation is
   *                        acceptable (always for the trip origin).
   * @throws NullPointerException if {@code deviationBudget} is null
   */
  public CarpoolStopBuilder withDeviationBudget(Duration deviationBudget) {
    this.deviationBudget = Objects.requireNonNull(deviationBudget);
    return this;
  }

  public WgsCoordinate coordinate() {
    return coordinate;
  }

  public ZonedDateTime expectedArrivalTime() {
    return expectedArrivalTime;
  }

  public ZonedDateTime aimedArrivalTime() {
    return aimedArrivalTime;
  }

  public ZonedDateTime latestExpectedArrivalTime() {
    return latestExpectedArrivalTime;
  }

  public ZonedDateTime expectedDepartureTime() {
    return expectedDepartureTime;
  }

  public ZonedDateTime aimedDepartureTime() {
    return aimedDepartureTime;
  }

  public int onboardCount() {
    return onboardCount;
  }

  public Duration deviationBudget() {
    return deviationBudget;
  }
}
