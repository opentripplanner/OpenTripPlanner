package org.opentripplanner.raptor.direct.api;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.request.SearchParams;

/**
 * Mutable version of {@link SearchParams}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
@SuppressWarnings("UnusedReturnValue")
public class RaptorDirectTransitRequestBuilder {

  private int earliestDepartureTime;
  private int searchWindowInSeconds;
  private RelaxFunction relaxC1;
  private final Collection<RaptorAccessEgress> accessPaths = new ArrayList<>();
  private final Collection<RaptorAccessEgress> egressPaths = new ArrayList<>();

  public RaptorDirectTransitRequestBuilder(RaptorDirectTransitRequest defaults) {
    this.earliestDepartureTime = defaults.earliestDepartureTime();
    this.searchWindowInSeconds = defaults.searchWindowInSeconds();
    this.relaxC1 = defaults.relaxC1();
    this.accessPaths.addAll(defaults.accessPaths());
    this.egressPaths.addAll(defaults.egressPaths());
  }

  public RaptorDirectTransitRequestBuilder earliestDepartureTime(int earliestDepartureTime) {
    this.earliestDepartureTime = earliestDepartureTime;
    return this;
  }

  public RaptorDirectTransitRequestBuilder searchWindowInSeconds(int searchWindowInSeconds) {
    this.searchWindowInSeconds = searchWindowInSeconds;
    return this;
  }

  public RaptorDirectTransitRequestBuilder searchWindow(Duration searchWindow) {
    return searchWindowInSeconds(
      searchWindow == null ? RaptorConstants.NOT_SET : (int) searchWindow.toSeconds()
    );
  }

  public RaptorDirectTransitRequestBuilder withRelaxC1(RelaxFunction relaxC1) {
    this.relaxC1 = relaxC1;
    return this;
  }

  public Collection<RaptorAccessEgress> accessPaths() {
    return accessPaths;
  }

  public RaptorDirectTransitRequestBuilder addAccessPaths(
    Collection<? extends RaptorAccessEgress> accessPaths
  ) {
    this.accessPaths.addAll(accessPaths);
    return this;
  }

  public RaptorDirectTransitRequestBuilder addAccessPaths(RaptorAccessEgress... accessPaths) {
    return addAccessPaths(Arrays.asList(accessPaths));
  }

  public Collection<RaptorAccessEgress> egressPaths() {
    return egressPaths;
  }

  public RaptorDirectTransitRequestBuilder addEgressPaths(
    Collection<? extends RaptorAccessEgress> egressPaths
  ) {
    this.egressPaths.addAll(egressPaths);
    return this;
  }

  public RaptorDirectTransitRequestBuilder addEgressPaths(RaptorAccessEgress... egressPaths) {
    return addEgressPaths(Arrays.asList(egressPaths));
  }

  public RaptorDirectTransitRequest build() {
    return new RaptorDirectTransitRequest(
      earliestDepartureTime,
      searchWindowInSeconds,
      relaxC1,
      accessPaths,
      egressPaths
    );
  }
}
