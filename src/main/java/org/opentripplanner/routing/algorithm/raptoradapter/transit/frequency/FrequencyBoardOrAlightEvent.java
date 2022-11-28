package org.opentripplanner.routing.algorithm.raptoradapter.transit.frequency;

import java.time.LocalDate;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.raptor.spi.RaptorTripPattern;
import org.opentripplanner.raptor.spi.RaptorTripScheduleBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.DefaultTripSchedule;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripPatternForDates;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.TripTimes;

/**
 * Represents a result of a {@link RaptorTripScheduleSearch}, with materialized {@link TripTimes}.
 * The concrete class will contain information on how to present the times to the user, as they
 * contain both the headway and travel time as the duration for RAPTOR, on order to prevent too
 * quick re-boarding after alighting.
 * <p>
 * Implementation Notes!
 * <p>
 * This class is both a board-or-alight event and a {code RaptorTripSchedule}. The board-and-alight
 * is used to board a Trip in Raptor and then thrown away, while the Raptor trip schedule is kept
 * for later to construct the itinerary. The event is used frequently, while the trip is used
 * infrequently. So splitting this into two classes and do lazy materialization of the trip would
 * save some resources. This kind of optimization is probably easier to do after a a clean up of the
 * internal OTP transit model.
 */
abstract class FrequencyBoardOrAlightEvent<T extends DefaultTripSchedule>
  implements RaptorTripScheduleBoardOrAlightEvent<T>, TripSchedule {

  protected final TripPatternForDates raptorTripPattern;
  protected final TripTimes tripTimes;
  protected final int stopPositionInPattern;
  protected final int departureTime;
  protected final int offset;
  protected final int headway;
  protected final LocalDate serviceDate;
  private final Accessibility wheelChairBoarding;

  public FrequencyBoardOrAlightEvent(
    TripPatternForDates raptorTripPattern,
    TripTimes tripTimes,
    int stopPositionInPattern,
    int departureTime,
    int offset,
    int headway,
    LocalDate serviceDate
  ) {
    this.raptorTripPattern = raptorTripPattern;
    this.tripTimes = tripTimes;
    this.stopPositionInPattern = stopPositionInPattern;
    this.departureTime = departureTime;
    this.offset = offset;
    this.headway = headway;
    this.serviceDate = serviceDate;
    this.wheelChairBoarding = tripTimes.getWheelchairAccessibility();
  }

  /* RaptorTripScheduleBoardOrAlightEvent implementation */

  @Override
  public int getTripIndex() {
    return tripTimes.getDepartureTime(0) + offset;
  }

  @Override
  public T getTrip() {
    return (T) this;
  }

  @Override
  public int getStopPositionInPattern() {
    return stopPositionInPattern;
  }

  @Override
  public int getTime() {
    return departureTime + offset;
  }

  @Override
  public RaptorTransferConstraint getTransferConstraint() {
    return RaptorTransferConstraint.REGULAR_TRANSFER;
  }

  /* RaptorTripSchedule implementation */

  @Override
  public int tripSortIndex() {
    return tripTimes.getDepartureTime(0) + offset;
  }

  @Override
  public abstract int arrival(int stopPosInPattern);

  @Override
  public abstract int departure(int stopPosInPattern);

  @Override
  public RaptorTripPattern pattern() {
    return raptorTripPattern;
  }

  @Override
  public int transitReluctanceFactorIndex() {
    return raptorTripPattern.transitReluctanceFactorIndex();
  }

  /* TripSchedule implementation */

  @Override
  public TripTimes getOriginalTripTimes() {
    return tripTimes;
  }

  @Override
  public TripPattern getOriginalTripPattern() {
    return raptorTripPattern.getTripPattern().getPattern();
  }

  @Override
  public LocalDate getServiceDate() {
    return serviceDate;
  }

  @Override
  public boolean isFrequencyBasedTrip() {
    return true;
  }

  @Override
  public int frequencyHeadwayInSeconds() {
    return headway;
  }

  @Override
  public Accessibility wheelchairBoarding() {
    return wheelChairBoarding;
  }
}
