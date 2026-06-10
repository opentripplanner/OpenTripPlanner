package org.opentripplanner.ext.carpooling.model;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.transit.model.framework.AbstractTransitEntity;

/**
 * Represents a stop along a carpool trip route with occupancy and timing information.
 * Stops are ordered sequentially along the route.
 */
public class CarpoolStop extends AbstractTransitEntity<CarpoolStop, CarpoolStopBuilder> {

  /** Default onboard count per stop (1 = driver only) when no occupancy information is provided. */
  public static final int DEFAULT_ONBOARD_COUNT = 1;

  /**
   * Default per-stop deviation budget used when the SIRI feed does not supply a
   * {@code latestExpectedArrivalTime} for the stop.
   */
  public static final Duration DEFAULT_DEVIATION_BUDGET = Duration.ofMinutes(15);

  private final WgsCoordinate coordinate;
  private final ZonedDateTime aimedArrivalTime;
  private final ZonedDateTime expectedArrivalTime;
  private final ZonedDateTime latestExpectedArrivalTime;
  private final ZonedDateTime aimedDepartureTime;
  private final ZonedDateTime expectedDepartureTime;
  private final int onboardCount;
  private final Duration deviationBudget;

  public CarpoolStop(CarpoolStopBuilder builder) {
    super(builder.getId());
    this.coordinate = Objects.requireNonNull(builder.coordinate());
    this.expectedArrivalTime = builder.expectedArrivalTime();
    this.aimedArrivalTime = builder.aimedArrivalTime();
    this.latestExpectedArrivalTime = builder.latestExpectedArrivalTime();
    this.expectedDepartureTime = builder.expectedDepartureTime();
    this.aimedDepartureTime = builder.aimedDepartureTime();
    this.onboardCount = builder.onboardCount();
    this.deviationBudget = builder.deviationBudget();
  }

  public static CarpoolStopBuilder of(FeedScopedId id) {
    return new CarpoolStopBuilder(id);
  }

  public static CarpoolStopBuilder of(CarpoolStop carpoolStop) {
    return new CarpoolStopBuilder(carpoolStop);
  }

  public WgsCoordinate getCoordinate() {
    return coordinate;
  }

  /**
   * @return The aimed arrival time, or null if not applicable (e.g., origin stop)
   */
  @Nullable
  public ZonedDateTime getAimedArrivalTime() {
    return aimedArrivalTime;
  }

  /**
   * @return The expected arrival time, or null if not applicable (e.g., origin stop)
   */
  @Nullable
  public ZonedDateTime getExpectedArrivalTime() {
    return expectedArrivalTime;
  }

  /**
   * @return The latest expected arrival time, or null if not provided
   */
  @Nullable
  public ZonedDateTime getLatestExpectedArrivalTime() {
    return latestExpectedArrivalTime;
  }

  /**
   * @return The aimed departure time, or null if not applicable (e.g., destination stop)
   */
  @Nullable
  public ZonedDateTime getAimedDepartureTime() {
    return aimedDepartureTime;
  }

  /**
   * @return The expected departure time, or null if not applicable (e.g., destination stop)
   */
  @Nullable
  public ZonedDateTime getExpectedDepartureTime() {
    return expectedDepartureTime;
  }

  /**
   * @return The number of passengers onboard (including the driver) when departing this stop
   */
  public int getOnboardCount() {
    return onboardCount;
  }

  /**
   * Returns the remaining slack the carpool may consume before this stop without breaking the
   * driver's commitment to passengers already onboard. This is <em>not</em> the original
   * commitment from the SIRI feed: as the trip is updated with additional SIRI messages,
   * the budget shrinks as prior detours eat into it.
   * A value of {@link Duration#ZERO} means no further deviation is acceptable here.
   */
  public Duration getDeviationBudget() {
    return deviationBudget;
  }

  @Override
  public boolean sameAs(CarpoolStop other) {
    return false;
  }

  @Override
  public CarpoolStopBuilder copy() {
    return new CarpoolStopBuilder(this);
  }
}
