package org.opentripplanner.raptor.api.request;

import static org.opentripplanner.raptor.api.request.RaptorRequest.assertProperty;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTransfer;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * The responsibility of this class is to encapsulate a Range Raptor travel request search
 * parameters.
 */
public class SearchParams {

  /**
   * The maximum number of via-locations is used as a check to avoid exploiting the
   * search performance. Consider restricting this further in the upstream services.
   */
  private static final int MAX_VIA_POINTS = 10;

  private final int earliestDepartureTime;
  private final int latestArrivalTime;
  private final int searchWindowInSeconds;
  private final boolean preferLateArrival;
  private final int numberOfAdditionalTransfers;
  private final int maxNumberOfTransfers;
  private final boolean timetable;
  private final boolean constrainedTransfers;
  private final Collection<RaptorAccessEgress> accessPaths;
  private final Collection<RaptorAccessEgress> egressPaths;
  private final List<RaptorViaLocation> viaLocations;

  /**
   * Default values are defined in the default constructor.
   */
  private SearchParams() {
    earliestDepartureTime = RaptorConstants.TIME_NOT_SET;
    latestArrivalTime = RaptorConstants.TIME_NOT_SET;
    searchWindowInSeconds = RaptorConstants.NOT_SET;
    preferLateArrival = false;
    numberOfAdditionalTransfers = 5;
    maxNumberOfTransfers = RaptorConstants.NOT_SET;
    timetable = false;
    constrainedTransfers = false;
    accessPaths = List.of();
    egressPaths = List.of();
    viaLocations = List.of();
  }

  SearchParams(SearchParamsBuilder<?> builder) {
    this.earliestDepartureTime = builder.earliestDepartureTime();
    this.latestArrivalTime = builder.latestArrivalTime();
    this.searchWindowInSeconds = builder.searchWindowInSeconds();
    this.preferLateArrival = builder.preferLateArrival();
    this.numberOfAdditionalTransfers = builder.numberOfAdditionalTransfers();
    this.maxNumberOfTransfers = builder.maxNumberOfTransfers();
    this.timetable = builder.timetable();
    this.constrainedTransfers = builder.constrainedTransfers();
    this.accessPaths = List.copyOf(builder.accessPaths());
    this.egressPaths = List.copyOf(builder.egressPaths());
    this.viaLocations = List.copyOf(builder.viaLocations());
  }

  /**
   * The earliest a journey can depart from the origin. The unit is seconds since midnight.
   * Inclusive.
   * <p>
   * In the case of a 'depart after' search this is a required. In the case of a 'arrive by' search
   * this is optional, but it will improve performance if it is set.
   */
  public int earliestDepartureTime() {
    return earliestDepartureTime;
  }

  public boolean isEarliestDepartureTimeSet() {
    return earliestDepartureTime != RaptorConstants.TIME_NOT_SET;
  }

  /**
   * The latest a journey may arrive at the destination. The unit is seconds since midnight.
   * Exclusive.
   * <p>
   * In the case of a 'arrive by' search this is a required. In the case of a 'depart after' search
   * this is optional, but it will improve performance if it is set.
   */
  public int latestArrivalTime() {
    return latestArrivalTime;
  }

  public boolean isLatestArrivalTimeSet() {
    return latestArrivalTime != RaptorConstants.TIME_NOT_SET;
  }

  /**
   * The time window used to search. The unit is seconds.
   * <p>
   * For a *depart-by-search*, this is added to the 'earliestDepartureTime' to find the
   * 'latestDepartureTime'.
   * <p>
   * For an *arrive-by-search* this is used to calculate the 'earliestArrivalTime'. The algorithm
   * will find all optimal travels within the given time window.
   * <p>
   * Set the search window to 0 (zero) to run 1 iteration.
   * <p>
   * Required. Must be a positive integer or 0(zero).
   */
  public int searchWindowInSeconds() {
    return searchWindowInSeconds;
  }

  public boolean isSearchWindowSet() {
    return searchWindowInSeconds != RaptorConstants.NOT_SET;
  }

  public boolean searchOneIterationOnly() {
    return searchWindowInSeconds == 0;
  }

  /**
   * Keep the latest departures arriving before the given latest-arrival-time(LAT). LAT is required
   * if this parameter is set. This parameter is not allowed if the {@link #timetable} is
   * enabled.
   * <p>
   * TODO - Reactor, we should use an Enum value instead of this and 'timetable':
   *      - timePreference : enum TimePreference{ TIMETABLE, DEPART_AFTER, ARRIVE_BY }
   *      - PS! There are some corner cases here. E.g. DEPART_AFTER must work when
   *        edt=null & lat!=null - same for ARRIVE_BY.
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
    return maxNumberOfTransfers != RaptorConstants.NOT_SET;
  }

  /**
   * The timetable flag allow a Journey to be included in the result if it departs from the origin
   * AFTER another Journey, even if the first departure have lower cost, number of transfers, and
   * shorter travel time. For two Journeys that depart at the same time only the best one will be
   * included (both if they are mutually dominating each other).
   * <p/>
   * Setting this parameter to "TRUE" will increase the number of paths returned. The performance
   * impact is small since the check only affect the pareto check at the destination.
   * <p/>
   * The default value is FALSE.
   */
  public boolean timetable() {
    return timetable;
  }

  /**
   * If requested, constrained transfers(guaranteed, stay-seated ..) are used during routing, if
   * not they are ignored. Some profiles do not support constrained transfers, for these profiles
   * constrained transfers are NOT used - the 'constrainedTransfers' flag is ignored. Constrained
   * transfers are supported for all profiles returning paths.
   */
  public boolean constrainedTransfers() {
    return constrainedTransfers;
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
   * List of all possible via locations.
   */
  public List<RaptorViaLocation> viaLocations() {
    return viaLocations;
  }

  /**
   * Get the maximum duration of any access or egress path in seconds.
   */
  public int accessEgressMaxDurationSeconds() {
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
      numberOfAdditionalTransfers,
      accessPaths,
      egressPaths,
      viaLocations
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
      egressPaths.equals(that.egressPaths) &&
      viaLocations.equals(viaLocations)
    );
  }

  @Override
  public String toString() {
    var dft = defaults();
    return ToStringBuilder.of(SearchParams.class)
      .addServiceTime("earliestDepartureTime", earliestDepartureTime, dft.earliestDepartureTime)
      .addServiceTime("latestArrivalTime", latestArrivalTime, dft.latestArrivalTime)
      .addDurationSec("searchWindow", searchWindowInSeconds, dft.searchWindowInSeconds)
      .addBoolIfTrue("departAsLateAsPossible", preferLateArrival)
      .addNum(
        "numberOfAdditionalTransfers",
        numberOfAdditionalTransfers,
        dft.numberOfAdditionalTransfers
      )
      .addCollection("accessPaths", accessPaths, 5, RaptorAccessEgress::defaultToString)
      .addCollection("egressPaths", egressPaths, 5, RaptorAccessEgress::defaultToString)
      .addCollection("via", viaLocations, 5)
      .toString();
  }

  public boolean isVisitViaSearch() {
    return (
      !viaLocations.isEmpty() &&
      viaLocations.stream().noneMatch(RaptorViaLocation::isPassThroughSearch)
    );
  }

  public boolean isPassThroughSearch() {
    return (
      !viaLocations.isEmpty() &&
      viaLocations.stream().allMatch(RaptorViaLocation::isPassThroughSearch)
    );
  }

  static SearchParams defaults() {
    return new SearchParams();
  }

  /* private methods */

  void verify() {
    assertProperty(
      isEarliestDepartureTimeSet() || isLatestArrivalTimeSet(),
      "'earliestDepartureTime' or 'latestArrivalTime' is required."
    );
    assertProperty(!accessPaths.isEmpty(), "At least one 'accessPath' is required.");
    assertProperty(!egressPaths.isEmpty(), "At least one 'egressPath' is required.");
    assertProperty(
      !(preferLateArrival && !isLatestArrivalTimeSet()),
      "The 'latestArrivalTime' is required when 'departAsLateAsPossible' is set."
    );
    assertProperty(
      !(preferLateArrival && timetable),
      "The 'departAsLateAsPossible' is not allowed together with 'timetableEnabled'."
    );
    assertProperty(
      viaLocations.size() <= MAX_VIA_POINTS,
      "The 'viaLocations' exceeds the  maximum number of via-locations (" + MAX_VIA_POINTS + ")."
    );
    assertProperty(
      viaLocations.isEmpty() || isVisitViaSearch() || isPassThroughSearch(),
      "Combining pass-through and regular via-vist it is not allowed: " + viaLocations + "."
    );
  }
}
