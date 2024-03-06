package org.opentripplanner.raptor.api.request;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;

/**
 * Mutable version of {@link SearchParams}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
@SuppressWarnings("UnusedReturnValue")
public class SearchParamsBuilder<T extends RaptorTripSchedule> {

  private final RaptorRequestBuilder<T> parent;
  private final Collection<RaptorAccessEgress> accessPaths = new ArrayList<>();
  private final Collection<RaptorAccessEgress> egressPaths = new ArrayList<>();
  // Search
  private int earliestDepartureTime;
  private int latestArrivalTime;
  private int searchWindowInSeconds;
  private boolean preferLateArrival;
  private int numberOfAdditionalTransfers;
  private int maxNumberOfTransfers;
  private boolean timetable;
  private boolean constrainedTransfers;
  private boolean allowEmptyAccessEgressPaths;

  public SearchParamsBuilder(RaptorRequestBuilder<T> parent, SearchParams defaults) {
    this.parent = parent;
    this.earliestDepartureTime = defaults.earliestDepartureTime();
    this.latestArrivalTime = defaults.latestArrivalTime();
    this.searchWindowInSeconds = defaults.searchWindowInSeconds();
    this.preferLateArrival = defaults.preferLateArrival();
    this.numberOfAdditionalTransfers = defaults.numberOfAdditionalTransfers();
    this.maxNumberOfTransfers = defaults.maxNumberOfTransfers();
    this.timetable = defaults.timetable();
    this.constrainedTransfers = defaults.constrainedTransfers();
    this.accessPaths.addAll(defaults.accessPaths());
    this.egressPaths.addAll(defaults.egressPaths());
    this.allowEmptyAccessEgressPaths = defaults.allowEmptyAccessEgressPaths();
  }

  public int earliestDepartureTime() {
    return earliestDepartureTime;
  }

  public SearchParamsBuilder<T> earliestDepartureTime(int earliestDepartureTime) {
    this.earliestDepartureTime = earliestDepartureTime;
    return this;
  }

  public int latestArrivalTime() {
    return latestArrivalTime;
  }

  public SearchParamsBuilder<T> latestArrivalTime(int latestArrivalTime) {
    this.latestArrivalTime = latestArrivalTime;
    return this;
  }

  public int searchWindowInSeconds() {
    return searchWindowInSeconds;
  }

  public SearchParamsBuilder<T> searchWindowInSeconds(int searchWindowInSeconds) {
    this.searchWindowInSeconds = searchWindowInSeconds;
    return this;
  }

  public SearchParamsBuilder<T> searchWindow(Duration searchWindow) {
    this.searchWindowInSeconds =
      searchWindow == null ? RaptorConstants.NOT_SET : (int) searchWindow.toSeconds();
    return this;
  }

  /**
   * Do one RangeRaptor iteration. This disable the dynamic resolved search-window Alias for calling
   * {@code searchWindow(Duration.ZERO)}.
   */
  public SearchParamsBuilder<T> searchOneIterationOnly() {
    return searchWindowInSeconds(0);
  }

  public boolean preferLateArrival() {
    return preferLateArrival;
  }

  public SearchParamsBuilder<T> preferLateArrival(boolean enable) {
    this.preferLateArrival = enable;
    return this;
  }

  public int numberOfAdditionalTransfers() {
    return numberOfAdditionalTransfers;
  }

  public SearchParamsBuilder<T> numberOfAdditionalTransfers(int numberOfAdditionalTransfers) {
    this.numberOfAdditionalTransfers = numberOfAdditionalTransfers;
    return this;
  }

  public int maxNumberOfTransfers() {
    return maxNumberOfTransfers;
  }

  public SearchParamsBuilder<T> maxNumberOfTransfers(int maxNumberOfTransfers) {
    this.maxNumberOfTransfers = maxNumberOfTransfers;
    return this;
  }

  public boolean timetable() {
    return timetable;
  }

  public SearchParamsBuilder<T> timetable(boolean enable) {
    this.timetable = enable;
    return this;
  }

  public boolean constrainedTransfers() {
    return constrainedTransfers;
  }

  public SearchParamsBuilder<T> constrainedTransfers(boolean enable) {
    this.constrainedTransfers = enable;
    return this;
  }

  public Collection<RaptorAccessEgress> accessPaths() {
    return accessPaths;
  }

  public SearchParamsBuilder<T> addAccessPaths(
    Collection<? extends RaptorAccessEgress> accessPaths
  ) {
    this.accessPaths.addAll(accessPaths);
    return this;
  }

  public SearchParamsBuilder<T> addAccessPaths(RaptorAccessEgress... accessPaths) {
    return addAccessPaths(Arrays.asList(accessPaths));
  }

  public Collection<RaptorAccessEgress> egressPaths() {
    return egressPaths;
  }

  public SearchParamsBuilder<T> addEgressPaths(
    Collection<? extends RaptorAccessEgress> egressPaths
  ) {
    this.egressPaths.addAll(egressPaths);
    return this;
  }

  public SearchParamsBuilder<T> addEgressPaths(RaptorAccessEgress... egressPaths) {
    return addEgressPaths(Arrays.asList(egressPaths));
  }

  public SearchParamsBuilder<T> allowEmptyAccessEgressPaths(boolean allowEmptyEgressPaths) {
    this.allowEmptyAccessEgressPaths = allowEmptyEgressPaths;
    return this;
  }

  public boolean allowEmptyAccessEgressPaths() {
    return allowEmptyAccessEgressPaths;
  }

  public RaptorRequest<T> build() {
    return parent.build();
  }

  /** This is public to allow tests to build just search params */
  public SearchParams buildSearchParam() {
    return new SearchParams(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(SearchParams.class)
      .addServiceTime("earliestDepartureTime", earliestDepartureTime, RaptorConstants.TIME_NOT_SET)
      .addServiceTime("latestArrivalTime", latestArrivalTime, RaptorConstants.TIME_NOT_SET)
      .addDurationSec("searchWindow", searchWindowInSeconds)
      .addBoolIfTrue("departAsLateAsPossible", preferLateArrival)
      .addNum("numberOfAdditionalTransfers", numberOfAdditionalTransfers)
      .addCollection("accessPaths", accessPaths, 5)
      .addCollection("egressPaths", egressPaths, 5)
      .toString();
  }
}
