package org.opentripplanner.raptor.api.request;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.opentripplanner.framework.tostring.ToStringBuilder;
import org.opentripplanner.raptor.spi.RaptorAccessEgress;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;

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
  private double relaxCostAtDestination;
  private boolean timetableEnabled;
  private boolean constrainedTransfersEnabled;
  private boolean allowEmptyEgressPaths;

  public SearchParamsBuilder(RaptorRequestBuilder<T> parent, SearchParams defaults) {
    this.parent = parent;
    this.earliestDepartureTime = defaults.earliestDepartureTime();
    this.latestArrivalTime = defaults.latestArrivalTime();
    this.searchWindowInSeconds = defaults.searchWindowInSeconds();
    this.preferLateArrival = defaults.preferLateArrival();
    this.numberOfAdditionalTransfers = defaults.numberOfAdditionalTransfers();
    this.maxNumberOfTransfers = defaults.maxNumberOfTransfers();
    this.relaxCostAtDestination = defaults.relaxCostAtDestination();
    this.timetableEnabled = defaults.timetableEnabled();
    this.constrainedTransfersEnabled = defaults.constrainedTransfersEnabled();
    this.accessPaths.addAll(defaults.accessPaths());
    this.egressPaths.addAll(defaults.egressPaths());
    this.allowEmptyEgressPaths = defaults.allowEmptyEgressPaths();
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
      searchWindow == null ? SearchParams.NOT_SET : (int) searchWindow.toSeconds();
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

  public double relaxCostAtDestination() {
    return relaxCostAtDestination;
  }

  public SearchParamsBuilder<T> relaxCostAtDestination(double relaxCostAtDestination) {
    this.relaxCostAtDestination = relaxCostAtDestination;
    return this;
  }

  public boolean timetableEnabled() {
    return timetableEnabled;
  }

  public SearchParamsBuilder<T> timetableEnabled(boolean enable) {
    this.timetableEnabled = enable;
    return this;
  }

  public boolean constrainedTransfersEnabled() {
    return constrainedTransfersEnabled;
  }

  public SearchParamsBuilder<T> constrainedTransfersEnabled(boolean enable) {
    this.constrainedTransfersEnabled = enable;
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

  public SearchParamsBuilder<T> allowEmptyEgressPaths(boolean allowEmptyEgressPaths) {
    this.allowEmptyEgressPaths = allowEmptyEgressPaths;
    return this;
  }

  public boolean allowEmptyEgressPaths() {
    return allowEmptyEgressPaths;
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
      .addServiceTime("earliestDepartureTime", earliestDepartureTime, SearchParams.TIME_NOT_SET)
      .addServiceTime("latestArrivalTime", latestArrivalTime, SearchParams.TIME_NOT_SET)
      .addDurationSec("searchWindow", searchWindowInSeconds)
      .addBoolIfTrue("departAsLateAsPossible", preferLateArrival)
      .addNum("numberOfAdditionalTransfers", numberOfAdditionalTransfers)
      .addCollection("accessPaths", accessPaths, 5)
      .addCollection("egressPaths", egressPaths, 5)
      .toString();
  }
}
