package org.opentripplanner.raptor.api.request;

import static org.opentripplanner.raptor.api.request.RaptorRequest.assertProperty;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTransfer;

/**
 * The responsibility of this class is to encapsulate a Range Raptor travel request search
 * parameters.
 */
public class SearchParams {

  /**
   * The TIME_NOT_SET constant is used to mark a parameter as not set, we uses a large negative
   * value to be sure we are not in conflict with a valid service time.
   * <p>
   * This would potentially support negative service times, which is not allowed, but in rear
   * cases/system happens when DST is adjusted.
   * <p>
   * We do not use {@link Integer#MIN_VALUE} because this could potentially lead to overflow
   * situations which would be very hard to debug. Add -1 to MIN_VALUE and you get a positive number
   * - not an exception.
   */
  public static final int TIME_NOT_SET = -9_999_999;

  public static final int NOT_SET = -1;

  private final int earliestDepartureTime;
  private final int latestArrivalTime;
  private final int searchWindowInSeconds;
  private final boolean preferLateArrival;
  private final int numberOfAdditionalTransfers;
  private final int maxNumberOfTransfers;
  private final double relaxCostAtDestination;
  private final boolean timetableEnabled;
  private final boolean constrainedTransfersEnabled;
  private final Collection<RaptorAccessEgress> accessPaths;
  private final Collection<RaptorAccessEgress> egressPaths;
  private final boolean allowEmptyEgressPaths;

  /**
   * Default values is defined in the default constructor.
   */
  private SearchParams() {
    earliestDepartureTime = TIME_NOT_SET;
    latestArrivalTime = TIME_NOT_SET;
    searchWindowInSeconds = NOT_SET;
    preferLateArrival = false;
    numberOfAdditionalTransfers = 5;
    maxNumberOfTransfers = NOT_SET;
    relaxCostAtDestination = NOT_SET;
    timetableEnabled = false;
    constrainedTransfersEnabled = false;
    accessPaths = List.of();
    egressPaths = List.of();
    allowEmptyEgressPaths = false;
  }

  SearchParams(SearchParamsBuilder<?> builder) {
    this.earliestDepartureTime = builder.earliestDepartureTime();
    this.latestArrivalTime = builder.latestArrivalTime();
    this.searchWindowInSeconds = builder.searchWindowInSeconds();
    this.preferLateArrival = builder.preferLateArrival();
    this.numberOfAdditionalTransfers = builder.numberOfAdditionalTransfers();
    this.maxNumberOfTransfers = builder.maxNumberOfTransfers();
    this.relaxCostAtDestination = builder.relaxCostAtDestination();
    this.timetableEnabled = builder.timetableEnabled();
    this.constrainedTransfersEnabled = builder.constrainedTransfersEnabled();
    this.accessPaths = List.copyOf(builder.accessPaths());
    this.egressPaths = List.copyOf(builder.egressPaths());
    this.allowEmptyEgressPaths = builder.allowEmptyEgressPaths();
  }

  /**
   * TODO OTP2 Cleanup doc:
   * The beginning of the departure window, in seconds since midnight. Inclusive.
   * The earliest a journey may start in seconds since midnight. In the case of a 'depart after'
   * search this is a required. In the case of a 'arrive by' search this is optional.
   * <p/>
   * In Raptor terms this maps to the beginning of the departure window. The {@link #searchWindowInSeconds()}
   * is used to find the end of the time window.
   * <p/>
   * Required. Must be a positive integer, seconds since midnight(transit service time).
   * Required for 'depart after'. Must be a positive integer, seconds since midnight(transit service time).
   */
  public int earliestDepartureTime() {
    return earliestDepartureTime;
  }

  public boolean isEarliestDepartureTimeSet() {
    return earliestDepartureTime != TIME_NOT_SET;
  }

  /**
   * TODO OTP2 Cleanup doc:
   * The end of the departure window, in seconds since midnight. Exclusive.
   * The latest a journey may arrive in seconds since midnight. In the case of a 'arrive by'
   * search this is a required. In the case of a 'depart after' search this is optional.
   * <p/>
   * Required. Must be a positive integer, seconds since midnight(transit service time).
   * In Raptor terms this maps to the beginning of the departure window of a reverse search. The
   * {@link #searchWindowInSeconds()} is used to find the end of the time window.
   * <p/>
   * Required for 'arrive by'. Must be a positive integer, seconds since midnight(transit service time).
   */
  public int latestArrivalTime() {
    return latestArrivalTime;
  }

  public boolean isLatestArrivalTimeSet() {
    return latestArrivalTime != TIME_NOT_SET;
  }

  /**
   * TODO OTP2 Cleanup doc:
   * The time window used to search. The unit is seconds. For a *depart by search*, this is
   * added to the 'earliestDepartureTime' to find the 'latestDepartureTime'. For a *arrive
   * by search* this is used to calculate the 'earliestArrivalTime'. The algorithm will find
   * all optimal travels within the given time window.
   * <p/>
   * Set the search window to 0 (zero) to run 1 iteration.
   * <p/>
   * Required. Must be a positive integer or 0(zero).
   */
  public int searchWindowInSeconds() {
    return searchWindowInSeconds;
  }

  public boolean isSearchWindowSet() {
    return searchWindowInSeconds != NOT_SET;
  }

  public boolean searchOneIterationOnly() {
    return searchWindowInSeconds == 0;
  }

  /**
   * Keep the latest departures arriving before the given latest-arrival-time(LAT). LAT is required
   * if this parameter is set. This parameter is not allowed if the {@link #timetableEnabled} is
   * enabled.
   */
  public boolean preferLateArrival() {
    return preferLateArrival;
  }

  /**
   * RangeRaptor is designed to search until the destination is reached and then {@code
   * numberOfAdditionalTransfers} more rounds.
   * <p/>
   * The default value is 5.
   */
  public int numberOfAdditionalTransfers() {
    return numberOfAdditionalTransfers;
  }

  /**
   * This is an absolute limit to the number of transfers. The preferred way to limit the transfers
   * is to use the {@link #numberOfAdditionalTransfers()}.
   * <p/>
   * The default is to use the limit in the tuning parameters {@link RaptorTuningParameters#maxNumberOfTransfers()}.
   */
  public int maxNumberOfTransfers() {
    return maxNumberOfTransfers;
  }

  public boolean isMaxNumberOfTransfersSet() {
    return maxNumberOfTransfers != NOT_SET;
  }

  /**
   * Whether to accept non-optimal trips if they are close enough - if and only if they represent an
   * optimal path for their given iteration. In other words this slack only relaxes the pareto
   * comparison at the destination.
   * <p/>
   * Let {@code c} be the existing minimum pareto optimal cost to beat. Then a trip with cost
   * {@code c'} is accepted if the following is true:
   * <pre>
   * c' < Math.round(c * relaxCostAtDestination)
   * </pre>
   * If the value is less than 0.0 a normal '<' comparison is performed.
   * <p/>
   * TODO - When setting this above 1.0, we get some unwanted results. We should have a filter to remove those
   * TODO - results. See issue https://github.com/entur/r5/issues/28
   * <p/>
   * The default value is -1.0 (disabled)
   */
  public double relaxCostAtDestination() {
    return relaxCostAtDestination;
  }

  /**
   * Time table allow a Journey to be included in the result if it depart from the origin AFTER
   * another Journey, even if the first departure have lower cost, number of transfers, and shorter
   * travel time. For two Journeys that depart at the same time only the best one will be included
   * (both if they are mutually dominating each other).
   * <p/>
   * Setting this parameter to "TRUE" will increase the number of paths returned. The performance
   * impact is small since the check only affect the pareto check at the destination.
   * <p/>
   * The default value is FALSE.
   */
  public boolean timetableEnabled() {
    return timetableEnabled;
  }

  /**
   * If enabled guaranteed transfers are used during routing, if not they are ignored. Some of the
   * profiles do not support guaranteed transfers, for these profiles this flag is ignored.
   * Transfers are supported for all profiles returning paths.
   */
  public boolean constrainedTransfersEnabled() {
    return constrainedTransfersEnabled;
  }

  /**
   * List of access paths from the origin to all transit stops using the street network.
   * <p/>
   * Required, at least one access path must exist.
   */
  public Collection<RaptorAccessEgress> accessPaths() {
    return accessPaths;
  }

  /**
   * List of all possible egress paths to reach the destination using the street network.
   * <p>
   * NOTE! The {@link RaptorTransfer#stop()} is the stop where the egress path start, NOT the
   * destination - think of it as a reversed path.
   * <p/>
   * Required, at least one egress path must exist.
   */
  public Collection<RaptorAccessEgress> egressPaths() {
    return egressPaths;
  }

  /**
   * If enabled, the check for egress paths is skipped. This is required when wanting to eg. run a
   * separate heuristic search, with no pre-defined destinations.
   */
  public boolean allowEmptyEgressPaths() {
    return allowEmptyEgressPaths;
  }

  /**
   * Get the maximum duration of any access or egress path in seconds.
   */
  public int getAccessEgressMaxDurationSeconds() {
    return Math.max(
      accessPaths.stream().mapToInt(RaptorAccessEgress::durationInSeconds).max().orElse(0),
      egressPaths.stream().mapToInt(RaptorAccessEgress::durationInSeconds).max().orElse(0)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(
      earliestDepartureTime,
      latestArrivalTime,
      searchWindowInSeconds,
      preferLateArrival,
      accessPaths,
      egressPaths,
      numberOfAdditionalTransfers
    );
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SearchParams that = (SearchParams) o;
    return (
      earliestDepartureTime == that.earliestDepartureTime &&
      latestArrivalTime == that.latestArrivalTime &&
      searchWindowInSeconds == that.searchWindowInSeconds &&
      preferLateArrival == that.preferLateArrival &&
      numberOfAdditionalTransfers == that.numberOfAdditionalTransfers &&
      accessPaths.equals(that.accessPaths) &&
      egressPaths.equals(that.egressPaths)
    );
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(SearchParams.class)
      .addServiceTime("earliestDepartureTime", earliestDepartureTime, TIME_NOT_SET)
      .addServiceTime("latestArrivalTime", latestArrivalTime, TIME_NOT_SET)
      .addDurationSec("searchWindow", searchWindowInSeconds)
      .addBoolIfTrue("departAsLateAsPossible", preferLateArrival)
      .addNum("numberOfAdditionalTransfers", numberOfAdditionalTransfers)
      .addCollection("accessPaths", accessPaths, 5)
      .addCollection("egressPaths", egressPaths, 5)
      .toString();
  }

  static SearchParams defaults() {
    return new SearchParams();
  }

  /* private methods */

  void verify() {
    assertProperty(
      earliestDepartureTime != TIME_NOT_SET || latestArrivalTime != TIME_NOT_SET,
      "'earliestDepartureTime' or 'latestArrivalTime' is required."
    );
    assertProperty(!accessPaths.isEmpty(), "At least one 'accessPath' is required.");
    assertProperty(
      allowEmptyEgressPaths || !egressPaths.isEmpty(),
      "At least one 'egressPath' is required."
    );
    assertProperty(
      !(preferLateArrival && latestArrivalTime == TIME_NOT_SET),
      "The 'latestArrivalTime' is required when 'departAsLateAsPossible' is set."
    );
    assertProperty(
      !(preferLateArrival && timetableEnabled),
      "The 'departAsLateAsPossible' is not allowed together with 'timetableEnabled'."
    );
  }
}
