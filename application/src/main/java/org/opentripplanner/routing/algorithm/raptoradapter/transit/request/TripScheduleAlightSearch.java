package org.opentripplanner.routing.algorithm.raptoradapter.transit.request;

import java.util.function.IntUnaryOperator;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransferConstraint;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.SearchDirection;
import org.opentripplanner.raptor.spi.RaptorBoardOrAlightEvent;
import org.opentripplanner.raptor.spi.RaptorTripScheduleSearch;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The purpose of this class is to optimize the search for a trip schedule for a given pattern and
 * stop. Normally the search scan from the upper bound index and down, it can do so because the
 * trips are ordered after the FIRST stop alight times. We also assume that trips do not pass each
 * other; Hence trips in service on a given day will also be in order for all other stops. For trips
 * operating on different service days (no overlapping) this assumption is not necessary true.
 * <p>
 * The search use a binary search if the number of trip schedules is above a given threshold. A
 * linear search is slow when the number of schedules is very large, let say more than 300 trip
 * schedules.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public final class TripScheduleAlightSearch<T extends RaptorTripSchedule>
  implements RaptorTripScheduleSearch<T>, RaptorBoardOrAlightEvent<T> {

  private final TripSearchTimetable<T> timetable;
  private final int nTrips;
  private final int binarySearchThreshold;

  private int latestAlightTime;
  private int stopPositionInPattern;
  private IntUnaryOperator arrivalTimes;

  private T candidateTrip;
  private int candidateTripIndex = RaptorConstants.NOT_FOUND;

  /**
   * Use {@link TripScheduleSearchFactory#create(SearchDirection, TripSearchTimetable)} to create a
   * trip schedule search.
   */
  TripScheduleAlightSearch(TripSearchTimetable<T> timetable, int binarySearchThreshold) {
    this.timetable = timetable;
    this.nTrips = timetable.numberOfTripSchedules();
    this.binarySearchThreshold = binarySearchThreshold;
  }

  /* TripScheduleBoardOrAlightEvent implementation using fly-weight pattern */

  @Override
  public T trip() {
    return candidateTrip;
  }

  @Override
  public int tripIndex() {
    return candidateTripIndex;
  }

  @Override
  public int time() {
    return candidateTrip.arrival(stopPositionInPattern);
  }

  @Override
  public int earliestBoardTime() {
    return latestAlightTime;
  }

  @Override
  public int stopPositionInPattern() {
    return stopPositionInPattern;
  }

  @Override
  public RaptorTransferConstraint transferConstraint() {
    return RaptorTransferConstraint.REGULAR_TRANSFER;
  }

  @Override
  public boolean empty() {
    return candidateTripIndex == RaptorConstants.NOT_FOUND;
  }

  /* TripScheduleSearch implementation */

  /**
   * Find the last trip leaving from the given stop BEFORE the the {@code latestAlightTime}, but
   * after the given trip ({@code tripIndexLowerBound}).
   *
   * @param latestAlightTime      The latest acceptable alight time (exclusive).
   * @param stopPositionInPattern The stop to board.
   * @param tripIndexLowerBound   Upper bound for trip index to search for (exclusive).
   */
  @Override
  public RaptorBoardOrAlightEvent<T> search(
    int latestAlightTime,
    int stopPositionInPattern,
    int tripIndexLowerBound
  ) {
    this.latestAlightTime = latestAlightTime;
    this.stopPositionInPattern = stopPositionInPattern;
    this.arrivalTimes = timetable.getArrivalTimes(stopPositionInPattern);
    this.candidateTrip = null;
    this.candidateTripIndex = RaptorConstants.NOT_FOUND;

    // No previous trip is found
    if (tripIndexLowerBound == UNBOUNDED_TRIP_INDEX) {
      if (nTrips > binarySearchThreshold) {
        return findFirstBoardingOptimizedForLargeSetOfTrips();
      } else {
        return findBoardingSearchForwardInTime(0);
      }
    }
    // We have already found a candidate in a previous search;
    // Hence searching forward from the lower bound is the fastest way to proceed.
    // We have to add 1 to the lower bound for go from exclusive to inclusive
    return findBoardingSearchForwardInTime(tripIndexLowerBound + 1);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TripScheduleAlightSearch.class)
      .addObj("nTrips", nTrips)
      .addObj("latestAlightTime", latestAlightTime)
      .addObj("stopPos", stopPositionInPattern)
      .addObj("tripIndex", candidateTripIndex)
      .addObj("trip", candidateTrip)
      .toString();
  }

  /* private methods */

  private RaptorBoardOrAlightEvent<T> findFirstBoardingOptimizedForLargeSetOfTrips() {
    int indexBestGuess = binarySearchForTripIndex();

    // Use the best guess from the binary search to look for a candidate trip
    // We can not use upper bound to exit the search. We need to continue
    // until we find a valid trip in service.
    var result = findBoardingSearchForwardInTime(indexBestGuess);

    // If a valid result is found and we can return
    if (!result.empty()) {
      return this;
    }

    // No trip schedule above the best guess was found. This may happen if enough
    // trips are not in service.
    //
    // So we have to search for the first valid trip schedule before that.
    return findBoardingSearchBackwardsInTime(indexBestGuess);
  }

  /**
   * This method search for the last scheduled trip arriving before the {@code latestAlightTime}.
   * Only trips with a trip index greater than the given {@code tripIndexLowerBound} is considered.
   *
   * @param tripIndexLowerBound The trip index lower bound, where search start (inclusive).
   */
  @Nullable
  private RaptorBoardOrAlightEvent<T> findBoardingSearchForwardInTime(int tripIndexLowerBound) {
    for (int i = tripIndexLowerBound; i < nTrips; ++i) {
      if (arrivalTimes.applyAsInt(i) <= latestAlightTime) {
        candidateTripIndex = i;
      } else {
        // this trip arrives too late. We can break out of the loop since
        // trips are sorted by departure time (trips in given schedule)
        // Trips passing another trip is not accounted for if both are in service.
        break;
      }
    }
    if (candidateTripIndex != RaptorConstants.NOT_FOUND) {
      candidateTrip = timetable.getTripSchedule(candidateTripIndex);
    }
    return this;
  }

  /**
   * This method search for the last scheduled trip arrival before the {@code latestAlightTime}.
   * Only trips with a trip index in the range: {@code [0..tripIndexUpperBound-1]} is considered.
   *
   * @param tripIndexUpperBound The trip index upper bound, where search end (exclusive).
   */
  private RaptorBoardOrAlightEvent<T> findBoardingSearchBackwardsInTime(
    final int tripIndexUpperBound
  ) {
    for (int i = tripIndexUpperBound - 1; i >= 0; --i) {
      if (arrivalTimes.applyAsInt(i) <= latestAlightTime) {
        candidateTrip = timetable.getTripSchedule(i);
        candidateTripIndex = i;
        return this;
      }
    }
    return this;
  }

  /**
   * Do a binary search to find the approximate lower bound index for where to start the search.
   * We IGNORE if the trip schedule is in service.
   * <p/>
   * This is just a guess, and we return when the trip with a best valid arrival is in the range of
   * the next {@link #binarySearchThreshold}.
   *
   * @return a better lower bound index (inclusive)
   */
  private int binarySearchForTripIndex() {
    int lower = 0, upper = nTrips;

    // Do a binary search to find where to start the search.
    // We IGNORE if the trip schedule is in service.
    while (upper - lower > binarySearchThreshold) {
      int m = (lower + upper) / 2;

      if (arrivalTimes.applyAsInt(m) <= latestAlightTime) {
        lower = m;
      } else {
        upper = m;
      }
    }
    return lower;
  }
}
